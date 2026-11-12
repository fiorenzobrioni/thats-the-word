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
    words: dict  # language -> LanguageWords
    glosses: dict  # (g, u) -> {display: Gloss}
    schedules: dict  # language -> list of keys
    dropped: dict  # language -> schedule words no longer solutions
    common_missing: dict  # language -> common words not accepted


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
    than the key itself (a lemma written as its key needs no record: its glossary is by key)."""
    records = {}
    for key in sorted(lw.accepted):
        displays = lw.displays.get(key, [key])
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
    gl = glossary.Glossary(wikt)
    files: dict = {}
    words, glosses, schedules, dropped, common_missing = {}, {}, {}, {}, {}
    days = schedule.days_needed()
    for g in config.LANGUAGES:
        lw = lexicon.build(g, wikt, config.LENGTH, log)
        words[g] = lw
        common = lexicon.read_list(f"common_{g}.txt")
        common_missing[g] = [w for w in common if folding.fold(w, g) not in lw.accepted]

        files[asset(g, f"accepted-{config.LENGTH}.bin")] = pack.accepted(g, config.LENGTH, lw.accepted)
        files[asset(g, f"solutions-{config.LENGTH}.bin")] = pack.solutions(g, config.LENGTH, lw.solutions)
        files[asset(g, f"forms-{config.LENGTH}.bin")] = pack.table("forms", g, config.LENGTH, forms_records(lw))

        daily_path = asset(g, "daily.bin")
        existing = pack.read_daily(daily_path.read_bytes(), g) if daily_path.exists() else []
        pool = [s.key for s in lw.solutions if s.level in config.DAILY_LEVELS]
        planned = schedule.extend(g, existing, pool, days)
        schedule.check_append_only(existing, planned)
        schedules[g] = planned
        solution_keys = {s.key for s in lw.solutions}
        dropped[g] = sorted({k for k in planned if k not in solution_keys})
        files[daily_path] = pack.daily(g, config.LENGTH, planned)

        described = glossary_words(lw)
        # A word of the schedule that left the solutions keeps its glossary entry.
        display_of = {s.key: s.display for s in lw.solutions}
        for k in dropped[g]:
            described.append(display_of.get(k, lw.displays.get(k, [k])[0]))
        described = sorted(set(described))
        for u in config.LANGUAGES:
            table = {}
            records = {}
            for d in described:
                x = gl.gloss(g, u, d)
                table[d] = x
                if x.translations or x.meaning:
                    records[d] = [x.translations, x.meaning or "", x.meaning_via or ""]
            glosses[(g, u)] = table
            files[asset(g, f"gloss-{u}.bin")] = pack.table(
                "glossary", g, 0, records, folding.LANGUAGE_CODE[u]
            )
            log(f"{g}->{u}: {len(records)} of {len(described)} words described")
    built = Built(files, words, glosses, schedules, dropped, common_missing)
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
