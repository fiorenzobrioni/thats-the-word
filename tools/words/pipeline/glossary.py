"""Meanings and translations, for each of the nine pairs (PLANNING.md §6.4).

What the end card, the hints and the notebook show is in the interface language U, for a word of
the game language G:

- **The same language** (U = G): the word's own definition, from its language's edition (the
  English edition for English).
- **A foreign word** (U ≠ G): its translations into U, and a short meaning in U.

A translation is offered only when it is **reliable**: proposed by at least two of four
independent sources, so that one edition's slip is never shown alone:

1. the U edition's own entry for the word, whose glosses are written in U (*perro*, in the Italian
   edition: «cane»);
2. the G edition's translation table for the word (*gatto* → es *gato*, in the Italian edition);
3. the U edition's translation tables that list the word (*gato*'s table, in the Spanish edition,
   has it *gatto*);
4. through English, for Italian and Spanish: the English words the English edition glosses the word
   with, and their translation tables, in the sense whose row holds the word.

The meaning in U is the U edition's gloss of the word when it is a definition rather than a bare
word; otherwise the meaning comes **through the translation**: the first translation's own
definition, in its own edition. A word with no reliable translation shows its meaning alone, when
it has one (VISION §9).
"""

import dataclasses
import re

from . import config, lexicon

_PARENS = re.compile(r"\s*\([^()]*\)\s*")
_NOTE = re.compile(
    r"\(\s*(approfondimento|tassonomia|citazioni|etimologia|link|vedi|definizione)[^)]*\)", re.I
)
_JUNK = re.compile(r"definizione mancante|se vuoi, aggiungila|definición faltante|^\W*$", re.I)
_GRAMMAR = frozenset(
    {"f", "m", "sing", "pl", "inv", "s", "agg", "n", "v", "tr", "intr", "pron", "avv", "loc", "sm", "sf"}
)
_ARTICLES = {
    "en": ("to ", "a ", "an ", "the "),
    "it": ("il ", "lo ", "la ", "i ", "gli ", "le ", "un ", "uno ", "una ", "l'"),
    "es": ("el ", "la ", "los ", "las ", "un ", "una ", "unos ", "unas "),
}


@dataclasses.dataclass
class Gloss:
    translations: list  # reliable ones, best first
    meaning: str | None
    meaning_via: str | None  # the translation the meaning was read through, if any
    candidates: list  # every candidate with its sources, for the report

    @property
    def agreed(self) -> bool:
        """Whether the translations shown are agreed by two sources (else one table gave one)."""
        return bool(self.translations) and any(
            len(c["sources"]) >= 2 for c in self.candidates if c["word"] == self.translations[0]
        )


def edition_extract(edition: str, lang: str) -> str:
    """The extract of the edition written in `edition` holding words of `lang`."""
    if edition == "en":
        return f"en.{lang}"
    return edition


def clean(text: str, headword: str = "") -> str | None:
    if not text:
        return None
    text = _NOTE.sub("", text)
    text = re.sub(r"\s+", " ", text).strip(" ;:,")
    if _JUNK.search(text):
        return None
    words = [w for w in re.split(r"[\s.,;:]+", text.lower()) if w]
    if not [w for w in words if w not in _GRAMMAR and w != headword.lower()]:
        return None
    if len(text) > config.MEANING_MAX:
        cut = text[: config.MEANING_MAX].rsplit(" ", 1)[0].rstrip(" ,;:")
        text = cut + "…"
    return text


def _candidate(text: str, lang: str) -> str | None:
    text = _PARENS.sub(" ", text).strip(" .;:!?\"'«»“”")
    low = text.lower()
    for article in _ARTICLES.get(lang, ()):
        if low.startswith(article) and len(low) > len(article):
            text = text[len(article) :]
            low = text.lower()
            break
    text = text.strip()
    if not text or len(text.split()) > 3 or any(c.isdigit() for c in text):
        return None
    return text


def _split_gloss(gloss: str, lang: str) -> list[str]:
    out = []
    for part in re.split(r"[;,]", _PARENS.sub(" ", gloss)):
        c = _candidate(part, lang)
        if c:
            out.append(c)
    return out


def _first_definition(wikt, lang: str, word: str, pos: tuple = ()) -> str | None:
    """A word's own definition, in its own edition (the English edition for English), from an
    entry of the same part of speech first (*bowel*, a noun, reads *intestino* the noun)."""
    extract = "en.en" if lang == "en" else lang
    entries = wikt.entries(extract, lang, word)
    ordered = (
        [e for e in entries if e.pos in pos]
        + [e for e in entries if e.pos in lexicon.SOLUTION_POS and e.pos not in pos]
        + [e for e in entries if e.pos not in lexicon.SOLUTION_POS]
    )
    for e in ordered:
        for s in lexicon.lemma_senses(e):
            text = clean(s.get("g", ""), word)
            if text:
                return text
    # The Italian edition sometimes writes a headword's sense only as its translation table's label.
    for e in ordered:
        for row in e.translations:
            label = clean(_PARENS.sub(" ", row[2]) if len(row) > 2 else "", word)
            if label and not label.startswith("#") and len(label.split()) >= 2:
                return label
    return None


TABLE_SOURCES = frozenset({"G-edition table", "U-edition tables", "through English"})


def reliable_candidate(c: dict) -> bool:
    """Two sources agree; or one translation table gives it for the word's first sense.

    The second is weaker (a table can put a rarer sense first: *guado*, the ford, has *glasto*,
    the woad, in one), so it gives one translation at most, and the samples mark it.
    """
    if len(c["sources"]) >= 2:
        return True
    return bool(c["first"] & TABLE_SOURCES) and config.SINGLE_TABLE_TRANSLATIONS


class Glossary:
    def __init__(self, wikt):
        self.wikt = wikt
        self._reverse: dict = {}

    def _reverse_index(self, extract: str, u: str, g: str) -> dict:
        """For the U-language headwords of an extract: the G words their tables list."""
        key = (extract, u, g)
        if key not in self._reverse:
            index: dict = {}
            for word, entries in self.wikt.words(extract, u).items():
                for e in entries:
                    first_label = e.translations[0][2] if e.translations else None
                    for row in e.translations:
                        if row[0] == g:
                            index.setdefault(row[1], []).append((word, row[2] == first_label))
            self._reverse[key] = index
        return self._reverse[key]

    def candidates(self, g: str, u: str, word: str) -> list:
        """Every translation proposed for the word, with the sources that propose it.

        A vote from a translation table also says whether it came from the table's first sense
        (`first`): a word's first sense is the one a player most likely met.
        """
        found: dict = {}

        def vote(candidate, source, rank, first=False):
            if not candidate:
                return
            if u != "en" and not candidate.isupper():
                candidate = candidate[0].lower() + candidate[1:]
            low = candidate.lower()
            slot = found.setdefault(low, {"word": candidate, "sources": set(), "rank": rank, "first": set()})
            slot["sources"].add(source)
            slot["rank"] = min(slot["rank"], rank)
            if first:
                slot["first"].add(source)

        # 1. The U edition's own entry for the word.
        for e in self.wikt.entries(edition_extract(u, g), g, word):
            for i, s in enumerate(lexicon.lemma_senses(e)):
                parts = list(s.get("k", ())) if u == "en" else []
                parts += _split_gloss(s.get("g", ""), u)
                for p in parts:
                    vote(_candidate(p, u), "U-edition gloss", i, i == 0)
        # 2. The G edition's table for the word.
        g_extract = "en.en" if g == "en" else g
        for e in self.wikt.entries(g_extract, g, word):
            first_label = e.translations[0][2] if e.translations else None
            for i, row in enumerate(e.translations):
                if row[0] == u:
                    vote(_candidate(row[1], u), "G-edition table", 10 + i, row[2] == first_label)
        # 3. The U edition's tables that list the word.
        u_extract = "en.en" if u == "en" else u
        for i, (target, first) in enumerate(self._reverse_index(u_extract, u, g).get(word, ())):
            vote(_candidate(target, u), "U-edition tables", 20 + i, first)
        # 4. Through English.
        if "en" not in (g, u):
            english = []
            for e in self.wikt.entries(f"en.{g}", g, word):
                for s in lexicon.lemma_senses(e):
                    english += list(s.get("k", ())) + _split_gloss(s.get("g", ""), "en")
            for en_word in dict.fromkeys(english):
                for e in self.wikt.entries("en.en", "en", en_word):
                    groups: dict = {}
                    for row in e.translations:
                        groups.setdefault(row[2], []).append(row)
                    for n, (label, rows) in enumerate(groups.items()):
                        if any(r[0] == g and r[1] == word for r in rows):
                            for r in rows:
                                if r[0] == u:
                                    vote(_candidate(r[1], u), "through English", 30 + n, n == 0)
        ranked = sorted(found.values(), key=lambda c: (-len(c["sources"]), c["rank"], c["word"]))
        return ranked

    def gloss(self, g: str, u: str, word: str) -> Gloss:
        if g == u:
            return Gloss([], _first_definition(self.wikt, g, word), None, [])
        ranked = self.candidates(g, u, word)
        agreed = [c["word"] for c in ranked if len(c["sources"]) >= 2][:3]
        single = [c["word"] for c in ranked if reliable_candidate(c)][:1]
        reliable = agreed or single
        meaning = None
        via = None
        # The U edition's gloss, when it defines rather than names.
        for e in self.wikt.entries(edition_extract(u, g), g, word):
            for s in lexicon.lemma_senses(e):
                text = clean(s.get("g", ""), word)
                if text and (u == "en" or len(text.split()) >= 3):
                    meaning = text
                    break
            if meaning:
                break
        if meaning is None and reliable:
            pos = tuple(
                dict.fromkeys(
                    e.pos for e in lexicon.headword_entries(self.wikt, g, word) if lexicon.lemma_senses(e)
                )
            )
            for t in reliable:
                meaning = _first_definition(self.wikt, u, t, pos)
                if meaning:
                    via = t
                    break
        return Gloss(reliable, meaning, via, ranked)
