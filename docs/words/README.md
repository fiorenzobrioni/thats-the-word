# The words: the report

Written by `tools/words/build.py` (PLANNING.md §6, §11 Phase 1): do not edit it by hand. It answers
VISION's questions 3 (the licences), 4 (the meanings' quality) and 5 (the size) with numbers;
the samples a person reads before the owner's go are in [`samples/`](samples/), and the words a
person decides on (the deny lists) in [`review/`](review/).

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
**solutions** are the lemmas a game may hide (§6.2); the **glossary** describes the solutions and
every lemma a guess may be or belong to.

| | Accepted | from the spelling dictionary | from Wiktionary only | Solutions | Glossary words |
|---|---|---|---|---|---|
| Italian | 8,314 | 6,101 | 2,213 | 1,691 | 3,383 |
| English | 22,919 | 6,584 | 16,335 | 3,612 | 6,457 |
| Spanish | 13,281 | 9,003 | 4,278 | 2,484 | 5,569 |

**The common words that must be accepted** (`tools/words/lists/common_*.txt`, 200 a language,
lemmas and inflected forms): 
Italian 200 of 200; English 200 of 200; Spanish 200 of 200.

### What keeps a lemma out of the solutions

Each lemma of the right length is checked in this order; the first reason that holds keeps it
out. The lists are in [`review/`](review/).

| Reason | Italian | English | Spanish |
|---|---|---|---|
| an inflected form (every entry opens with one) | 43 | 123 | 56 |
| archaic, obsolete, rare or regional in every sense | 180 | 1,372 | 258 |
| not a common word's part of speech (a name, an interjection, an affix...) | 20 | 138 | 67 |
| not in the spelling dictionary | 252 | 1,690 | 777 |
| offensive or vulgar (Wiktionary's tags) | 30 | 266 | 91 |
| outside the Italian alphabet's 21 letters | 143 | 0 | 0 |
| spelled two ways (British and American) | 0 | 82 | 0 |
| the deny list | 1 | 3 | 3 |
| too rare (not in wordfreq's list) | 433 | 5,359 | 1,769 |

## The levels

By the solution's Zipf frequency in wordfreq (§6.3): Base from 4.5, Intermediate from 3.5, Advanced from 2.7, Complete below. Cumulative (§15 Open 12): a level
plays its own words and those of the levels before it.

| | Base | Intermediate | Advanced | Complete | Daily pool (Base and Intermediate) |
|---|---|---|---|---|---|
| Italian | 270 | 506 | 484 | 431 | 776 |
| English | 434 | 868 | 1,036 | 1,274 | 1,302 |
| Spanish | 301 | 608 | 705 | 870 | 909 |

Examples, the most common of each level:

- **Italian**. Base: *anche*, *stato*, *tutti*, *tutto*, *fatto*, *prima*; Intermediate: *abito*, *bomba*, *cassa*, *email*, *mappa*, *poeta*; Advanced: *colon*, *mecca*, *pappa*, *salmo*, *solco*, *colei*; Complete: *clava*, *gorgo*, *truce*, *uscio*, *anice*, *asola*.
- **English**. Base: *about*, *their*, *there*, *which*, *would*, *other*; Intermediate: *faced*, *pride*, *rough*, *smell*, *topic*, *pants*; Advanced: *jenny*, *teddy*, *molly*, *silva*, *brock*, *monte*; Complete: *adder*, *algal*, *allay*, *bream*, *coder*, *degas*.
- **Spanish**. Base: *sobre*, *entre*, *desde*, *hasta*, *hacer*, *ahora*; Intermediate: *extra*, *turno*, *vacío*, *ciclo*, *conde*, *falso*; Advanced: *dotar*, *jaque*, *malla*, *pizca*, *sesgo*, *soler*; Complete: *apnea*, *bemol*, *ojiva*, *picón*, *talud*, *visir*.

## The meanings and translations (VISION Q4)

On the solutions, for each pair (game language → interface language): a **translation** is
shown when two sources agree on it, or when one translation table gives it for the word's first
sense (one translation only, marked *single* in the samples); a **meaning** is a definition in
the interface language (§6.4). The daily pool is the words most players meet.

| Game → interface | Solutions | Translation | of which agreed | Meaning | Either | Daily pool: translation | Daily pool: meaning |
|---|---|---|---|---|---|---|---|
| Italian → Italian | 1,691 | (the same language) | | 87% | 87% | | 96% |
| Italian → English | 1,691 | 86% | 80% | 98% | 98% | 94% | 99% |
| Italian → Spanish | 1,691 | 69% | 39% | 64% | 69% | 82% | 79% |
| English → Italian | 3,612 | 65% | 46% | 61% | 67% | 88% | 87% |
| English → English | 3,612 | (the same language) | | 100% | 100% | | 100% |
| English → Spanish | 3,612 | 68% | 41% | 64% | 70% | 87% | 88% |
| Spanish → Italian | 2,484 | 67% | 36% | 61% | 67% | 85% | 83% |
| Spanish → English | 2,484 | 85% | 77% | 97% | 97% | 93% | 99% |
| Spanish → Spanish | 2,484 | (the same language) | | 92% | 92% | | 96% |

## The daily schedules

Per language, 3,654 dailies: ten years from the provisional epoch,
2026-11-12 (§15 Open 10). Append-only: a rebuild keeps every entry and only adds.
Each word comes once before any comes twice, so a word returns after the whole pool has passed.

| | Dailies | Pool | A word returns after | Words that left the solutions |
|---|---|---|---|---|
| Italian | 3,654 | 776 | about 2.1 years | none |
| English | 3,654 | 1,302 | about 3.6 years | none |
| Spanish | 3,654 | 909 | about 2.5 years | none |

## The size (VISION Q5)

The files, stored uncompressed in the APK so they are read in place (§6.6, ADR 0003):

| File | Italian | English | Spanish |
|---|---|---|---|
| `accepted-5.bin` | 32 KB | 90 KB | 52 KB |
| `daily.bin` | 14 KB | 14 KB | 14 KB |
| `forms-5.bin` | 124 KB | 219 KB | 242 KB |
| `gloss-en.bin` | 133 KB | 484 KB | 212 KB |
| `gloss-es.bin` | 191 KB | 361 KB | 405 KB |
| `gloss-it.bin` | 192 KB | 313 KB | 250 KB |
| `solutions-5.bin` | 7 KB | 14 KB | 10 KB |
| **Total** | **692 KB** | **1,496 KB** | **1,185 KB** |

All three languages: **3.3 MB** in the APK.
