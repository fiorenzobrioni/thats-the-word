"""The words' report and the pages a person reads (PLANNING.md §6.7, §11 Phase 1).

Written by the pipeline, never by hand: `docs/words/README.md` (the sources, the counts, the
levels, the coverage of the nine pairs, the sizes), `docs/words/samples/` (50 words per pair, to
be read before the owner's go), and `docs/words/review/` (the words a person decides on: every
solution with an offensive or vulgar sense somewhere, and what each filter kept out).
"""

import collections
import datetime
import random

from . import config, folding, glossary, lexicon, schedule, sources

NAMES = {"it": "Italian", "en": "English", "es": "Spanish"}


def _pct(n: int, d: int) -> str:
    return f"{100 * n / d:.0f}%" if d else "–"


def _kb(n: int) -> str:
    return f"{n / 1024:,.0f} KB" if n >= 1024 else f"{n} B"


def _cell(text) -> str:
    return (text or "").replace("|", "\\|").replace("\n", " ")


def coverage(built, g: str, u: str, solutions) -> tuple[int, int, int, int]:
    table = built.glosses[(g, u)]
    tr = sum(1 for s in solutions if table[s.display].translations)
    agreed = sum(1 for s in solutions if table[s.display].agreed)
    me = sum(1 for s in solutions if table[s.display].meaning)
    either = sum(1 for s in solutions if table[s.display].translations or table[s.display].meaning)
    return tr, agreed, me, either


def render(built) -> dict:
    files = {}
    files[config.REPORT / "README.md"] = _readme(built).encode("utf-8")
    for g in config.LANGUAGES:
        for u in config.LANGUAGES:
            files[config.REPORT / "samples" / f"{g}-{u}.md"] = _samples(built, g, u).encode("utf-8")
        files[config.REPORT / "review" / f"{g}.md"] = _review(built, g).encode("utf-8")
    return files


def _readme(built) -> str:
    out = []
    w = out.append
    w("# The words: the report\n")
    w("Written by `tools/words/build.py` (PLANNING.md §6, §11 Phase 1): do not edit it by hand. It answers")
    w("VISION's questions 3 (the licences), 4 (the meanings' quality) and 5 (the size) with numbers;")
    w("the samples a person reads before the owner's go are in [`samples/`](samples/), and the words a")
    w("person decides on (the deny lists) in [`review/`](review/).\n")

    w("## The sources (VISION Q3)\n")
    w("Every source is fetched at the version below and checked against its SHA-256 before a build;")
    w("each licence is compatible with the app's GPL-3.0-or-later, and each source is credited in")
    w("`licenses/README.md` and, from the phase that draws it, in Settings, About.\n")
    w("| Source | Version | Licence | Used for |")
    w("|---|---|---|---|")
    for o in sources.ORIGINS:
        w(f"| [{o.name}]({o.home}) | {o.version} | {o.licence} | {o.note} |")
    w("\n<details><summary>The files and their checksums</summary>\n")
    w("| Key | File | SHA-256 of the content | Bytes |")
    w("|---|---|---|---|")
    for key, s in sources.SOURCES.items():
        w(f"| `{key}` | {s.url} | `{s.sha256}` | {s.size:,} |")
    w("\n</details>\n")

    w("## The words\n")
    w(f"Five letters, {config.ATTEMPTS} attempts (VISION §8). **Accepted** is every form a guess may be;")
    w("**solutions** are the lemmas a game may hide (§6.2); the **glossary** describes the solutions and")
    w("every lemma a guess may be or belong to.\n")
    w("| | Accepted | from the spelling dictionary | from Wiktionary only | Solutions | Glossary words |")
    w("|---|---|---|---|---|---|")
    for g in config.LANGUAGES:
        lw = built.words[g]
        spelling = lw.accepted_by_source.get("Hunspell", lw.accepted_by_source.get("SCOWL", 0))
        wikt_only = len(lw.accepted) - spelling
        described = len(built.glosses[(g, g)])
        w(f"| {NAMES[g]} | {len(lw.accepted):,} | {spelling:,} | {wikt_only:,} | {len(lw.solutions):,} | {described:,} |")
    w("")
    w("**The common words that must be accepted** (`tools/words/lists/common_*.txt`, 200 a language,")
    w("lemmas and inflected forms): ")
    parts = []
    for g in config.LANGUAGES:
        missing = built.common_missing[g]
        n = len(lexicon.read_list(f"common_{g}.txt"))
        parts.append(f"{NAMES[g]} {n - len(missing)} of {n}" + (f" (missing: {', '.join(missing)})" if missing else ""))
    w("; ".join(parts) + ".\n")

    w("### What keeps a lemma out of the solutions\n")
    w("Each lemma of the right length is checked in this order; the first reason that holds keeps it")
    w("out. The lists are in [`review/`](review/).\n")
    reasons = sorted({r for g in config.LANGUAGES for r in built.words[g].excluded})
    w("| Reason | " + " | ".join(NAMES[g] for g in config.LANGUAGES) + " |")
    w("|---|" + "---|" * len(config.LANGUAGES))
    for r in reasons:
        w(f"| {r} | " + " | ".join(f"{len(built.words[g].excluded.get(r, [])):,}" for g in config.LANGUAGES) + " |")
    w("")

    w("## The levels\n")
    w("By the solution's Zipf frequency in wordfreq (§6.3): Base from "
      + ", Intermediate from ".join(
          f"{t}" for t in config.LEVEL_THRESHOLDS["it"][:2]
      ) + f", Advanced from {config.LEVEL_THRESHOLDS['it'][2]}, Complete below. Cumulative (§15 Open 12): a level")
    w("plays its own words and those of the levels before it.\n")
    w("| | " + " | ".join(config.LEVEL_NAMES) + " | Daily pool (Base and Intermediate) |")
    w("|---|" + "---|" * (len(config.LEVEL_NAMES) + 1))
    for g in config.LANGUAGES:
        c = collections.Counter(s.level for s in built.words[g].solutions)
        cells = [f"{c[i]:,}" for i in range(len(config.LEVEL_NAMES))]
        w(f"| {NAMES[g]} | " + " | ".join(cells) + f" | {sum(c[i] for i in config.DAILY_LEVELS):,} |")
    w("")
    w("Examples, the most common of each level:\n")
    for g in config.LANGUAGES:
        sols = built.words[g].solutions
        line = []
        for i, name in enumerate(config.LEVEL_NAMES):
            top = sorted((s for s in sols if s.level == i), key=lambda s: (-s.zipf, s.display))[:6]
            line.append(f"{name}: " + ", ".join(f"*{s.display}*" for s in top))
        w(f"- **{NAMES[g]}**. " + "; ".join(line) + ".")
    w("")

    w("## The meanings and translations (VISION Q4)\n")
    w("On the solutions, for each pair (game language → interface language): a **translation** is")
    w("shown when two sources agree on it, or when one translation table gives it for the word's first")
    w("sense (one translation only, marked *single* in the samples); a **meaning** is a definition in")
    w("the interface language (§6.4). The daily pool is the words most players meet.\n")
    w("| Game → interface | Solutions | Translation | of which agreed | Meaning | Either | Daily pool: translation | Daily pool: meaning |")
    w("|---|---|---|---|---|---|---|---|")
    for g in config.LANGUAGES:
        sols = built.words[g].solutions
        pool = [s for s in sols if s.level in config.DAILY_LEVELS]
        for u in config.LANGUAGES:
            tr, agreed, me, either = coverage(built, g, u, sols)
            ptr, _, pme, _ = coverage(built, g, u, pool)
            if g == u:
                w(f"| {NAMES[g]} → {NAMES[u]} | {len(sols):,} | (the same language) | | {_pct(me, len(sols))} | {_pct(me, len(sols))} | | {_pct(pme, len(pool))} |")
            else:
                w(f"| {NAMES[g]} → {NAMES[u]} | {len(sols):,} | {_pct(tr, len(sols))} | {_pct(agreed, len(sols))} | {_pct(me, len(sols))} | {_pct(either, len(sols))} | {_pct(ptr, len(pool))} | {_pct(pme, len(pool))} |")
    w("")

    w("## The daily schedules\n")
    w(f"Per language, {len(next(iter(built.schedules.values()))):,} dailies: ten years from the epoch,")
    w(f"{config.EPOCH.isoformat()} (§15 Open 10). Append-only: a rebuild keeps every entry and only adds.")
    w("Each word comes once before any comes twice, so a word returns after the whole pool has passed.\n")
    w("| | Dailies | Pool | A word returns after | Words that left the solutions |")
    w("|---|---|---|---|---|")
    for g in config.LANGUAGES:
        sched = built.schedules[g]
        pool = sum(1 for s in built.words[g].solutions if s.level in config.DAILY_LEVELS)
        w(f"| {NAMES[g]} | {len(sched):,} | {pool:,} | about {pool / 365.25:.1f} years | {', '.join(built.dropped[g]) or 'none'} |")
    w("")

    w("## The size (VISION Q5)\n")
    w("The files, stored uncompressed in the APK so they are read in place (§6.6, ADR 0003):\n")
    by_lang = collections.defaultdict(dict)
    for path, data in built.files.items():
        if config.ASSETS in path.parents:
            by_lang[path.parent.name][path.name] = len(data)
    names = sorted({n for d in by_lang.values() for n in d})
    w("| File | " + " | ".join(NAMES[g] for g in config.LANGUAGES) + " |")
    w("|---|" + "---|" * len(config.LANGUAGES))
    for n in names:
        w(f"| `{n}` | " + " | ".join(_kb(by_lang[g].get(n, 0)) for g in config.LANGUAGES) + " |")
    totals = [sum(by_lang[g].values()) for g in config.LANGUAGES]
    w("| **Total** | " + " | ".join(f"**{_kb(t)}**" for t in totals) + " |")
    w(f"\nAll three languages: **{sum(totals) / 1024 / 1024:.1f} MB** in the APK.\n")
    return "\n".join(out)


def _samples(built, g: str, u: str) -> str:
    sols = [s for s in built.words[g].solutions]
    rng = random.Random(f"samples/{g}/{u}")
    pool = sorted(s.display for s in sols if s.level in config.DAILY_LEVELS)
    rest = sorted(s.display for s in sols if s.level not in config.DAILY_LEVELS)
    picked = sorted(rng.sample(pool, min(35, len(pool)))) + sorted(rng.sample(rest, min(15, len(rest))))
    level = {s.display: config.LEVEL_NAMES[s.level] for s in sols}
    table = built.glosses[(g, u)]
    out = [f"# {NAMES[g]} words, read in {NAMES[u]}\n"]
    out.append("Written by `tools/words/build.py`: 50 solutions drawn at random (35 from the daily pool, 15")
    out.append("from the other levels), with what the end card, the hints and the notebook would show.")
    out.append("Read each line and mark what is wrong; the owner's go on their quality closes PLANNING")
    out.append("§11 Phase 1 (VISION Q4).\n")
    if g == u:
        out.append("| Word | Level | Definition |")
        out.append("|---|---|---|")
        for d in picked:
            x = table[d]
            out.append(f"| *{d}* | {level[d]} | {_cell(x.meaning) or '—'} |")
    else:
        out.append("| Word | Level | Translation | Meaning |")
        out.append("|---|---|---|---|")
        for d in picked:
            x = table[d]
            tr = ", ".join(x.translations)
            if tr and not x.agreed:
                tr += " *(single)*"
            meaning = x.meaning or ""
            if meaning and x.meaning_via:
                meaning = f"({x.meaning_via}) {meaning}"
            out.append(f"| *{d}* | {level[d]} | {_cell(tr) or '—'} | {_cell(meaning) or '—'} |")
    return "\n".join(out) + "\n"


def _review(built, g: str) -> str:
    lw = built.words[g]
    out = [f"# {NAMES[g]}: the words a person decides on\n"]
    out.append("Written by `tools/words/build.py`. A word moved out of the solutions by hand goes in")
    out.append(f"`tools/words/lists/deny_{g}.txt`, with a comment saying why; then rebuild.\n")
    out.append("## Solutions with an offensive or vulgar sense somewhere\n")
    out.append("Kept, because neither their first sense nor half of their senses is (the rule in")
    out.append("`lexicon.is_offensive`). Read them: a daily is the same for everyone.\n")
    out.append("| Word | Level | The sense |")
    out.append("|---|---|---|")
    for s in lw.solutions:
        if s.display in lw.flagged:
            out.append(f"| *{s.display}* | {config.LEVEL_NAMES[s.level]} | {_cell(lw.flagged[s.display])[:120]} |")
    out.append("")
    out.append("## Kept out\n")
    for reason, words in lw.excluded.items():
        out.append(f"<details><summary>{reason}: {len(words):,}</summary>\n")
        out.append(", ".join(words))
        out.append("\n</details>\n")
    return "\n".join(out) + "\n"
