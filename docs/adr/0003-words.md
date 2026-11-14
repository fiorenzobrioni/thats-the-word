# ADR 0003: The words, their sources and their files (Phase 1)

- Status: accepted (the owner's go on the meanings, the deny lists and the daily pool, 12 Nov 2026);
  decisions 4 and 7 amended by ADR 0005 (no lists, the meanings' rules, 14 Nov 2026)
- Date: 2026-11-12

## Context

The words are the game's riskiest part (PLANNING.md §6, §14): three languages' lists, four levels,
meanings and translations in nine pairs, from free sources, reproducibly, small enough to ship.
Phase 1 builds them before anything is drawn, and measures them (`docs/words/README.md`).

## Decisions

1. **One pipeline, standard library only** (`tools/words/`, Python 3.11+). Nothing to install, so
   it runs the same anywhere; Hunspell's affixes are expanded by its own small expander (prefixes,
   suffixes, conditions, cross product, continuation classes two deep), MessagePack (wordfreq's
   data) by its own small decoder. It never ships.
2. **The sources** (`tools/words/pipeline/sources.py`, each pinned by the SHA-256 of its
   content): LibreOffice's Italian Hunspell dictionary (5.1.1, GPL-3.0); the RLA-ES Spanish
   dictionaries for Spain, Mexico and the Río de la Plata (2.9, tri-licensed, used under
   GPL-3.0-or-later); SCOWL 2020.12.07, the last release in its classic expanded form (its 2026
   release is a database to build, not lists); wordfreq 3.1.1's data (CC BY-SA 4.0), read from its
   wheel; Wiktionary through wiktextract (kaikki.org): the English edition's Italian, Spanish and
   English entries, and the Italian and Spanish editions (CC BY-SA 4.0 and GFDL, used under CC BY-SA
   4.0, one-way compatible with GPL-3.0). The cache is outside the repository; only what the
   pipeline writes is committed.
3. **Wiktionary's extracts are not archived upstream**: kaikki.org republishes them weekly. The
   pins hold while a cache holds the files; a repin is one command, and changes meanings, never a
   daily (decision 6). Committing reduced snapshots was weighed and left out: PLANNING §6.1 keeps
   sources out of the repository, and the outputs are committed anyway.
4. **Accepted** is the spelling dictionary's forms (Hunspell expanded; SCOWL to size 70, both
   spellings and the first variants) widened by every single word Wiktionary has in the language,
   never a name, an abbreviation or a Roman numeral. **Solutions** are lemmas (the English edition
   decides, the language's own edition where the English has no entry) of a common part of speech,
   not an inflected form first, not offensive (the first sense, or half the senses), not dated in
   every sense, in wordfreq's list, in the spelling dictionary, within the Italian alphabet's 21
   letters (§15 Open 6, confirmed by the owner), written the same in British and American English, and not on
   the language's deny list. One display form a key: the most frequent.
5. **Levels** by Zipf frequency, the same thresholds in the three languages (Base 4.5,
   Intermediate 3.5, Advanced 2.7); an English word whose name SCOWL lists as more common is
   Advanced at least, since wordfreq folds case.
6. **The daily schedule** is drawn from Base and Intermediate, every word once before any twice,
   each entry by a generator seeded with the schedule's seed and the entry's number; append-only,
   enforced by the build (it reads the committed file and refuses to change an entry). Ten years
   from the epoch, 1 November 2026 (fixed by the owner on 12 Nov 2026, PLANNING §15, Open 10;
   the entries are by number, so fixing it changed no byte).
7. **Translations** are shown when two of four sources agree (the interface edition's gloss, the
   game edition's table, the interface edition's tables that list the word, the English edition's
   tables through English), or, one at most, when a single table gives it for the word's first
   sense (marked *single* in the samples, for the owner to keep or drop: `SINGLE_TABLE_TRANSLATIONS`).
   The meaning is the interface edition's gloss when it defines, otherwise the translation's own
   definition, from an entry of the same part of speech.
8. **The files**, one directory a game language (`core/words/src/main/assets/words/<language>/`):
   `accepted-5.bin`, `solutions-5.bin`, `daily.bin`, `forms-5.bin`, `gloss-<interface>.bin`. Every
   file has a sixteen-byte header (magic `TTW` and its kind, version, language, length, interface,
   count) and big-endian numbers. A word is packed five bits a letter, first letter highest, in four
   bytes up to six letters and in eight from seven to twelve (the width follows from the header's
   length: decided now, PLANNING §15, 12 Nov 2026, so 1.1's and 1.2's lengths need no new format
   and leave the five-letter files as they are), so sorted integers are sorted words, found by
   binary search with no decoding. Solutions keep their level in the two low bits. The daily XORs
   each word with the first four bytes of SHA-256("ttw-daily/<language>/<number>"): no plain text,
   and any day decoded alone. Forms and glossaries are text records (UTF-8, fields by U+001F, list
   items by U+001E) behind an offset table, sorted by their key's bytes. A form's record gives its
   written forms and their lemmas, a solution's own written form first (*città* before *citta*:
   PLANNING §15, 12 Nov 2026, «Phase 2: the game engine»); a lemma's glossary record its translations, its meaning and the
   word the meaning was read through.
9. **Uncompressed.** The files are stored uncompressed in the APK (`noCompress`, Phase 2) and read
   in place: 3.3 MB for the three languages (VISION Q5). Compressing the glossaries would save about
   two thirds of that at the cost of an inflater per lookup and byte-exact builds tied to a zlib
   version; not worth it at this size.

## Consequences

- `:core:words` (Phase 2) maps the assets and hands their bytes to the decoder in `:core:domain`,
  pure Kotlin, tested on the JVM against files its tests write.
- The owner read the samples and the deny lists and gave the go (PLANNING §15, 12 Nov 2026,
  «Phase 1 closed»): the single-table translations stay, the seven denied words stay out, the
  daily pool stays Base and Intermediate.
