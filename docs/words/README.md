# The words: the report

Written by `tools/words/build.py` (PLANNING.md §6, §11 Phase 1): do not edit it by hand. It answers
VISION's questions 3 (the licences), 4 (the meanings' quality) and 5 (the size) with numbers;
the samples a person reads before the owner's go are in [`samples/`](samples/), and what each
filter kept out of the solutions in [`review/`](review/). No list of words to avoid exists: the
editions' own labels keep a word out, and a word they keep out is counted here, never named.

## The sources (VISION Q3)

Every source is fetched at the version below and checked against its SHA-256 before a build;
each licence is compatible with the app's GPL-3.0-or-later, and each source is credited in
`licenses/README.md` and, from the phase that draws it, in Settings, About.

| Source | Version | Licence | Used for |
|---|---|---|---|
| [Italian Hunspell dictionary (LibreItalia)](https://github.com/LibreOffice/dictionaries/tree/master/it_IT) | 5.1.1 (7 Nov 2022), LibreOffice/dictionaries at c0865a85eb | GPL-3.0 | Every Italian form the game accepts, expanded from its affixes by this pipeline. |
| [Spanish Hunspell dictionaries (RLA-ES)](https://github.com/sbosio/rla-es) | 2.9, es_ES, es_MX and es_AR, LibreOffice/dictionaries at c0865a85eb | GPL-3.0-or-later, LGPL-3.0-or-later or MPL-1.1, at the user's choice: used under GPL-3.0-or-later | Every Spanish form the game accepts, expanded from its affixes by this pipeline. |
| [SCOWL (Spell Checker Oriented Word Lists), Kevin Atkinson](http://wordlist.aspell.net/) | 2020.12.07 | SCOWL's own permissive licence (MIT-like, with the notices of its sources kept) | Every English form the game accepts, American and British, up to size 70. |
| [wordfreq, Robyn Speer](https://github.com/rspeer/wordfreq) | 3.1.1 (its data frozen in 2024) | Code Apache-2.0 (not used); data CC BY-SA 4.0 | How common a word is: the four levels. |
| [Wiktionary, through wiktextract (kaikki.org)](https://kaikki.org/) | the English edition's dump of 1 Oct 2026; the Italian and Spanish editions' of Oct 2026 | CC BY-SA 4.0 (and GFDL); used under CC BY-SA 4.0, one-way compatible with GPL-3.0 | Which words are lemmas, their meanings and translations, their written forms, the tags that keep a word out. |

<details><summary>The files and their checksums</summary>

| Key | File | SHA-256 of the content | Bytes |
|---|---|---|---|
| `it_IT.dic` | https://raw.githubusercontent.com/LibreOffice/dictionaries/c0865a85eb67dc377f2cf8e8fe2c4fa9e53f0dcb/it_IT/it_IT.dic | `bae1e3501dcd2a923669592493b3fde6c02aae7c7aab83bf5e5b49077e73dd64` | 1,295,083 |
| `it_IT.aff` | https://raw.githubusercontent.com/LibreOffice/dictionaries/c0865a85eb67dc377f2cf8e8fe2c4fa9e53f0dcb/it_IT/it_IT.aff | `951afaa19272f13555b8823e8bcf9ccf78f8fe1a07835bdfb912ab3e4d537c2b` | 70,054 |
| `it_IT.readme` | https://raw.githubusercontent.com/LibreOffice/dictionaries/c0865a85eb67dc377f2cf8e8fe2c4fa9e53f0dcb/it_IT/README_it_IT.txt | `34c3e93595cf3cf5f3afc9bc0d98eea12593750383f1e82ed1d3ca29f9681283` | 38,902 |
| `es_ES.dic` | https://raw.githubusercontent.com/LibreOffice/dictionaries/c0865a85eb67dc377f2cf8e8fe2c4fa9e53f0dcb/es/es_ES.dic | `6975dddec3d5d2c676069537bc67b4b5f786c65c5d4cf6703a82acf779ac9ec1` | 715,989 |
| `es_ES.aff` | https://raw.githubusercontent.com/LibreOffice/dictionaries/c0865a85eb67dc377f2cf8e8fe2c4fa9e53f0dcb/es/es_ES.aff | `e73a9bf8e1383f4986a5dc9e2fbed49371c0c61f511c626d15586bd433c1cad9` | 169,202 |
| `es_MX.dic` | https://raw.githubusercontent.com/LibreOffice/dictionaries/c0865a85eb67dc377f2cf8e8fe2c4fa9e53f0dcb/es/es_MX.dic | `44ce35af220962c68f97962776639bef271f7d90a85ba924b539a36e33315f82` | 723,507 |
| `es_MX.aff` | https://raw.githubusercontent.com/LibreOffice/dictionaries/c0865a85eb67dc377f2cf8e8fe2c4fa9e53f0dcb/es/es_MX.aff | `d966cb748e4a688ed75ec84b50c0835ac438e5325ee4fce703a093794f9cba7e` | 160,081 |
| `es_AR.dic` | https://raw.githubusercontent.com/LibreOffice/dictionaries/c0865a85eb67dc377f2cf8e8fe2c4fa9e53f0dcb/es/es_AR.dic | `4fe3e193425bb3841ee218d36a9fc95d85b8daa8909363bdbba55a2f9f4e3d1d` | 707,817 |
| `es_AR.aff` | https://raw.githubusercontent.com/LibreOffice/dictionaries/c0865a85eb67dc377f2cf8e8fe2c4fa9e53f0dcb/es/es_AR.aff | `e9a96514295f8664db270d024c345700ccb9410a384eb71aec0fe5f0931d8c70` | 171,527 |
| `es.license` | https://raw.githubusercontent.com/LibreOffice/dictionaries/c0865a85eb67dc377f2cf8e8fe2c4fa9e53f0dcb/es/LICENSE.md | `2b70fd5cb8d3b34cc6ae322c969557a2270b99017594addb2531ef5ffd9e1b17` | 991 |
| `scowl` | https://downloads.sourceforge.net/wordlist/scowl-2020.12.07.tar.gz | `5587667caa20c4891390c2d42dbb4d5c4c3f41bee77af1457ece3ba23fb859cc` | 2,569,810 |
| `wordfreq` | https://files.pythonhosted.org/packages/24/61/62835c475d69872d30689f284497853fe33fe1d6dd18f57346d13305861d/wordfreq-3.1.1-py3-none-any.whl | `4b1c6ecffc6198be3396d5cf871c4423ca71c907c231348d352dd54d62b97473` | 56,834,549 |
| `wiktionary.en.it` | https://kaikki.org/dictionary/Italian/kaikki.org-dictionary-Italian.jsonl | `f112deb9747b7f33f4247da4999dfba0aa739bb4aaff9dd42fdf0615918a9c11` | 779,439,733 |
| `wiktionary.en.es` | https://kaikki.org/dictionary/Spanish/kaikki.org-dictionary-Spanish.jsonl | `336903d1290968d77bd361c620d7d799ad7a7a9bc6ad353ec35954f2e15fbe6f` | 1,119,672,965 |
| `wiktionary.en.en` | https://kaikki.org/dictionary/English/kaikki.org-dictionary-English.jsonl | `ce2b80e5ad1132f14cedd2f531a606e1f0c6aeab422d50515e88db13a16a59a7` | 3,431,211,190 |
| `wiktionary.it` | https://kaikki.org/dictionary/downloads/it/it-extract.jsonl.gz | `9f35d5ac30f2fbab4774eb3e81ec6394806bd2723f802b405e7e9901185d5229` | 43,849,715 |
| `wiktionary.es` | https://kaikki.org/dictionary/downloads/es/es-extract.jsonl.gz | `0a7a02456ecf34dd3419e0d5adf4f18da5531487dde31829d126afe86eade861` | 103,226,090 |

</details>

## The words

Five letters, 6 attempts (VISION §8). **Accepted** is every form a guess may be;
**refused** is what the spelling dictionaries or Wiktionary hold that no guess may be, because
an edition labels its first sense, or most of its senses, coarse or derogatory (an inflection
with its lemma); **solutions** are the lemmas a game may hide (§6.2), each with a whole
definition in the three languages; the **glossary** describes the solutions and every lemma a
guess may be or belong to.

| | Accepted | from the spelling dictionary | from Wiktionary only | Refused | Solutions | Glossary words |
|---|---|---|---|---|---|---|
| Italian | 8,260 | 6,067 | 2,193 | 54 | 717 | 26,915 |
| English | 22,528 | 6,516 | 16,012 | 391 | 1,355 | 37,211 |
| Spanish | 13,105 | 8,922 | 4,183 | 176 | 1,068 | 32,312 |

**The common words that must be accepted** (`tools/words/lists/common_*.txt`, 200 a language,
lemmas and inflected forms): 
Italian 200 of 200; English 200 of 200; Spanish 200 of 200.

### What keeps a lemma out of the solutions

Each lemma of the right length is checked in this order; the first reason that holds keeps it
out. The labels come first, so a word they keep out is in no other list. The lists are in
[`review/`](review/).

| Reason | Italian | English | Spanish |
|---|---|---|---|
| a coarse, derogatory or intimate sense (the editions' labels; counted, never listed) | 79 | 307 | 211 |
| an inflected form (every entry opens with one) | 37 | 117 | 52 |
| archaic, obsolete, rare or regional in every sense | 176 | 1,369 | 258 |
| no whole definition of two lines in Italian, English and Spanish | 914 | 2,074 | 1,271 |
| not a common word's part of speech (a name, an interjection, an affix...) | 20 | 135 | 65 |
| not in the spelling dictionary | 250 | 1,673 | 752 |
| outside the Italian alphabet's 21 letters | 142 | 0 | 0 |
| spelled two ways (British and American) | 0 | 78 | 0 |
| too rare (not in wordfreq's list) | 432 | 5,340 | 1,754 |

## The levels

By the solution's Zipf frequency in wordfreq (§6.3): Base from 4.5, Intermediate from 3.5, Advanced from 2.7, Complete below. Cumulative (§15 Open 12): a level
plays its own words and those of the levels before it.

| | Base | Intermediate | Advanced | Complete | Daily pool (Base and Intermediate) |
|---|---|---|---|---|---|
| Italian | 180 | 256 | 175 | 106 | 436 |
| English | 293 | 471 | 398 | 193 | 764 |
| Spanish | 222 | 337 | 304 | 205 | 559 |

Examples, the most common of each level:

- **Italian**. Base: *anche*, *stato*, *tutto*, *fatto*, *parte*, *molto*; Intermediate: *abito*, *bomba*, *cassa*, *mappa*, *poeta*, *ovest*; Advanced: *pappa*, *salmo*, *solco*, *edile*, *osare*, *panca*; Complete: *clava*, *gorgo*, *uscio*, *anice*, *asola*, *bignè*.
- **English**. Base: *about*, *their*, *which*, *would*, *other*, *after*; Intermediate: *pride*, *rough*, *pants*, *sharp*, *chart*, *mount*; Advanced: *willy*, *cling*, *icing*, *jihad*, *lapse*, *mural*; Complete: *adder*, *coder*, *degas*, *rivet*, *tardy*, *twirl*.
- **Spanish**. Base: *sobre*, *entre*, *desde*, *hasta*, *hacer*, *ahora*; Intermediate: *extra*, *vacío*, *ciclo*, *conde*, *falso*, *norma*; Advanced: *dotar*, *jaque*, *apego*, *curro*, *aleta*, *chavo*; Complete: *apnea*, *bemol*, *visir*, *brida*, *cuate*, *gnomo*.

## The lengths (for 1.1 and 1.2)

Every length the later versions may play (VISION §8: Bridge in 1.1, variable length in 1.2),
built with the same rules and shipped in the app already (§15, 14 Nov 2026); five letters are
1.0's game and have its daily schedule. The glossary column is the solutions' meanings and
translations in the three interface languages (the glossary files hold them with every guess's
lemma, all lengths together).

| | Letters | Accepted | Refused | Solutions | Daily pool (Base and Intermediate) | Solutions' glossaries |
|---|---|---|---|---|---|---|
| Italian | 3 | 541 | 1 | 65 | 53 | 13 KB |
| Italian | 4 | 2,618 | 12 | 297 | 212 | 64 KB |
| Italian | 5 (1.0's game) | 8,260 | 54 | 717 | 436 | 155 KB |
| Italian | 6 | 20,399 | 86 | 1,037 | 517 | 230 KB |
| Italian | 7 | 47,569 | 204 | 1,601 | 644 | 356 KB |
| Italian | 8 | 87,072 | 272 | 1,736 | 657 | 390 KB |
| English | 3 | 2,516 | 39 | 283 | 217 | 60 KB |
| English | 4 | 9,372 | 167 | 957 | 664 | 212 KB |
| English | 5 (1.0's game) | 22,528 | 391 | 1,355 | 764 | 297 KB |
| English | 6 | 42,763 | 671 | 1,878 | 846 | 414 KB |
| English | 7 | 67,571 | 1,099 | 1,965 | 815 | 439 KB |
| English | 8 | 90,890 | 1,378 | 1,959 | 715 | 439 KB |
| Spanish | 3 | 841 | 16 | 96 | 74 | 20 KB |
| Spanish | 4 | 3,912 | 68 | 458 | 280 | 101 KB |
| Spanish | 5 (1.0's game) | 13,105 | 176 | 1,068 | 559 | 233 KB |
| Spanish | 6 | 31,055 | 405 | 1,507 | 621 | 329 KB |
| Spanish | 7 | 61,701 | 621 | 1,871 | 699 | 417 KB |
| Spanish | 8 | 98,522 | 831 | 1,895 | 672 | 428 KB |

## The meanings and translations (VISION Q4)

On the solutions, for each pair (game language → interface language): a **translation** is
shown when two sources agree on it, or when one translation table gives it for the word's first
sense (one translation only, marked *single* in the samples); a **meaning** is a whole
definition in the interface language (§6.4), at most 100 characters, never cut: every solution
has one in the three languages. The daily pool is the words most players meet.

| Game → interface | Solutions | Translation | of which agreed | Meaning | Either | Daily pool: translation | Daily pool: meaning |
|---|---|---|---|---|---|---|---|
| Italian → Italian | 717 | (the same language) | | 100% | 100% | | 100% |
| Italian → English | 717 | 99% | 98% | 100% | 100% | 100% | 100% |
| Italian → Spanish | 717 | 99% | 63% | 100% | 100% | 100% | 100% |
| English → Italian | 1,355 | 98% | 81% | 100% | 100% | 99% | 100% |
| English → English | 1,355 | (the same language) | | 100% | 100% | | 100% |
| English → Spanish | 1,355 | 99% | 73% | 100% | 100% | 99% | 100% |
| Spanish → Italian | 1,068 | 99% | 60% | 100% | 100% | 100% | 100% |
| Spanish → English | 1,068 | 99% | 97% | 100% | 100% | 100% | 100% |
| Spanish → Spanish | 1,068 | (the same language) | | 100% | 100% | | 100% |

How long the solutions' meanings are, in characters:

| Interface | Shortest | Median | Longest |
|---|---|---|---|
| Italian | 8 | 44 | 100 |
| English | 8 | 52 | 100 |
| Spanish | 8 | 51 | 100 |

## The daily schedules

Per language, 3,654 dailies: ten years from the epoch,
2026-11-01 (§15 Open 10). Append-only: a rebuild keeps every entry and only adds
(written anew once, before 1.0, with these rules: §15, 14 Nov 2026).
Each word comes once before any comes twice, so a word returns after the whole pool has passed.

| | Dailies | Pool | A word returns after | Words that left the solutions |
|---|---|---|---|---|
| Italian | 3,654 | 436 | about 1.2 years | none |
| English | 3,654 | 764 | about 2.1 years | none |
| Spanish | 3,654 | 559 | about 1.5 years | none |

## The size (VISION Q5)

The files, stored uncompressed in the APK so they are read in place (§6.6, ADR 0003):

| File | Italian | English | Spanish |
|---|---|---|---|
| `accepted-3.bin` | 2 KB | 10 KB | 3 KB |
| `accepted-4.bin` | 10 KB | 37 KB | 15 KB |
| `accepted-5.bin` | 32 KB | 88 KB | 51 KB |
| `accepted-6.bin` | 80 KB | 167 KB | 121 KB |
| `accepted-7.bin` | 372 KB | 528 KB | 482 KB |
| `accepted-8.bin` | 680 KB | 710 KB | 770 KB |
| `daily.bin` | 14 KB | 14 KB | 14 KB |
| `forms-3.bin` | 4 KB | 21 KB | 7 KB |
| `forms-4.bin` | 31 KB | 74 KB | 53 KB |
| `forms-5.bin` | 123 KB | 215 KB | 239 KB |
| `forms-6.bin` | 337 KB | 467 KB | 680 KB |
| `forms-7.bin` | 838 KB | 871 KB | 1,524 KB |
| `forms-8.bin` | 1,478 KB | 1,249 KB | 2,709 KB |
| `gloss-en.bin` | 1,716 KB | 1,955 KB | 1,738 KB |
| `gloss-es.bin` | 782 KB | 1,236 KB | 1,231 KB |
| `gloss-it.bin` | 870 KB | 1,184 KB | 829 KB |
| `solutions-3.bin` | 276 B | 1 KB | 400 B |
| `solutions-4.bin` | 1 KB | 4 KB | 2 KB |
| `solutions-5.bin` | 3 KB | 5 KB | 4 KB |
| `solutions-6.bin` | 4 KB | 7 KB | 6 KB |
| `solutions-7.bin` | 13 KB | 15 KB | 15 KB |
| `solutions-8.bin` | 14 KB | 15 KB | 15 KB |
| **Total** | **7,405 KB** | **8,875 KB** | **10,509 KB** |

All three languages: **26.2 MB** in the APK.
