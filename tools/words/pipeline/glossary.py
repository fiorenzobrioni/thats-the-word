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

The meaning in U comes **through the translation**: the translation's own definition, in its own
edition and of the word's part of speech, read first in the sense whose translation table holds
the word (*heart*'s organ, whose table has *cuore*, before its courage), so the three languages
describe one sense; else it is the U edition's gloss of the word, when that defines rather than
names (the English edition glosses most Italian words with their translations).
A word with no reliable translation shows its meaning alone, when it has one (VISION §9).

A meaning (§15, 14 Nov 2026) is a whole definition of at most `config.MEANING_MAX` characters and
`config.MEANING_MIN_WORDS` words of meaning or more, never cut with an ellipsis: a gloss too long is
shortened where a clause (`;`) or a sentence ends, its asides in brackets dropped, or, last, at its
first comma when what comes before is a phrase of its own (three words of meaning at least, not a
qualifier opening with a preposition); else the next sense
is read among the finer senses of the first one (not a figurative one), in the first of a word's
homographs, or the sense a translation table names: never a later, rarer sense in the first one's
place, and never a gloss that leans on the sense before it («Bulbo de esta planta»). It never comes from a sense the editions label coarse,
derogatory or intimate, never holds a word no guess may be, never names a foreign word itself (or its stem: the definition is also a
hint), and never points elsewhere instead of defining («synonym of»).
"""

import dataclasses
import re
import unicodedata

from . import config, lexicon

_PARENS = re.compile(r"\s*\([^()]*\)\s*")
_BRACKETS = re.compile(r"\s*[\(\[][^()\[\]]*[\)\]]\s*")
# A gloss that points to another word instead of defining this one.
_POINTER = re.compile(
    r"^(?:synonym of|alternative (?:form|spelling) of|see\b|used other than|sinonimo di|"
    r"sinónimo de|variante\b|variant of|forma alternativa|vedi\b|vedasi\b|véase\b|v\.\s|"
    # A gloss that describes a form, not a meaning («Participio pasado del verbo…»).
    r"(?:past |present )?participle of|plural of|feminine of|participio|plurale di|femminile di|"
    r"maschile di|plural de|femenino de|masculino de|grafía|grafia)",
    re.I,
)
# A gloss that leans on the sense before it («Bulbo de esta planta», «A serving of this beverage»):
# alone, it says nothing.
_ANAPHORA = re.compile(
    r"\b(?:of|from|to) (?:this|these|such)\b|^(?:this|these|such)\b(?!\s+as\b)|"
    r"\b(?:de|del|a) (?:esta|este|estas|estos|dicha|dicho|dichas|dichos)\b|"
    r"\b(?:di|da|a) (?:questa|questo|queste|questi)\b|\bsuddett\w*",
    re.I,
)
# A clause a long gloss may be shortened to at a comma must stand alone: not a qualifier opening
# with a preposition («di cavalli o altri quadrupedi, procedere…»).
_QUALIFIER = re.compile(
    r"^(?:of|for|in|on|with|by|among|within|di|del|dello|della|dei|degli|delle|nel|nello|nella|"
    r"nei|negli|nelle|nell'|sul|sullo|sulla|sui|sugli|sulle|per|con|tra|fra|de|para|por|en|entre)\b",
    re.I,
)
# A word that makes a qualifier a definition after all («In grado di volare», «En que se…»).
_VERBISH = re.compile(r"\b(?:che|chi|cui|que|quien|which|who|that|grado|able|capable|is|are|es|son|è|sono)\b", re.I)
_RELATIVE = re.compile(
    r"\b(?:che|cui|quale|quali|dove|which|who|whom|whose|where|que|cual|cuales|donde|cuyo|cuya)\b", re.I
)
_DETERMINERS = frozenset("a an the il lo la i gli le un uno una el los las unos unas".split())
_SUBSCRIPTS = re.compile(r"[₀-₉⁰-⁹]")
# A gloss that quotes another dictionary or an etymology («'Cuchilla…', princ. S. XV»).
_QUOTED = re.compile(r"^['‘’\"“”«]")
# A finer sense that moves away from the first one rather than refining it.
_DRIFT_TAGS = frozenset({"figuratively", "metaphoric", "broadly", "by-extension", "humorous", "slang", "rare"})
# A label in brackets before the definition (*(zoology)*): the definition is what follows.
_LEADING_LABEL = re.compile(r"^\s*[\(\[][^()\[\]]*[\)\]]\s*")
# Where a sentence ends inside a gloss: a full stop after a word, before a capital.
_SENTENCE_END = re.compile(r"(?<=\w\w)[.!?]\s+(?=[A-ZÁÉÍÓÚÑÀÈÌÒÙ¿¡«\"])")
_NOTE = re.compile(
    r"\(\s*(approfondimento|tassonomia|citazioni|etimologia|link|vedi|definizione)[^)]*\)", re.I
)
_JUNK = re.compile(r"definizione mancante|se vuoi, aggiungila|definición faltante|^\W*$", re.I)
# The editions' grammar marks, left at a gloss's end («casa f sing»).
_GRAMMAR = frozenset(
    {"f", "m", "sing", "pl", "inv", "s", "agg", "n", "v", "tr", "intr", "pron", "avv", "loc", "sm", "sf"}
)
# Words that carry no meaning of their own: a definition needs others («A dog» names, «Large shield»
# and «Che è pieno» define).
_FUNCTION_WORDS = frozenset(
    "a an the to of in on at by for or and with from as "
    "il lo la i gli le un una di da del dello della dei degli delle in su per con tra fra e o "
    "el los las unos unas de del al en con por para y o".split()
    # and the prepositions and conjunctions a phrase cannot end on
    + "into than between about over under within whose which that "
    "verso senza sopra sotto dopo prima che cui "
    "entre sobre hacia hasta desde sin contra tras que cuyo".split()
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
    pos: str | None = None  # the part of speech of the entry the meaning was read in

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


def _fold(text: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFD", text.lower()) if not unicodedata.combining(c))


def leaks(text: str, words, stems: bool = True) -> bool:
    """Whether a text names one of the words, or (with `stems`) the stem of a word of five letters
    or more (all but its last letter: *gatto* in *gattino*), so a definition shown as a hint gives
    nothing away."""
    tokens = re.findall(r"[^\W\d_]+", _fold(text))
    for word in words:
        folded = _fold(word)
        if not folded:
            continue
        stem = folded[:-1] if stems and len(folded) >= 5 else None
        for t in tokens:
            if t == folded or (stem and t.startswith(stem)):
                return True
    return False


def _tidy(text: str) -> str:
    # A space left before a stop where a bracket was taken away («alphabet (Γγ), preceded»).
    return re.sub(r"\s+([;:,.])", r"\1", re.sub(r"\s+", " ", text)).strip(" ;:,")


def fit(text: str, avoid=(), stems: bool = True) -> str | None:
    """A gloss as a meaning the app may show, or None (the rules are the module's)."""
    if not text:
        return None
    # An edition's cross-reference numbers («lanza₁») and loose spaces before a stop say nothing.
    text = _SUBSCRIPTS.sub("", text)
    text = re.sub(r"\s+([;:,.])", r"\1", _tidy(_LEADING_LABEL.sub("", _NOTE.sub("", text))))
    if not text or _JUNK.search(text) or _POINTER.search(text) or text.endswith(":"):
        return None
    if "…" in text or "..." in text or _ANAPHORA.search(text) or _QUOTED.match(text):
        return None
    bare = _tidy(_BRACKETS.sub(" ", text))
    clause = _tidy(bare.split(";")[0])
    sentence = _tidy(_SENTENCE_END.split(bare)[0])
    first = _tidy(_SENTENCE_END.split(clause)[0])
    # The first clause or sentence first, when it defines on its own: «Firm in standing or
    # position», not the four that follow it; then the whole gloss.
    variants = [v for v in (first, clause, sentence) if _content_words(v) >= config.MEANING_MIN_WORDS + 1]
    variants += [text, bare, clause, sentence]
    # At its first comma, last: what comes before it, when it is a phrase of its own (*città*,
    # «centro abitato di grandi dimensioni»); a later comma would split a list. Never an ellipsis.
    if len(first) > config.MEANING_MAX and not _QUALIFIER.match(first) and "," in first:
        head = _tidy(first.split(",")[0])
        last = head.split()[-1].lower() if head.split() else ""
        # Not a head that opens a clause the comma would leave unfinished («sul quale un
        # signore», «69: a fairly soft»).
        open_clause = ":" in head or _unfinished_relative(head)
        if _content_words(head) >= config.MEANING_MIN_WORDS + 1 and last not in _FUNCTION_WORDS and not open_clause:
            variants.append(head)
    for v in dict.fromkeys(variants):
        v = v.rstrip(".").strip(" ;:,")
        if not v or len(v) > config.MEANING_MAX:
            continue
        if v.count("(") != v.count(")") or v.count("[") != v.count("]") or v.count('"') % 2:
            continue
        words = [w for w in v.split() if re.search(r"\w", w)]
        while words and words[-1].lower().strip(".,;") in _GRAMMAR:
            words.pop()
        v = " ".join(words) if words and v.split()[-1].lower().strip(".,;") in _GRAMMAR else v
        content = [w for w in words if w.lower().strip(".,;:()'\"«»") not in _FUNCTION_WORDS]
        if len(content) < config.MEANING_MIN_WORDS:
            continue
        # A fragment: a qualifier with nothing after it («Nella specie dei mammiferi»), or a
        # phrase whose end was a bracket taken away («…en el medievo entre»).
        if _QUALIFIER.match(v) and "," not in v and not _VERBISH.search(v):
            continue
        if words and words[-1].lower().strip(".,;:") in _FUNCTION_WORDS:
            continue
        if leaks(v, avoid, stems):
            continue
        return v[0].upper() + v[1:]
    return None


def _unfinished_relative(head: str) -> bool:
    """Whether a head ends inside a relative clause that has its subject and not yet its verb: «sul
    quale un signore» (the comma opened an aside before the verb), not «dove si accentrano attività
    amministrative» nor «che ama»."""
    words = head.split()
    marks = [i for i, w in enumerate(words) if i > 0 and _RELATIVE.fullmatch(w.strip(",.;:").lower())]
    if not marks:
        return False
    rest = words[marks[-1] + 1 :]
    return 0 < len(rest) <= 3 and rest[0].lower() in _DETERMINERS


def _content_words(text: str) -> int:
    return sum(
        1
        for w in text.split()
        if re.search(r"\w", w) and w.lower().strip(".,;:()'\"«»") not in _FUNCTION_WORDS | _GRAMMAR
    )


def clean(text: str, headword: str = "") -> str | None:
    """A gloss as a meaning (`fit`), the headword avoided."""
    return fit(text, (headword,) if headword else ())


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


def _ordered(entries, pos: tuple = ()) -> list:
    """A word's entries of the part of speech asked, when it has one (*libro*, a noun, is never
    read through *book*, the verb); otherwise its common parts of speech first."""
    same = [e for e in entries if e.pos in pos]
    if same:
        return same
    return [e for e in entries if e.pos in lexicon.SOLUTION_POS] + [
        e for e in entries if e.pos not in lexicon.SOLUTION_POS
    ]


def names_only(text: str) -> bool:
    """Whether a gloss only names the word in other words (*aiuto*, «help, assistance, aid»): a
    translation, not a definition. Every part of it, between commas, is three words at most."""
    parts = [p for p in re.split(r"[,;]", _BRACKETS.sub(" ", text)) if p.strip()]
    return all(len(p.split()) <= 3 for p in parts)


def _readable_senses(entry, prefer=frozenset()) -> list:
    """An entry's own senses a meaning may come from, by their position in the entry: its first
    sense and the finer senses under it (a word's first sense is its commonest, and a later one
    read in its place shows *oveja* as mutton), and the senses `prefer` names; none the editions
    label, none of a form."""
    if entry.pos in lexicon.NOT_WORDS:
        return []
    own = [
        (i, s)
        for i, s in enumerate(entry.senses)
        if not (s.get("f") or s.get("a") or lexicon._FORM_TAGS & set(s.get("t", ())))
    ]
    if not own:
        return []
    head = own[0][1].get("h") or own[0][1].get("g")
    family = [
        x
        for x in own
        if x is own[0]
        or (x[1].get("h") and x[1].get("h") == head and not _DRIFT_TAGS & set(x[1].get("t", ())))
    ]
    chosen = family[: config.MEANING_SENSES] + [x for x in own if (id(entry), x[0]) in prefer and x not in family]
    return [(i, s) for i, s in chosen if not s.get("r")]


def _overlap(a: str, b: str) -> int:
    words = lambda t: {w for w in re.findall(r"[^\W\d_]{3,}", _fold(t))}  # noqa: E731
    return len(words(a) & words(b))


_SENSE_NUMBERS = re.compile(r"^#?\s*(\d+)(?:\s*[-–]\s*(\d+))?((?:\s*,\s*\d+)*)\s*$")


def _numbered(label: str) -> list[int]:
    """The senses a label names by number, from 1 («#1–2», «#3»), as the Spanish edition writes."""
    m = _SENSE_NUMBERS.match(label.strip())
    if not m:
        return []
    first = int(m.group(1))
    last = int(m.group(2)) if m.group(2) else first
    rest = [int(x) for x in re.findall(r"\d+", m.group(3) or "")]
    return list(range(first, last + 1)) + rest


def aligned_senses(entries, g: str, word: str) -> set:
    """The senses, as (entry, position), whose translation table holds the word of language g: a
    table hung from a sense names it; a label of numbers names them («#1–2»); any other label is
    matched to the sense it shares the most words with."""
    out = set()
    for e in entries:
        own = [(i, s) for i, s in enumerate(e.senses) if not s.get("f")]
        for row in e.translations:
            if row[0] != g or row[1] != word:
                continue
            numbers = _numbered(row[2]) if row[2] else []
            if len(row) > 3 and row[3] >= 0:
                out.add((id(e), row[3]))
            elif numbers:
                out.update((id(e), n - 1) for n in numbers if 0 < n <= len(e.senses))
            elif row[2] and own:
                # Two words in common at least: one shared word names the wrong sense too often
                # (*back*, «a support or resource in reserve», for *dorso*).
                best = max(own, key=lambda x: _overlap(row[2], x[1].get("g", "")))
                if _overlap(row[2], best[1].get("g", "")) >= 2:
                    out.add((id(e), best[0]))
    return out


def names_refused(wikt, lang: str, text: str) -> bool:
    """Whether a meaning holds a word no guess of its language may be: such a word is written
    nowhere, a definition included."""
    return any(lexicon.refused(wikt, lang, t) for t in re.findall(r"[^\W\d_]+", text.lower()))


def own_extract(lang: str) -> str:
    """The extract where a language's words are defined in that language."""
    return "en.en" if lang == "en" else lang


def definition(
    wikt, lang: str, word: str, pos: tuple = (), prefer=frozenset(), avoid=(), only_preferred: bool = False
) -> str | None:
    return definition_with_pos(wikt, lang, word, pos, prefer, avoid, only_preferred)[0]


def definition_with_pos(
    wikt, lang: str, word: str, pos: tuple = (), prefer=frozenset(), avoid=(), only_preferred: bool = False
) -> tuple:
    """A word's own definition, in its own edition (the English edition for English): from the
    senses `prefer` names first, then from an entry of the same part of speech (*bowel*, a noun,
    reads *intestino* the noun), then the rest.

    Without `avoid`, the word is defined in its own language, shown once the game is over (no hint
    reads it: hints are for a foreign word): a definition may then share the word's stem (*voter*,
    «someone who votes»), not name the word itself."""
    entries = _ordered(wikt.entries(own_extract(lang), lang, word), pos)
    stems = bool(avoid)
    avoid = tuple(avoid) or (word,)
    readable = [(e, i, s) for e in entries for i, s in _readable_senses(e, prefer)]
    preferred = [x for x in readable if (id(x[0]), x[1]) in prefer]
    # Of homographs (*cuna*, a cradle, and *cuna*, a language), the first entry is the word.
    first = [x for x in readable if entries and x[0] is entries[0] and x not in preferred]
    readable = preferred if only_preferred else preferred + first
    for e, _, s in readable:
        text = fit(s.get("g", ""), avoid, stems)
        if text and not names_refused(wikt, lang, text):
            return text, e.pos
    if only_preferred:
        return None, None
    # The Italian edition sometimes writes a headword's sense only as its translation table's
    # label: the first table's, the first sense's.
    for e in entries[:1]:
        first = e.translations[0] if e.translations else None
        if first and first[2] and not first[2].startswith("#"):
            text = fit(_PARENS.sub(" ", first[2]), avoid, stems)
            if text and not names_refused(wikt, lang, text):
                return text, e.pos
    return None, None


def _first_definition(wikt, lang: str, word: str, pos: tuple = ()) -> str | None:
    return definition(wikt, lang, word, pos)


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

    def native_senses(self, g: str, word: str, pos: tuple = ()) -> set:
        """For a word defined in its own edition (Italian, Spanish): the senses whose translation
        table holds the English edition's first gloss of it. An edition orders its homographs by
        their origin, not by use (*avión*, the bird, before *avión*, the aeroplane); the English
        edition's first sense says which one is the word."""
        if g == "en":
            return set()
        english: list = []
        for e in _ordered(self.wikt.entries(f"en.{g}", g, word), pos)[:1]:
            for s in lexicon.lemma_senses(e)[:1]:
                english = list(s.get("k", ())) + _split_gloss(s.get("g", ""), "en")
        out: set = set()
        entries = self.wikt.entries(own_extract(g), g, word)
        for t in english:
            out |= aligned_senses(entries, "en", t)
        return out

    def has_pos(self, u: str, word: str, pos: tuple) -> bool:
        """Whether a translation has an entry of the word's part of speech in its own edition: it
        comes first, so a noun is not explained through a verb (*clout*, influence, read through
        *rattoppare*), nor a verb through a noun (*count* through *conde*)."""
        return not pos or any(e.pos in pos for e in self.wikt.entries(own_extract(u), u, word))

    def native_translations(self, g: str, u: str, word: str, pos: tuple = ()) -> set:
        """The words the word's own edition translates it with, in the senses the English edition's
        first gloss names (`native_senses`); none when it names none."""
        senses = self.native_senses(g, word, pos)
        if not senses:
            return set()
        out = set()
        for e in self.wikt.entries(own_extract(g), g, word):
            for row in e.translations:
                if row[0] != u:
                    continue
                numbers = _numbered(row[2]) if row[2] else []
                at = [row[3]] if len(row) > 3 and row[3] >= 0 else [n - 1 for n in numbers]
                if any((id(e), i) in senses for i in at):
                    out.add(row[1])
        return out

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

    def gloss(self, g: str, u: str, word: str, pos: tuple = ()) -> Gloss:
        if not pos:
            pos = tuple(
                dict.fromkeys(
                    e.pos for e in lexicon.headword_entries(self.wikt, g, word) if lexicon.lemma_senses(e)
                )
            )
        if g == u:
            text, used = definition_with_pos(self.wikt, g, word, pos, self.native_senses(g, word, pos))
            return Gloss([], text, None, [], used)
        # A translation no guess may be is never shown (the labels, `lexicon.refused`).
        ranked = [c for c in self.candidates(g, u, word) if not lexicon.refused(self.wikt, u, c["word"])]
        # The translations the word's own edition gives for the sense it is (`native_senses`) come
        # first: *avión* reads *aeroplane* before *martin*, the bird of its other entry.
        native = self.native_translations(g, u, word, pos)
        ranked.sort(key=lambda c: (0 if c["word"] in native else 1, 0 if self.has_pos(u, c["word"], pos) else 1))
        agreed = [c["word"] for c in ranked if len(c["sources"]) >= 2][:3]
        single = [c["word"] for c in ranked if reliable_candidate(c)][:1]
        reliable = agreed or single
        avoid = (word,)
        meaning = None
        via = None
        used = None
        # Through the translation, in the sense whose table holds the word; with no reliable one,
        # through what one source gives for the word's first sense (the meaning is shown, the
        # translation is not: it stays unreliable).
        through = reliable or [c["word"] for c in ranked if c["first"]][: config.MEANING_THROUGH_SINGLE]
        for n, t in enumerate(through):
            # The first translation is read in its own first sense when no table names one; a
            # later one only in a sense its table names (*canna* is never read through *porro*'s
            # music).
            entries = self.wikt.entries(own_extract(u), u, t)
            aligned = aligned_senses(entries, g, word)
            meaning, used = definition_with_pos(self.wikt, u, t, pos, aligned, avoid, only_preferred=n > 0)
            if meaning:
                via = t
                break
        # Else the U edition's own gloss of the word, in its first sense, when it defines rather
        # than names (the English edition glosses most Italian words with their translations).
        if meaning is None:
            for e in _ordered(self.wikt.entries(edition_extract(u, g), g, word), pos):
                for _, s in _readable_senses(e)[:1]:
                    text = fit(s.get("g", ""), avoid)
                    if text and not names_only(text) and not names_refused(self.wikt, u, text):
                        meaning, used = text, e.pos
                if meaning:
                    break
        return Gloss(reliable, meaning, via, ranked, used)
