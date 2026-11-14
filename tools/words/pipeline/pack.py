"""The files the app ships, and how they are written (PLANNING.md §6.6, docs/adr/0003-words.md).

Every file starts with the same sixteen-byte header, then its payload; every number is big-endian:

    0  magic     "TTW" and the file's kind: A accepted, S solutions, D daily, F forms, G glossary
    4  version   1
    5  language  0 Italian, 1 English, 2 Spanish (the game language)
    6  length    the words' length in letters (0 where words of every length are keys)
    7  extra     the interface language, for a glossary; 0 otherwise
    8  count     the number of entries
    12 reserved  0

A word is packed five bits a letter, the first letter highest, in a slot whose width follows from
the file's length alone (`word_width`): **four bytes up to six letters, eight bytes from seven to
twelve**. Six letters are 30 bits, plus a solution's two level bits, 32; twelve are 60, plus two, 62.
The width is chosen per length, not once for all, so the five-letter files stay half the size and a
longer length never changes them; a reader takes it from the header's length.

- **A, accepted**: `count` words, sorted, one slot each.
- **S, solutions**: `count` words, sorted, one slot each: the packed word shifted left by two, its
  level (0 Base to 3 Complete) in the two low bits.
- **D, daily**: `count` words in the schedule's order, one slot each: the packed word XORed with
  the first bytes (as many as the slot) of SHA-256("ttw-daily/<language>/<number>"), so a glance
  spoils nothing and any day is read alone.
- **F, forms** and **G, glossary**: a table of text records sorted by their key's UTF-8 bytes:
  `count + 1` offsets (four bytes each, from the start of the text block), then the text block.
  A record is UTF-8 fields separated by U+001F; its first field is its key; a field holding a
  list separates its items by U+001E.
  A forms record holds a field a written form (the form, then its lemmas), a solution's own
  written form first; a glossary record holds the translations, the meaning, the word the
  meaning was read through and the part of speech in the interface language (empty when the
  meaning was not read in the word's own part of speech; a reader of the first three fields
  reads the files unchanged).
"""

import hashlib
import struct

from . import folding

MAX_LENGTH = 12


def word_width(length: int) -> int:
    """The bytes a packed word takes in a file of words of this length."""
    if not 1 <= length <= MAX_LENGTH:
        raise ValueError(f"no word file for {length} letters")
    return 4 if length * folding.BITS_PER_LETTER + 2 <= 32 else 8


def _slot(value: int, width: int) -> bytes:
    return value.to_bytes(width, "big")


KINDS = {"accepted": b"A", "solutions": b"S", "daily": b"D", "forms": b"F", "glossary": b"G"}
VERSION = 1
FIELD = "\u001f"
ITEM = "\u001e"


def header(kind: str, language: str, length: int, count: int, extra: int = 0) -> bytes:
    return b"TTW" + KINDS[kind] + struct.pack(
        ">BBBBII", VERSION, folding.LANGUAGE_CODE[language], length, extra, count, 0
    )


def read_header(data: bytes) -> dict:
    if data[:3] != b"TTW":
        raise ValueError("not a words file")
    version, language, length, extra, count, _ = struct.unpack(">BBBBII", data[4:16])
    kind = {v: k for k, v in KINDS.items()}[data[3:4]]
    return {"kind": kind, "version": version, "language": language, "length": length,
            "extra": extra, "count": count}


def words(kind: str, language: str, length: int, values: list[int]) -> bytes:
    width = word_width(length)
    return header(kind, language, length, len(values)) + b"".join(_slot(v, width) for v in values)


def accepted(language: str, length: int, keys) -> bytes:
    return words("accepted", language, length, sorted(folding.pack(k, language) for k in keys))


def solutions(language: str, length: int, solutions) -> bytes:
    values = sorted((folding.pack(s.key, language) << 2) | s.level for s in solutions)
    return words("solutions", language, length, values)


def daily_mask(language: str, number: int, width: int = 4) -> int:
    digest = hashlib.sha256(f"ttw-daily/{language}/{number}".encode()).digest()
    return int.from_bytes(digest[:width], "big")


def daily(language: str, length: int, keys: list[str]) -> bytes:
    width = word_width(length)
    values = [folding.pack(k, language) ^ daily_mask(language, i + 1, width) for i, k in enumerate(keys)]
    return words("daily", language, length, values)


def read_daily(data: bytes, language: str) -> list[str]:
    h = read_header(data)
    width = word_width(h["length"])
    return [
        folding.unpack(value ^ daily_mask(language, i + 1, width), h["length"], language)
        for i, value in enumerate(read_words(data))
    ]


def read_words(data: bytes) -> list[int]:
    h = read_header(data)
    width = word_width(h["length"])
    return [int.from_bytes(data[16 + width * i : 16 + width * (i + 1)], "big") for i in range(h["count"])]


def table(kind: str, language: str, length: int, records: dict, extra: int = 0) -> bytes:
    """Text records, sorted by their key's bytes, behind their offsets."""
    encoded = []
    for key, fields in records.items():
        parts = [key] + [ITEM.join(f) if isinstance(f, (list, tuple)) else (f or "") for f in fields]
        for part in parts:
            if FIELD in part or (ITEM in part and part is key):
                raise ValueError(f"a separator inside {key!r}")
        encoded.append((key.encode("utf-8"), FIELD.join(parts).encode("utf-8")))
    encoded.sort()
    offsets, blob, position = [], [], 0
    for _, record in encoded:
        offsets.append(position)
        blob.append(record)
        position += len(record)
    offsets.append(position)
    return (
        header(kind, language, length, len(encoded), extra)
        + b"".join(struct.pack(">I", o) for o in offsets)
        + b"".join(blob)
    )


def read_table(data: bytes) -> dict:
    h = read_header(data)
    n = h["count"]
    offsets = [struct.unpack(">I", data[16 + 4 * i : 20 + 4 * i])[0] for i in range(n + 1)]
    base = 16 + 4 * (n + 1)
    out = {}
    for i in range(n):
        record = data[base + offsets[i] : base + offsets[i + 1]].decode("utf-8").split(FIELD)
        out[record[0]] = [f.split(ITEM) if ITEM in f else f for f in record[1:]]
    return out
