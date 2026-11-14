# tools/words: the words, made from free sources

That's The Word's words are generated, never edited by hand (PLANNING.md §6, CLAUDE.md). This
directory is the pipeline that makes them: Python 3.11 or later, its standard library alone,
nothing to install.

```bash
python3 tools/words/build.py
```

```bash
python3 tools/words/build.py --check
```

```bash
python3 -m unittest discover -s tools/words/tests -t tools/words
```

The first writes every file; the second rebuilds in memory and fails if a committed file differs
(a second run must write the same bytes); the third runs the pipeline's tests (the last of them
read the committed files, as the app will).

## What it does

1. **Fetch** (`pipeline/sources.py`): every source at a pinned version, checked against the
   SHA-256 of its content, into a cache outside the repository (`~/.cache/thats-the-word/words`,
   or `$THATSTHEWORD_WORDS_CACHE`; about 1 GB). A source whose content differs is refused.
2. **Reduce** (`pipeline/wiktionary.py`): the five Wiktionary extracts (5 GB of JSON) are read
   once and kept in the cache as what the game reads: single words of the three languages, each
   sense's gloss, tags, links and the lemma it is a form of, and the translation tables into the
   other two languages. About ten minutes, once.
3. **The words** (`pipeline/lexicon.py`): the accepted set (Hunspell expanded by
   `pipeline/hunspell.py` for Italian and Spanish, SCOWL for English, widened by Wiktionary's
   single words), less what the labels refuse (`pipeline/register.py`); the written forms of
   each key form; the lemmas; the solutions and why each other lemma is kept out; the levels
   (`pipeline/frequency.py` reads wordfreq's data). For every length of `config.LENGTHS`.
4. **The glossary** (`pipeline/glossary.py`): for the nine pairs, the translations (agreed by two
   sources, or given by one table for the first sense) and the meanings (whole, 100 characters
   at most, of the word's first sense); a solution needs one in each of the three languages.
5. **The daily schedules** (`pipeline/schedule.py`): append-only; the committed file is read
   first and no entry in it ever changes.
6. **The files** (`pipeline/pack.py`, its formats in `docs/adr/0003-words.md`) into
   `core/words/src/main/assets/words/`, and **the report** (`pipeline/report.py`) into
   `docs/words/`: the counts, the levels, the coverage, the sizes, the samples a person reads and
   the words a person decides on.

A build after the first takes about five minutes (every length from 3 to 8) and about 7.5 GB of
memory.

## What a person writes

`lists/` holds the only words written by hand: `common_<language>.txt`, 200 common words a
language, lemmas and inflected forms, that must all be accepted (the build fails if one is not).

**There is no list of words to avoid** (PLANNING.md §6.2, ADR 0005): the editions' own labels keep
a word out of the solutions, or out of the game, in `pipeline/register.py` (the labels, three
kinds: coarse, derogatory, intimate) and `pipeline/lexicon.py` (what they decide). Neither names a
word; the report counts what they keep out and never lists it; a test fails if `lists/` holds
anything else. A word wrongly kept or wrongly let in is answered by a rule, never by a list.

The choices (the levels' thresholds, the daily's levels and seed, the epoch, the Spanish
dictionaries read, the lengths built and shipped, the meanings' length and words) are in
`pipeline/config.py`, each with its reason. Change one, rebuild, read the report and the samples,
and record it in PLANNING.md §15.

## The lengths

Every length in `config.LENGTHS` (3 to 8) is built with the same rules and measured in the
report's «The lengths»; only `config.SHIPPED_LENGTHS` (five letters, 1.0's) become files in
`core/words/src/main/assets/words/`. A later version ships a length by adding it there; its daily
schedule is written then, and is append-only from its first build.

## When a source moves

kaikki.org republishes its Wiktionary extracts every week and does not keep the old ones. Once a
pinned extract can no longer be fetched (on a machine whose cache lacks it):

```bash
python3 tools/words/build.py fetch --repin wiktionary.en.en wiktionary.en.it wiktionary.en.es wiktionary.it wiktionary.es
```

prints the new checksums, to be written in `pipeline/sources.py` with the dump's date in
`ORIGINS`; then rebuild and read the report. Meanings and translations may change; a daily never
does (the schedule is append-only, and a word that left the solutions keeps its day and its
glossary entry, named in the report).
