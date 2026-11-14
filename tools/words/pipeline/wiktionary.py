"""Wiktionary, as wiktextract extracts it (kaikki.org), reduced to what the game reads.

The extracts are large (the English edition's English entries alone are 3.4 GB of JSON), so they
are read once and reduced to a compact cache: single words of the three languages, with each
sense's first gloss, its tags, the lemma it is a form of, its links, and the translations into
the other two languages. Every later step reads the reduced files.

Five extracts, three editions:

- `en.it`, `en.es`, `en.en`: the English edition's Italian, Spanish and English entries. Its
  Italian and Spanish glosses are English (a translation and a meaning at once); its English
  entries carry translation tables into Italian and Spanish, sense by sense.
- `it`: the Italian edition. Italian words defined in Italian, with tables into English and
  Spanish; English and Spanish words glossed in Italian.
- `es`: the Spanish edition, the same for Spanish.
"""

import gzip
import json
import pathlib

from . import register, sources

EXTRACTS = {
    "en.it": "wiktionary.en.it",
    "en.es": "wiktionary.en.es",
    "en.en": "wiktionary.en.en",
    "it": "wiktionary.it",
    "es": "wiktionary.es",
}

# The edition each extract belongs to: the language its glosses are written in.
EDITION = {"en.it": "en", "en.es": "en", "en.en": "en", "it": "it", "es": "es"}

GAME_LANGUAGES = ("it", "en", "es")

# A reduced entry's version: a change here invalidates the cache.
REDUCED_VERSION = 4


def reduced_path(name: str) -> pathlib.Path:
    return sources.cache_dir() / "reduced" / f"v{REDUCED_VERSION}" / f"{name}.jsonl.gz"


def _category_names(obj) -> list[str]:
    names = []
    for c in obj.get("categories", ()) or ():
        if isinstance(c, dict):
            names.append(c.get("name", ""))
        elif isinstance(c, str):
            names.append(c)
    return names


def _translation(t, edition, sense_no=-1):
    """A translation row: its language, its word, the sense it translates (the table's label), and
    the position of that sense in the entry when the table hangs from one (-1 otherwise)."""
    code = t.get("lang_code") or t.get("code")
    if code not in GAME_LANGUAGES or code == edition and edition != "en":
        return None
    word = t.get("word")
    if not word:
        return None
    sense = t.get("sense")
    if sense is None and t.get("sense_index") is not None:
        sense = "#" + str(t.get("sense_index"))
    return [code, word, sense or "", sense_no]


def reduce_entry(d: dict, extract: str) -> dict | None:
    word = d.get("word")
    if not word or " " in word:
        return None
    lang = d.get("lang_code")
    if lang not in GAME_LANGUAGES:
        return None
    edition = EDITION[extract]
    senses = []
    entry_register = register.of_categories(_category_names(d))
    translations = []
    for t in d.get("translations", ()) or ():
        row = _translation(t, edition)
        if row:
            translations.append(row)
    for s in d.get("senses", ()) or ():
        glosses = s.get("glosses") or s.get("raw_glosses") or []
        gloss = glosses[-1] if glosses else ""
        tags = sorted(set((s.get("tags") or []) + (s.get("raw_tags") or [])))
        sense = {"g": gloss}
        labels = register.of_tags(tags) | register.of_categories(_category_names(s))
        if labels:
            sense["r"] = register.code(labels)
        if len(glosses) > 1:
            sense["h"] = glosses[0]
        if tags:
            sense["t"] = tags
        form_of = [f.get("word") for f in s.get("form_of", ()) or () if f.get("word")]
        if form_of:
            sense["f"] = form_of
        alt_of = [f.get("word") for f in s.get("alt_of", ()) or () if f.get("word")]
        if alt_of:
            sense["a"] = alt_of
        links = []
        for link in s.get("links", ()) or ():
            if isinstance(link, list) and link:
                links.append(link[0])
        if links:
            sense["k"] = links[:6]
        if s.get("sense_index") is not None:
            sense["i"] = str(s.get("sense_index"))
        for t in s.get("translations", ()) or ():
            row = _translation(t, edition, len(senses))
            if row:
                translations.append(row)
        senses.append(sense)
    forms = []
    for f in d.get("forms", ()) or ():
        form = f.get("form")
        tags = f.get("tags") or []
        if not form or " " in form or len(form) > 12:
            continue
        if any(t in tags for t in ("table-tags", "inflection-template", "romanization", "class")):
            continue
        forms.append(form)
    out = {"w": word, "l": lang, "p": d.get("pos", ""), "s": senses}
    if translations:
        out["tr"] = translations
    if forms:
        out["fm"] = sorted(set(forms))
    if entry_register:
        out["r"] = register.code(entry_register)
    return out


def reduce(extract: str, log=print) -> pathlib.Path:
    """Reads an extract and writes its reduced form into the cache, once."""
    target = reduced_path(extract)
    if target.exists():
        return target
    target.parent.mkdir(parents=True, exist_ok=True)
    source = sources.path(EXTRACTS[extract])
    part = target.with_suffix(".part")
    kept = 0
    with gzip.open(source, "rt", encoding="utf-8") as stream, gzip.GzipFile(
        part, "wb", mtime=0, compresslevel=6
    ) as out:
        for line in stream:
            try:
                d = json.loads(line)
            except json.JSONDecodeError:
                continue
            r = reduce_entry(d, extract)
            if r is not None:
                out.write((json.dumps(r, ensure_ascii=False, separators=(",", ":")) + "\n").encode())
                kept += 1
    part.replace(target)
    log(f"reduced {extract}: {kept} entries")
    return target


def entries(extract: str):
    with gzip.open(reduced_path(extract), "rt", encoding="utf-8") as stream:
        for line in stream:
            yield json.loads(line)


if __name__ == "__main__":
    import sys

    for name in sys.argv[1:] or EXTRACTS:
        reduce(name)
