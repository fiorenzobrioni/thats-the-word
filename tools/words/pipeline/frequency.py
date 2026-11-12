"""How common a word is, from wordfreq's data (PLANNING.md §6.3).

wordfreq ships its frequencies as "cBpack" files inside its wheel: a gzipped MessagePack list
whose first item is a header and whose item i+1 lists the words at -i centibels (a frequency of
10^(-i/100)). Its Zipf value, the one the levels are cut on, is 9 - i/100. The wheel is read as a
zip and the MessagePack decoded here (the few types these files use), so nothing of wordfreq is
installed or run: only its data is read, under its data licence (CC BY-SA 4.0).
"""

import gzip
import struct
import zipfile

from . import sources


def _decode(data: bytes, i: int = 0):
    b = data[i]
    if b <= 0x7F:
        return b, i + 1
    if 0x80 <= b <= 0x8F:
        return _map(data, i + 1, b & 0x0F)
    if 0x90 <= b <= 0x9F:
        return _array(data, i + 1, b & 0x0F)
    if 0xA0 <= b <= 0xBF:
        n = b & 0x1F
        return data[i + 1 : i + 1 + n].decode("utf-8"), i + 1 + n
    if b == 0xC0:
        return None, i + 1
    if b in (0xC2, 0xC3):
        return b == 0xC3, i + 1
    if b in (0xD9, 0xDA, 0xDB):
        size = {0xD9: 1, 0xDA: 2, 0xDB: 4}[b]
        n = int.from_bytes(data[i + 1 : i + 1 + size], "big")
        start = i + 1 + size
        return data[start : start + n].decode("utf-8"), start + n
    if b in (0xCC, 0xCD, 0xCE, 0xCF):
        size = {0xCC: 1, 0xCD: 2, 0xCE: 4, 0xCF: 8}[b]
        return int.from_bytes(data[i + 1 : i + 1 + size], "big"), i + 1 + size
    if b in (0xDC, 0xDD):
        size = 2 if b == 0xDC else 4
        n = int.from_bytes(data[i + 1 : i + 1 + size], "big")
        return _array(data, i + 1 + size, n)
    if b in (0xDE, 0xDF):
        size = 2 if b == 0xDE else 4
        n = int.from_bytes(data[i + 1 : i + 1 + size], "big")
        return _map(data, i + 1 + size, n)
    if b == 0xCB:
        return struct.unpack(">d", data[i + 1 : i + 9])[0], i + 9
    if b >= 0xE0:
        return b - 0x100, i + 1
    raise ValueError(f"MessagePack type 0x{b:02x} not read here")


def _array(data, i, n):
    out = []
    for _ in range(n):
        value, i = _decode(data, i)
        out.append(value)
    return out, i


def _map(data, i, n):
    out = {}
    for _ in range(n):
        key, i = _decode(data, i)
        value, i = _decode(data, i)
        out[key] = value
    return out, i


def zipf_table(language: str) -> dict[str, float]:
    """Every word of the language's large list with its Zipf frequency, two decimals."""
    with zipfile.ZipFile(sources.path("wordfreq")) as wheel:
        packed = wheel.read(f"wordfreq/data/large_{language}.msgpack.gz")
    value, _ = _decode(gzip.decompress(packed))
    header, buckets = value[0], value[1:]
    if not isinstance(header, dict) or header.get("format") != "cB":
        raise ValueError("not a cBpack file")
    table = {}
    for i, words in enumerate(buckets):
        zipf = round(9 - i / 100, 2)
        for word in words:
            table.setdefault(word, zipf)
    return table
