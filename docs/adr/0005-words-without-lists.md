# ADR 0005: The words without lists, and their meanings (Phase 1b)

- Status: accepted by the owner's answers (PLANNING §15, 14 Nov 2026, «Phase 1b: the words
  again»); the samples and the daily pool's size wait for the owner's go (§15, Open 23, 24).
  Amends ADR 0003's decisions 4 (what keeps a word out) and 7 (the meanings).
- Date: 2026-11-14

## Context

ADR 0003 kept a word out of the solutions by Wiktionary's tags and by a deny list per language,
written by hand and read by a person; a meaning was the first gloss found, cut at 160 characters
with an ellipsis, and a solution needed a meaning in its own language or English only. The owner
asked for no list of offensive, vulgar or inappropriate words anywhere in the repository, a search
of it finding none; for a coarse word to be no word of the game at all; and for every solution to
have a short, whole definition in Italian, English and Spanish.

No free dictionary is clean by construction (every general one describes the coarse words too),
and of the free ones only Wiktionary has definitions in the three languages: Wikidata's lexemes
gloss a word mostly in its own language, the Open Multilingual Wordnet's Italian and Spanish have
few definitions, BabelNet is non-commercial, the publishers' dictionaries have no licence to
reuse. Wiktionary's editions label their senses, each in its own way.

## Decisions

1. **The labels decide, and no word is named.** `tools/words/pipeline/register.py` reads a
   sense's tags, usage notes and categories, and an entry's categories, as three kinds: coarse
   (vulgar, obscene, offensive, a slur, a swear word), derogatory (derogatory, pejorative) and
   intimate (the editions' topics of sex). Its patterns name labels, never words; it is tested on
   made-up words. Every edition that has a word is read (the English one, its own, and the third,
   which glosses it).
2. **A solution** has no coarse or derogatory sense in any edition, and no intimate first sense.
   This is strict by the owner's choice: words whose one sense in many is labelled (*palla*,
   *colpo*, *rosso*) are guesses, never words to find.
3. **A refused word** is in no file: not accepted as a guess, not described, not written as a
   translation or inside a meaning. A word is refused when most of the editions that have it open
   with a coarse sense, or one edition's first entry is mostly coarse, or every edition opens
   with a harsh sense; a form goes with its lemmas, or with its Hunspell stems when Wiktionary
   does not list it. One edition's order alone is not a verdict (the Spanish edition opens
   *bombear* with its slang), nor a usage note on a first sense («a veces despectivo»).
4. **The report counts, never names.** What the labels keep out, and what is refused, is a number
   in `docs/words/`; the other reasons keep their lists. Nothing in the repository is a list of
   words to avoid, and a test fails if `tools/words/lists/` holds anything but the common words.
5. **A meaning** is a whole definition of at most 100 characters, two words of meaning at least
   (articles and prepositions not counted), never cut with an ellipsis: shortened at a clause, a
   sentence or, last, the first comma when what comes before stands alone; read in the word's
   first sense or a finer sense under it, in the homograph the English edition's first sense
   names, or in the sense a translation table names (a label of numbers, «#1–2», included);
   never a pointer to another word or form, never a gloss leaning on the one before, never naming
   a foreign word or its stem (the definition is a hint). A foreign word's meaning is its
   translation's definition first, the interface edition's own gloss only when it defines rather
   than translates. Every solution has one in each of the three languages, or is not a solution.
6. **Every length from 3 to 8 is built and measured; five ship.** The rules do not depend on the
   length (`config.LENGTHS`), so 1.1's Bridge and 1.2's variable length add a length by shipping
   its files (`config.SHIPPED_LENGTHS`), and freeze its daily schedule then.
7. **The daily schedules were written anew once**, before 1.0, with these rules (the owner's
   answer); they are append-only again from now.

## Consequences

- Fewer solutions (717 Italian, 1,355 English, 1,068 Spanish, from 1,691, 3,612 and 2,484) and a
  smaller daily pool (436, 764, 559), most for want of a reliable translation between Italian or
  Spanish and the other languages, or of a short first gloss: Open 24 asks the owner how large the
  pool should be.
- The files keep ADR 0003's formats; 2.7 MB for the three languages.
- What the labels miss is not caught: a word the editions do not label is a word of the game.
  The rule is read and changed in `register.py` and `lexicon.py`, never by listing a word.
