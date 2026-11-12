"""Languages, their keys and folding (PLANNING.md §4.1).

A word as written (its display form, *città*, *niño*) is folded to its key form, the letters as
typed: lower case, NFD, the combining marks dropped, then mapped to the language's keys. In
Spanish the ñ is kept before the marks go. A word with anything left outside the keys (an
apostrophe, a hyphen, a space, a digit) is not a word of the game, and folds to None.

The letter indices are the app's (`Letter` in `:core:model`): A to Z are 0 to 25 in every
language, Ñ is 26 in Spanish. The packed form puts five bits a letter, the first letter highest,
so that sorting the packed integers sorts the words by their indices.
"""

import unicodedata

LANGUAGES = ("it", "en", "es")

# The order of the languages in a file's header byte (PLANNING.md §6.6, ADR 0003).
LANGUAGE_CODE = {"it": 0, "en": 1, "es": 2}

_LATIN = "abcdefghijklmnopqrstuvwxyz"
KEYS = {
    "it": _LATIN,
    "en": _LATIN,
    "es": _LATIN + "ñ",
}

# The Italian alphabet's 21 letters: a solution keeps to them (PLANNING §15 Open 6, the owner's
# answer), while the keyboard has all 26, for the loanwords a player types.
ITALIAN_ALPHABET = frozenset("abcdefghilmnopqrstuvz")

_TILDE = "̃"
BITS_PER_LETTER = 5


def fold(word: str, language: str) -> str | None:
    """The key form of a written word, in lower case, or None when it is not a word of the game."""
    keys = KEYS[language]
    decomposed = unicodedata.normalize("NFD", word.lower())
    out = []
    for i, ch in enumerate(decomposed):
        if unicodedata.combining(ch):
            if language == "es" and ch == _TILDE and out and out[-1] == "n":
                out[-1] = "ñ"
            continue
        out.append(ch)
    folded = "".join(out)
    if not folded or any(ch not in keys for ch in folded):
        return None
    return folded


def indices(key: str, language: str) -> list[int]:
    keys = KEYS[language]
    return [keys.index(ch) for ch in key]


def pack(key: str, language: str) -> int:
    """Five bits a letter, the first letter highest (a five-letter word in 25 bits)."""
    value = 0
    for index in indices(key, language):
        value = (value << BITS_PER_LETTER) | index
    return value


def unpack(value: int, length: int, language: str) -> str:
    keys = KEYS[language]
    letters = []
    for _ in range(length):
        letters.append(keys[value & 0x1F])
        value >>= BITS_PER_LETTER
    return "".join(reversed(letters))


def within_italian_alphabet(key: str) -> bool:
    return all(ch in ITALIAN_ALPHABET for ch in key)
