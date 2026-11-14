"""A language's words: the accepted set, the written forms, the lemmas and the solutions (§6.2).

Read from the reduced sources: Hunspell's forms (Italian, Spanish) or SCOWL's (English), widened
by every single word Wiktionary has in the language; Wiktionary's senses say which written forms
are lemmas, which are forms of another, and which are kept out of the solutions by their tags.

No word is ever named here or in a list to keep it out: the editions' own labels decide
(`register.py`), in every edition that has the word.

- **Refused** (not even a guess): a word whose first sense is coarse in most of the editions that
  have it, or most of whose first entry's senses are coarse in one, or whose first sense every
  edition labels harsh; a form whose every lemma (or Hunspell stem) is refused.
- **Never a solution**: besides the refused, a word with a coarse or derogatory sense anywhere, in
  any edition (*palla*, *colpo* and *rosso* stay guesses, never a word to find), or whose first
  sense is intimate.
"""

import dataclasses
import pathlib
import re

import functools

from . import config, folding, frequency, hunspell, register, scowl, sources, wiktionary

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
    register: str  # the entry's own labels (its categories), as `register` writes them
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
    spelling_keys: set  # the accepted keys the spelling dictionary (Hunspell, SCOWL) has
    refused: int  # keys of this length no guess may be, by the editions' labels (counted, never listed)
    unlisted: int  # lemmas kept out by the labels (counted, never listed)


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
                    d.get("fm", []), d.get("r", ""), extract,
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


def sense_labels(sense: dict) -> set:
    return set(sense.get("r", ""))


_HARSH = frozenset({register.COARSE, register.DEROGATORY})


def _first_lemma_sense(entries):
    for e in entries:
        senses = lemma_senses(e)
        if senses:
            return e, senses
    return None, []


def _first_entry(entries):
    """An edition's first entry of a word that is a word (not a name, an affix...)."""
    for e in entries:
        if e.senses and e.pos not in NOT_WORDS:
            return e
    return None


def _first_labels(entries) -> set:
    """The labels of an edition's first sense of a word, as the edition orders it: a form's
    («plural of tooth») included, so a slang sense listed after it is not taken for the first."""
    e = _first_entry(entries)
    if e is None:
        return set()
    first = sense_labels(e.senses[0])
    if len(e.senses) == 1:
        first |= set(e.register)
    return first


def mostly_coarse(entries) -> bool:
    """Whether more than half of the senses of an edition's first entry of a word are coarse (a
    word that is mostly an obscenity, whatever its first sense). The first entry only: a word's
    other entries (an interjection, a homograph) are other words, and derogatory senses do not
    count (*orso* has two, a bear stays a bear)."""
    e = _first_entry(entries)
    if e is None:
        return False
    coarse = sum(1 for s in e.senses if register.COARSE in sense_labels(s))
    return coarse * 2 > len(e.senses)


def refused_in(entries) -> bool:
    """Whether one edition's entries make a word one no guess may be: its first sense is coarse, or
    most of its first entry's senses are. `refused` weighs the editions together."""
    return register.COARSE in _first_labels(entries) or mostly_coarse(entries)


def harsh_anywhere(entries) -> bool:
    """Whether any sense, or the entry itself, is coarse or derogatory: such a word is never a
    solution (the owner's choice, §15, 14 Nov 2026: *rosso*, *palla* and *colpo* go too)."""
    for e in entries:
        if set(e.register) & _HARSH:
            return True
        if any(sense_labels(s) & _HARSH for s in e.senses):
            return True
    return False


def intimate_first(entries) -> bool:
    _, senses = _first_lemma_sense(entries)
    return bool(senses) and register.INTIMATE in sense_labels(senses[0])


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


def editions(wikt, language: str, word: str) -> list:
    """A word's entries, edition by edition: every extract that has it as a word of its language
    (the English edition, its own, and the third, which glosses it in its own language)."""
    out = []
    for extract in wiktionary.EXTRACTS:
        entries = wikt.entries(extract, language, word)
        if entries:
            out.append(entries)
    return out


def _verdicts(wikt) -> dict:
    cache = getattr(wikt, "_register_verdicts", None)
    if cache is None:
        cache = {}
        wikt._register_verdicts = cache
    return cache


def refused(wikt, language: str, word: str) -> bool:
    """Whether a lemma is no word of the game, by any edition's labels (and the same for the word
    written in lower case: a sentence's first word is no different)."""
    key = ("refused", language, word)
    cache = _verdicts(wikt)
    if key not in cache:
        groups = [g for g in editions(wikt, language, word) if any(e.senses for e in g)]
        first_coarse = sum(1 for entries in groups if register.COARSE in _first_labels(entries))
        cache[key] = (
            # Most editions put a coarse sense first (one edition's order is not a verdict: the
            # Spanish edition opens *bombear*, to pump, with its slang);
            first_coarse * 2 > len(groups)
            # or one edition's first entry is mostly coarse;
            or any(mostly_coarse(entries) for entries in groups)
            # or every edition puts a derogatory sense first.
            or (bool(groups) and all(_first_labels(entries) & _HARSH for entries in groups)
                and any(register.DEROGATORY in _first_labels(entries) for entries in groups))
        )
    return cache[key]


def labelled(wikt, language: str, word: str) -> bool:
    """Whether a lemma may not be a solution by the labels: refused, coarse or derogatory in any
    sense of any edition, or intimate in its first sense in one."""
    key = ("labelled", language, word)
    cache = _verdicts(wikt)
    if key not in cache:
        groups = editions(wikt, language, word)
        cache[key] = refused(wikt, language, word) or any(
            harsh_anywhere(entries) or intimate_first(entries) for entries in groups
        )
    return cache[key]


def common_lists() -> list[str]:
    return sorted(p.name for p in config.LISTS.glob("common_*.txt"))


def read_list(name: str) -> list[str]:
    path = config.LISTS / name
    if not path.exists():
        return []
    out = []
    for line in path.read_text(encoding="utf-8").splitlines():
        out += line.split("#", 1)[0].split()
    return out


@functools.cache
def spelling_forms(language: str) -> dict:
    """Every written form the spelling dictionaries give, of any length the game may use, with the
    stems it comes from (none for English: SCOWL lists forms, not stems)."""
    longest = max(config.LENGTHS) + 3
    if language == "en":
        accepted, _ = scowl.read()
        return {form: set() for form in accepted}
    out: dict = {}
    for name in config.HUNSPELL[language]:
        keep = lambda f: "'" not in f and len(f) <= longest  # noqa: E731
        found = hunspell.forms_by_stem(sources.path(f"{name}.dic"), sources.path(f"{name}.aff"), keep)
        for form, stems in found.items():
            out.setdefault(form, set()).update(stems)
    return out


def _source_forms(language: str, length: int) -> tuple[dict, dict, dict]:
    """Written forms of the right length from the spelling sources, by key, their source, and the
    stems each comes from."""
    by_key: dict = {}
    origin: dict = {}
    stems: dict = {}
    source = "SCOWL" if language == "en" else "Hunspell"
    for form, from_stems in spelling_forms(language).items():
        if form[:1].isupper() or _ROMAN.match(form):
            continue
        key = folding.fold(form, language)
        if key is None or len(key) != length:
            continue
        low = form.lower()
        by_key.setdefault(key, set()).add(low)
        origin.setdefault(source, set()).add(key)
        stems.setdefault(low, set()).update(from_stems)
    return by_key, origin, stems


LABELLED = "a coarse, derogatory or intimate sense (the editions' labels; counted, never listed)"


def build(language: str, wikt: Wiktionary, length: int = 5, log=print) -> LanguageWords:
    by_key, origin, stems = _source_forms(language, length)
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

    # Lemmas and the forms of a lemma. The English edition says it when it has the word (its tags
    # are the most regular); the language's own edition only when the English one has no entry.
    lemma_of: dict = {}
    lemmas = set()
    for key, forms in by_key.items():
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

    # What no guess may be: a refused lemma; a form whose every lemma is refused, or whose every
    # Hunspell stem is (an inflection the editions do not list is judged with its stem).
    def refused_form(display: str) -> bool:
        if refused(wikt, language, display):
            return True
        targets = lemma_of.get(display, [])
        if targets and all(refused(wikt, language, t) for t in targets):
            return True
        from_stems = stems.get(display, ())
        return bool(from_stems) and all(
            s != display and refused(wikt, language, s) for s in from_stems
        )

    refused_keys = 0
    displays = {}
    for key, forms in by_key.items():
        kept = sorted(d for d in forms if not refused_form(d))
        if kept:
            displays[key] = kept
        else:
            refused_keys += 1
    accepted = set(displays)
    spelling_keys &= accepted
    kept = {d for forms in displays.values() for d in forms}
    lemmas &= kept
    lemma_of = {d: [t for t in ts if not refused(wikt, language, t)] for d, ts in lemma_of.items() if d in kept}

    zipf = frequency.zipf_table(language)
    _, one_spelling = scowl.read() if language == "en" else (None, set())

    excluded: dict = {}
    unlisted = 0
    candidates: dict = {}
    for key, forms in sorted(displays.items()):
        for display in forms:
            if display not in lemmas:
                continue
            entries = headword_entries(wikt, language, display)
            pos = [e.pos for e in entries if lemma_senses(e) and e.pos in SOLUTION_POS]
            reason = None
            if labelled(wikt, language, display):
                unlisted += 1
                continue
            if not pos:
                reason = "not a common word's part of speech (a name, an interjection, an affix...)"
            elif mostly_a_form(entries):
                reason = "an inflected form (every entry opens with one)"
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
    for s in solutions:
        s.level = config.level_of(language, s.zipf)
        # wordfreq counts a word and the name it spells as one (*cisco*). Where a list says the
        # name is the more common (SCOWL's sizes, for English), the frequency says nothing of the
        # word: Advanced at least, never a daily.
        if language == "en" and scowl.name_outranks(s.display):
            s.level = max(s.level, 2)
            s.name_homograph = True
    for reason in excluded:
        excluded[reason] = sorted(set(excluded[reason]))
    log(f"{language}-{length}: {len(accepted)} accepted ({refused_keys} refused), {len(lemmas)} lemmas, "
        f"{len(solutions)} candidate solutions")
    return LanguageWords(
        language, length, accepted, displays, lemma_of, lemmas, solutions, excluded,
        {k: len(v & accepted) for k, v in origin.items()},
        spelling_keys,
        refused_keys,
        unlisted,
    )


def without(lw: LanguageWords, keys: set, reason: str) -> None:
    """Moves solutions out, for a reason found after the lexicon (a meaning missing)."""
    out = [s for s in lw.solutions if s.key in keys]
    lw.solutions = [s for s in lw.solutions if s.key not in keys]
    if out:
        lw.excluded[reason] = sorted(set(lw.excluded.get(reason, [])) | {s.display for s in out})
