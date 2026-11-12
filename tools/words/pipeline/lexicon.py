"""A language's words: the accepted set, the written forms, the lemmas and the solutions (§6.2).

Read from the reduced sources: Hunspell's forms (Italian, Spanish) or SCOWL's (English), widened
by every single word Wiktionary has in the language; Wiktionary's senses say which written forms
are lemmas, which are forms of another, and which are kept out of the solutions by their tags.
"""

import dataclasses
import pathlib
import re

from . import config, folding, frequency, hunspell, scowl, sources, wiktionary

# Wiktionary's parts of speech a solution may have. A name, an abbreviation, an affix, a phrase,
# a symbol, an interjection, an article or a contraction never is.
SOLUTION_POS = frozenset({"noun", "verb", "adj", "adv", "pron", "prep", "conj", "num", "det", "participle"})

# Parts of speech whose words are not words of the game at all (not even accepted).
NOT_WORDS = frozenset(
    {"name", "abbrev", "symbol", "character", "prefix", "suffix", "phrase", "punct", "proverb",
     "romanization", "hard-redirect", "unknown", "infix", "interfix", "circumfix", "root"}
)

# A sense with one of these is not a lemma's own: the word is a form, a variant or an error.
_FORM_TAGS = frozenset(
    {"form-of", "alt-of", "compound-of", "contraction", "misspelling", "abbreviation", "initialism",
     "acronym", "clipping"}
)

# Tags that keep a word out of the solutions whenever any of its senses has one.
OFFENSIVE_TAGS = frozenset(
    {"offensive", "vulgar", "derogatory", "slur", "pejorative", "ethnic", "obscene", "sexual",
     "swear-word", "disparaging"}
)

# Tags that keep a word out when every one of its lemma senses has one: a solution must be a word
# of today's language.
DATED_TAGS = frozenset(
    {"obsolete", "archaic", "dated", "outdated", "rare", "poetic", "dialectal", "nonstandard",
     "proscribed", "Tuscany", "regional", "historical-spelling", "no-gloss", "uncommon"}
)

# A Roman numeral as a dictionary writes it in lower case (*xviii*), not *civil* or *vivid*.
_ROMAN = re.compile(r"^m{0,4}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})$")


@dataclasses.dataclass
class Entry:
    word: str
    lang: str
    pos: str
    senses: list
    translations: list
    forms: list
    bad: bool
    extract: str


@dataclasses.dataclass
class Solution:
    key: str
    display: str
    zipf: float
    pos: str
    level: int = 0
    name_homograph: bool = False


@dataclasses.dataclass
class LanguageWords:
    language: str
    length: int
    accepted: set
    displays: dict  # key -> sorted display forms
    lemma_of: dict  # display -> sorted lemmas it is a form of (empty for a lemma)
    lemmas: set  # displays that are lemmas
    solutions: list
    excluded: dict  # reason -> sorted displays
    accepted_by_source: dict
    flagged: dict  # solution display -> its first offensive or vulgar sense, for a person to read
    spelling_keys: set  # the accepted keys the spelling dictionary (Hunspell, SCOWL) has


class Wiktionary:
    """The reduced extracts, indexed by (extract, language, word)."""

    def __init__(self, log=print):
        self.index: dict = {}
        for extract in wiktionary.EXTRACTS:
            wiktionary.reduce(extract, log)
            count = 0
            for d in wiktionary.entries(extract):
                e = Entry(
                    d["w"], d["l"], d.get("p", ""), d.get("s", []), d.get("tr", []),
                    d.get("fm", []), bool(d.get("bad")), extract,
                )
                self.index.setdefault((extract, e.lang), {}).setdefault(e.word, []).append(e)
                count += 1
            log(f"loaded {extract}: {count} entries")

    def entries(self, extract: str, lang: str, word: str) -> list:
        return self.index.get((extract, lang), {}).get(word, [])

    def words(self, extract: str, lang: str):
        return self.index.get((extract, lang), {})

    def all_entries(self, lang: str, word: str) -> list:
        """Every entry of a word in a language, in every edition."""
        out = []
        for extract in wiktionary.EXTRACTS:
            out += self.entries(extract, lang, word)
        return out

    def native_extracts(self, lang: str) -> tuple:
        """The extracts where a language's words are headwords: the English edition's and its own."""
        return tuple(dict.fromkeys((f"en.{lang}", lang if lang != "en" else "en.en")))


def lemma_senses(entry: Entry) -> list:
    if entry.pos in NOT_WORDS:
        return []
    out = []
    for s in entry.senses:
        if s.get("f") or s.get("a"):
            continue
        if _FORM_TAGS & set(s.get("t", ())):
            continue
        out.append(s)
    return out


def form_senses(entry: Entry) -> list:
    return [s for s in entry.senses if s.get("f") or _FORM_TAGS & set(s.get("t", ()))]


def mostly_a_form(entries) -> bool:
    """Whether every entry of a word opens with an inflected form.

    Wiktionary orders an entry's senses by use, so an entry whose first sense is "plural of" is a
    form first. *cuori*, *bumps*, *years* and *teeth* have no entry that opens otherwise; *posto*,
    *nulla*, *chica*, *mayor* and *tired* have one that opens with a meaning of its own (a noun, a
    pronoun, an adjective), whatever form shares their letters.
    """
    forms = lemmas = 0
    for e in entries:
        if not e.senses or e.pos in NOT_WORDS:
            continue
        first = e.senses[0]
        if first.get("f") or first.get("a") or _FORM_TAGS & set(first.get("t", ())):
            forms += 1
        else:
            lemmas += 1
    return forms > 0 and lemmas == 0


def is_offensive(entries) -> bool:
    """Whether a word's first meaning, or half of its meanings, is offensive or vulgar.

    A sense marked so somewhere among many does not make a word offensive (*apple*, *bacon* and
    *rosso* have one each); a word whose first sense is, or most of whose senses are, is kept out.
    What this misses or keeps wrongly, the deny list settles, read by a person.
    """
    senses = [s for e in entries for s in lemma_senses(e)]
    if not senses:
        return False
    marked = [bool(OFFENSIVE_TAGS & set(s.get("t", ()))) for s in senses]
    first = [lemma_senses(e) for e in entries if lemma_senses(e)][0][0]
    return bool(OFFENSIVE_TAGS & set(first.get("t", ()))) or sum(marked) * 2 >= len(marked)


def is_dated(entries) -> bool:
    senses = [s for e in entries for s in lemma_senses(e)]
    if not senses:
        return False
    return all(DATED_TAGS & set(s.get("t", ())) for s in senses)


def headword_entries(wikt: Wiktionary, language: str, word: str) -> list:
    """A word's entries in the English edition, or in its own edition when the English has none."""
    for extract in wikt.native_extracts(language):
        entries = wikt.entries(extract, language, word)
        if entries:
            return entries
    return []


def read_list(name: str) -> list[str]:
    path = config.LISTS / name
    if not path.exists():
        return []
    out = []
    for line in path.read_text(encoding="utf-8").splitlines():
        out += line.split("#", 1)[0].split()
    return out


def _source_forms(language: str, length: int) -> tuple[dict, dict]:
    """Written forms of the right length from the spelling sources, by key, and their source."""
    by_key: dict = {}
    origin: dict = {}

    def add(form, source):
        if form[:1].isupper() or _ROMAN.match(form):
            return
        key = folding.fold(form, language)
        if key is None or len(key) != length:
            return
        by_key.setdefault(key, set()).add(form.lower())
        origin.setdefault(source, set()).add(key)

    if language == "en":
        accepted, _ = scowl.read()
        for form in accepted:
            add(form, "SCOWL")
    else:
        for name in config.HUNSPELL[language]:
            keep = lambda f: "'" not in f and len(f) <= length + 3  # noqa: E731
            for form in hunspell.forms(sources.path(f"{name}.dic"), sources.path(f"{name}.aff"), keep):
                add(form, "Hunspell")
    return by_key, origin


def build(language: str, wikt: Wiktionary, length: int = 5, log=print) -> LanguageWords:
    by_key, origin = _source_forms(language, length)
    spelling_keys = set(by_key)

    # Wiktionary's single words widen the accepted set, but never with a name or an abbreviation.
    wikt_keys = set()
    for extract in wikt.native_extracts(language):
        for word, entries in wikt.words(extract, language).items():
            if word[:1].isupper() or _ROMAN.match(word):
                continue
            if all(e.pos in NOT_WORDS for e in entries):
                continue
            key = folding.fold(word, language)
            if key is None or len(key) != length:
                continue
            by_key.setdefault(key, set()).add(word)
            wikt_keys.add(key)
    origin["Wiktionary"] = wikt_keys

    accepted = set(by_key)
    displays = {k: sorted(v) for k, v in by_key.items()}

    # Lemmas and the forms of a lemma. The English edition says it when it has the word (its tags
    # are the most regular); the language's own edition only when the English one has no entry.
    lemma_of: dict = {}
    lemmas = set()
    for key, forms in displays.items():
        for display in forms:
            entries = headword_entries(wikt, language, display)
            if any(lemma_senses(e) for e in entries):
                lemmas.add(display)
                lemma_of[display] = []
                continue
            targets = []
            for e in entries:
                for s in e.senses:
                    for t in list(s.get("f", ())) + list(s.get("a", ())):
                        if t not in targets and t != display:
                            targets.append(t)
            lemma_of[display] = targets

    zipf = frequency.zipf_table(language)
    _, one_spelling = scowl.read() if language == "en" else (None, set())
    deny = set(read_list(f"deny_{language}.txt"))

    excluded: dict = {}
    candidates: dict = {}
    for key, forms in sorted(displays.items()):
        for display in forms:
            if display not in lemmas:
                continue
            entries = headword_entries(wikt, language, display)
            pos = [e.pos for e in entries if lemma_senses(e) and e.pos in SOLUTION_POS]
            reason = None
            if not pos:
                reason = "not a common word's part of speech (a name, an interjection, an affix...)"
            elif display in deny or key in deny:
                reason = "the deny list"
            elif mostly_a_form(entries):
                reason = "an inflected form (every entry opens with one)"
            elif is_offensive(entries):
                reason = "offensive or vulgar (Wiktionary's tags)"
            elif is_dated([e for e in entries if e.pos in SOLUTION_POS]):
                reason = "archaic, obsolete, rare or regional in every sense"
            elif language == "it" and not folding.within_italian_alphabet(key):
                reason = "outside the Italian alphabet's 21 letters"
            elif zipf.get(display, 0) < config.MIN_ZIPF:
                reason = "too rare (not in wordfreq's list)"
            elif key not in spelling_keys:
                reason = "not in the spelling dictionary"
            elif language == "en" and display not in one_spelling:
                reason = "spelled two ways (British and American)"
            if reason:
                excluded.setdefault(reason, []).append(display)
                continue
            z = zipf.get(display, 0.0)
            current = candidates.get(key)
            if current is None or z > current.zipf:
                candidates[key] = Solution(key, display, z, pos[0])
    solutions = sorted(candidates.values(), key=lambda s: s.key)
    flagged = {}
    for s in solutions:
        for e in headword_entries(wikt, language, s.display):
            bad = [x for x in lemma_senses(e) if OFFENSIVE_TAGS & set(x.get("t", ()))]
            if bad and s.display not in flagged:
                flagged[s.display] = bad[0].get("g", "")
    for s in solutions:
        s.level = config.level_of(language, s.zipf)
        # wordfreq counts a word and the name it spells as one (*lewis*, *cisco*). Where a list says
        # the name is the more common (SCOWL's sizes, for English), the frequency says nothing of
        # the word: Advanced at least, never a daily.
        if language == "en" and scowl.name_outranks(s.display):
            s.level = max(s.level, 2)
            s.name_homograph = True
    for reason in excluded:
        excluded[reason] = sorted(set(excluded[reason]))
    log(f"{language}: {len(accepted)} accepted, {len(lemmas)} lemmas, {len(solutions)} solutions")
    return LanguageWords(
        language, length, accepted, displays, lemma_of, lemmas, solutions, excluded,
        {k: len(v) for k, v in origin.items()},
        flagged,
        spelling_keys,
    )


def lists_dir() -> pathlib.Path:
    return config.LISTS
