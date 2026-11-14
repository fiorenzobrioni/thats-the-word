"""Every file the words make, from the pinned sources (PLANNING.md §6, §11 Phase 1).

`run()` fetches what the cache lacks (and refuses a source whose content differs from its pin),
reduces Wiktionary once, builds each language's words, their glossaries in the nine pairs and the
daily schedules, and returns every file it would write, by path, as bytes: `write()` writes them,
`check()` compares them with the files in the repository.
"""

import collections
import dataclasses
import pathlib
import random

from . import config, folding, glossary, lexicon, pack, report, schedule, sources


@dataclasses.dataclass
class Built:
    files: dict  # path -> bytes
    words: dict  # language -> LanguageWords, of the shipped length
    glosses: dict  # (g, u) -> {display: Gloss}
    schedules: dict  # language -> list of keys
    dropped: dict  # language -> schedule words no longer solutions
    common_missing: dict  # language -> common words not accepted
    lengths: dict = dataclasses.field(default_factory=dict)  # (language, length) -> LanguageWords
    measured: dict = dataclasses.field(default_factory=dict)  # (language, length) -> the solutions' glossaries' bytes
    # (language, length) -> what keeping solutions to nouns, adjectives and verbs leaves out: the
    # words that would otherwise have been solutions, and those of them in the daily's levels
    kinds_cost: dict = dataclasses.field(default_factory=dict)


NO_MEANING = "no whole definition of two lines in Italian, English and Spanish"


class Glosses:
    """The glossary's answers, each asked once: a solution's three meanings decide whether it is
    one, and the same answers are what the files keep."""

    def __init__(self, gl: glossary.Glossary):
        self.gl = gl
        self.cache: dict = {}

    def get(self, g: str, u: str, display: str, pos: tuple = ()) -> glossary.Gloss:
        key = (g, u, display)
        if key not in self.cache:
            self.cache[key] = self.gl.gloss(g, u, display, pos)
        return self.cache[key]


def keep_described(lw: lexicon.LanguageWords, glosses: Glosses) -> None:
    """Keeps as solutions only the words with a meaning in every interface language (§6.4), and
    of them the most frequent `config.MAX_SOLUTIONS` of the length."""
    missing = set()
    for s in lw.solutions:
        for u in config.LANGUAGES:
            if not glosses.get(lw.language, u, s.display, (s.pos,)).meaning:
                missing.add(s.key)
                break
    lexicon.without(lw, missing, NO_MEANING)
    cap = config.MAX_SOLUTIONS.get(lw.length)
    if cap is not None and len(lw.solutions) > cap:
        ranked = sorted(lw.solutions, key=lambda s: (-s.zipf, s.key))
        lexicon.without(lw, {s.key for s in ranked[cap:]}, f"beyond the {cap:,} most frequent of its length")


def record(g: str, u: str, gloss, pos: str | None) -> list:
    """A glossary record: the translations, the meaning, the word it was read through, and the
    part of speech in the interface language, when the meaning was read in the word's own."""
    label = config.PART_OF_SPEECH[u].get(pos) if pos and gloss.meaning and gloss.pos == pos else None
    return [gloss.translations, gloss.meaning or "", gloss.meaning_via or "", label or ""]


def asset(*parts: str) -> pathlib.Path:
    return config.ASSETS.joinpath(*parts)


def glossary_words(lw: lexicon.LanguageWords) -> list[str]:
    """The written forms a glossary describes: the solutions, every lemma among the accepted
    words the spelling dictionary knows, and every lemma such a form belongs to (of any length:
    *canti* → *cantare*)."""
    out = {s.display for s in lw.solutions}
    for key, forms in lw.displays.items():
        if key not in lw.spelling_keys:
            # A form only Wiktionary knows is accepted, but too rare to describe.
            continue
        for d in forms:
            if d in lw.lemmas:
                out.add(d)
            out.update(lw.lemma_of.get(d, ()))
    return sorted(out)


def forms_records(lw: lexicon.LanguageWords) -> dict:
    """Per accepted key, its written forms and the lemmas each is a form of, where that says more
    than the key itself (a lemma written as its key needs no record: its glossary is by key).

    A solution's own written form comes first in its record, the others after it in their order:
    the app reads a solution's display form there (PLANNING.md §4.1), and several lemmas can share
    a key (*citta* and *città*, *gatto* and *gattò*), the most frequent being the solution's."""
    solution_display = {s.key: s.display for s in lw.solutions}
    records = {}
    for key in sorted(lw.accepted):
        displays = lw.displays.get(key, [key])
        own = solution_display.get(key)
        if own is not None:
            displays = [own] + [d for d in displays if d != own]
        items = []
        informative = displays != [key]
        for d in displays:
            lemmas = [x for x in lw.lemma_of.get(d, ()) if x != d]
            if lemmas:
                informative = True
            items.append([d] + lemmas)
        if informative:
            records[key] = items
    return records


def run(log=print, wikt=None) -> Built:
    wrong = sources.fetch(log=lambda *a: None)
    if wrong:
        raise SystemExit(
            "These sources differ from their pins: " + ", ".join(wrong) +
            ". Run `build.py fetch --repin " + " ".join(wrong) + "` to take the current ones."
        )
    wikt = wikt or lexicon.Wiktionary(log=log)
    glosses = Glosses(glossary.Glossary(wikt))
    files: dict = {}
    words, tables, schedules, dropped, common_missing = {}, {}, {}, {}, {}
    lengths, measured, kinds_cost = {}, {}, {}
    days = schedule.days_needed()
    for g in config.LANGUAGES:
        for length in config.LENGTHS:
            lw = lexicon.build(g, wikt, length, log)
            keep_described(lw, glosses)
            lengths[(g, length)] = lw
            kept_out = [
                (z, d) for d, (z, kind) in lw.set_aside.items()
                if z >= config.MIN_ZIPF
                and all(glosses.get(g, u, d, (kind,)).meaning for u in config.LANGUAGES)
            ]
            kinds_cost[(g, length)] = (
                len(kept_out),
                sum(1 for z, _ in kept_out if config.level_of(g, z) in config.DAILY_LEVELS),
                [d for _, d in sorted(kept_out, key=lambda x: (-x[0], x[1]))[:8]],
            )
            measured[(g, length)] = sum(
                len(pack.table("glossary", g, 0, {
                    s.display: record(g, u, x, s.pos)
                    for s in lw.solutions
                    for x in [glosses.get(g, u, s.display, (s.pos,))]
                }, folding.LANGUAGE_CODE[u]))
                for u in config.LANGUAGES
            )
            log(f"{g}-{length}: {len(lw.solutions)} solutions described in the three languages")
        lw = lengths[(g, config.LENGTH)]
        words[g] = lw
        common = lexicon.read_list(f"common_{g}.txt")
        common_missing[g] = [w for w in common if folding.fold(w, g) not in lw.accepted]

        # The daily schedule is five letters' (1.0's daily): a schedule is frozen once written, so a
        # length's comes with the mode that plays its dailies, not before.
        daily_path = asset(g, "daily.bin")
        existing = pack.read_daily(daily_path.read_bytes(), g) if daily_path.exists() else []
        pool = [s.key for s in lw.solutions if s.level in config.DAILY_LEVELS]
        planned = schedule.extend(g, existing, pool, days)
        schedule.check_append_only(existing, planned)
        schedules[g] = planned
        dropped[g] = sorted({k for k in planned if k not in {s.key for s in lw.solutions}})
        files[daily_path] = pack.daily(g, config.LENGTH, planned)

        # Every shipped length's words, and one glossary for all of them: it is read by written
        # form, whatever the form's length.
        described: set = set()
        pos_of: dict = {}
        for length in config.SHIPPED_LENGTHS:
            lx = lengths[(g, length)]
            files[asset(g, f"accepted-{length}.bin")] = pack.accepted(g, length, lx.accepted)
            files[asset(g, f"solutions-{length}.bin")] = pack.solutions(g, length, lx.solutions)
            files[asset(g, f"forms-{length}.bin")] = pack.table("forms", g, length, forms_records(lx))
            described.update(glossary_words(lx))
            pos_of.update({s.display: (s.pos,) for s in lx.solutions})
        # A word of the schedule that left the solutions keeps its glossary entry.
        display_of = {s.key: s.display for s in lw.solutions}
        for k in dropped[g]:
            described.add(display_of.get(k, lw.displays.get(k, [k])[0]))
        described = sorted(described)
        for u in config.LANGUAGES:
            table = {}
            records = {}
            for d in described:
                x = glosses.get(g, u, d, pos_of.get(d, ()))
                table[d] = x
                if x.translations or x.meaning:
                    records[d] = record(g, u, x, lexicon.word_kind(wikt, g, d))
            tables[(g, u)] = table
            files[asset(g, f"gloss-{u}.bin")] = pack.table("glossary", g, 0, records, folding.LANGUAGE_CODE[u])
            log(f"{g}->{u}: {len(records)} of {len(described)} words described")
    built = Built(files, words, tables, schedules, dropped, common_missing, lengths, measured, kinds_cost)
    files.update(report.render(built))
    return built


def write(built: Built) -> None:
    for path, data in sorted(built.files.items()):
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.exists() or path.read_bytes() != data:
            path.write_bytes(data)


def check(built: Built) -> list[pathlib.Path]:
    """The files whose bytes in the repository differ from a fresh build's."""
    return [p for p, data in sorted(built.files.items()) if not p.exists() or p.read_bytes() != data]


def sample(items, n: int, seed: str):
    items = sorted(items)
    return sorted(random.Random(seed).sample(items, min(n, len(items))))


def level_counts(lw) -> collections.Counter:
    return collections.Counter(s.level for s in lw.solutions)
