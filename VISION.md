# That's The Word — Vision

> Status: approved by the owner, 12 November 2026. Written in English as the family writes its documents (the owner's answer, below); the owner's Italian original is in the repository's history (`0d7cacf`). The owner's answers to the plan's review are in «The owner's answers», at the end.

## 1. In one sentence

**That's The Word** is an Android game in which, every day, you look for a hidden word, in Italian, English and Spanish, until the moment you can say "there, that's the one". It is meant both as a daily pastime and as a way to exercise the vocabulary of a language you are learning. Premium quality, open source, entirely offline.

## 2. Name and voice

**That's The Word** is what you exclaim when the word you had on the tip of your tongue finally comes to mind: "there, that's the one". The name describes the best moment of the game, the one in which the hidden word is found, and it is the line with which the app greets every win.

- The name stays in English in every language of the interface.
- The victory line is rendered in the other languages in the same spirit (*Ecco la parola.*, *Esa es la palabra.*).
- The repository's name: `thats-the-word`.

Subtitle: **One word a day. Three languages.**

Tone of the texts: short, calm, a little theatrical. Short sentences, no exclamation marks, no gamer slang. Every text is written in the three interface languages with the same rhythm, not translated word for word.

## 3. Principles

1. **Few things, done very well.** One polished mode is better than three sketched.
2. **The foreign language is the heart.** What sets the app apart is being able to play in a language that is not your own without frustration, and to come out of it having learnt something.
3. **Offline and respectful.** No account, no ads, no tracking. The app does not ask for permission to access the Internet.
4. **Open.** Code under the GPL-3.0-or-later licence, language data from free and documented sources, the build produced by CI.
5. **An identity of its own.** Name, look, colours and word lists original, not taken from existing games.

## 4. Who it is for

- Whoever wants a carefully made word pastime without ads.
- Whoever studies Italian, English or Spanish and wants a light, daily exercise.

An honest promise: it is a vocabulary exercise, not a language course.

## 5. Languages

- **Interface**: Italian, English, Spanish. It follows the device's language and can be changed from the settings.
- **Game language**: Italian, English, Spanish, chosen independently of the interface and changeable at any time.
- Translations, definitions, hints and the surrounding texts are always shown **in the interface language**.
- **Playing in a foreign language**: when the game language differs from the interface's, the app turns on the practice tools (section 9).
- **Playing in your own language**: when the two languages are the same, the game is a classic pastime: no hints, and at the end of a game the definition instead of the translation.

## 6. Basic rules

- A hidden word to guess in a limited number of attempts (6 in the classic five-letter mode).
- Every attempt must be a valid word of the vocabulary.
- After every attempt each letter gets a feedback: right position, present elsewhere, absent.
- The feedback is given by **colour and shape together**, never by colour alone.

### Special characters

- **Italian**: accents are ignored when typing; the correct form (e.g. *città*) is shown at the end of the game.
- **Spanish**: **ñ** is a letter of its own, with its own key; accented vowels and the diaeresis are ignored when typing.
- **English**: both British and American spellings are accepted; words with two spellings are left out of the solutions.

## 7. The app's structure

| Screen | Content |
|---|---|
| **Start** | The word of the day in evidence for the active game language, a quick change of language, the calendar of the dailies, access to the other modes. |
| **Game** | Grid, keyboard with the letters' state, hints (if available), the end-of-game card. |
| **Notebook** | The words met, with search and review. |
| **Progress** | Statistics, streaks and milestones. |
| **Settings** | Languages, vocabulary level, Hard option, sounds, vibration, animations, backup, licences and attributions. |

## 8. Game modes

The names below are working names in English; their names in the three interface languages are an open question (§16, 2).

### Version 1.0: the core

| Mode | Description |
|---|---|
| **Daily** | One word a day for each language, the same for everyone. It is computed from the date on an ordered, frozen list, so without a server. It changes at local midnight. |
| **Archive** | A calendar to catch up on missed dailies or replay a specific date, from the app's first daily to today. Future dates cannot be played. The calendar shows for every day whether the word was solved, missed or not played. |
| **Free play** | Unlimited games, with a choice of vocabulary level. |
| **Hard option** | The clues obtained must be reused in the following attempts. Available in Daily and Free play. |

Everything at 5 letters and 6 attempts. Included from the start: vocabulary levels, hints, the end-of-game card, the Notebook, statistics, streaks, milestones, backup.

### Version 1.1: the language modes

| Mode | Description |
|---|---|
| **Translate** | A word is shown in the interface language; you guess its translation in the game language, with fewer attempts (about 4). It trains active recall. |
| **Bridge** | Three grids, the same concept in the three languages (*gatto / cat / gato*), with different lengths. Solving one reveals the meaning and helps with the others. It is the app's signature mode. |
| **Review** | Brings back the Notebook's words at growing intervals of days. |

### Version 1.2 and beyond: the variants

| Mode | Description |
|---|---|
| **Ladder** | A series of words with one pool of attempts for all of them; each solution becomes the first attempt of the next word. |
| **Several grids** | Two or four words to guess in parallel with the same attempts. |
| **Variable length** | Words from 4 to 7 letters. |
| **Liar** | In every row one feedback is false. |

The order reflects the cost: every new length multiplies the word lists to prepare. The number of attempts of the modes beyond the classic one is to be tuned by playing the prototypes.

## 9. Practising a language

### Vocabulary levels

The solutions are divided by frequency of use: **Base**, **Intermediate**, **Advanced**, **Complete**. In a foreign language you start from Base. The Daily always draws from common words, because it must be the same for everyone.

### Hints

Available only when playing in a language different from the interface's:

- **Translation**: shows the solution's translation, at the cost of one attempt.
- **Definition**: shows a definition in the interface language, at the cost of one attempt.
- **Letter**: reveals a letter in its correct position, at the cost of one attempt.

Games played with hints count for the streak but are marked in the statistics.

### End-of-game card

For the solution and for every word tried: meaning, translation into the interface language, the form with its correct accents. If a translation is missing, the definition alone is shown.

### Notebook

- Gathers the words met automatically, with translation, language and date.
- Marks which words were guessed, missed or solved with hints.
- Can be browsed and searched like a small personal dictionary.
- Feeds the Review mode.

## 10. Vocabularies

Goal: vocabularies **as wide as possible**, with a clear distinction between two sets.

| Set | Content | Criterion |
|---|---|---|
| **Accepted words** | Every valid form, inflected forms included (plurals, conjugations) | As wide as possible: fewer unfair refusals |
| **Solutions** | Base forms only, without proper nouns, abbreviations and offensive terms | Wide but ordered by level; rare words stay in Advanced and Complete |

### Planned sources

| Data | Source | Licence |
|---|---|---|
| Italian words | The Italian Hunspell dictionary (LibreOffice's version), expanded into every form | GPL 3 |
| Spanish words | The Spanish Hunspell dictionary | To be verified |
| English words | SCOWL or ENABLE | To be verified |
| Frequencies of use | wordfreq, OpenSubtitles lists, Leipzig Corpora | CC BY-SA 4.0 / CC BY 4.0 |
| Definitions and translations | Wiktionary, through the data extracted by kaikki.org | CC BY-SA and GFDL |

### Rules on the data

- The vocabularies are generated by a **reproducible script** included in the repository: sources, versions and filters are tracked.
- The data is distributed as separate files, each with its own licence and attribution, listed in a dedicated file and on a screen of the app.
- The daily solution lists are **frozen**: updates add at the end and never change the words already assigned to a date.
- The translations' coverage will be incomplete for rare words: words without a reliable translation do not enter Translate and Bridge.
- Bridge needs triples of concepts aligned in the three languages: generated automatically among the common words and then **reviewed by hand**.

## 11. Progression

- **Statistics** per language and mode: games, the share won, the distribution of attempts, the use of hints.
- **Daily streak**, per language. Only the words solved on their own day count: the dailies caught up from the Archive enter the statistics and the calendar but do not mend a broken streak.
- **Milestones**: goals that reward steadiness and learning, for example "7 dailies in a row", "first win in a foreign language", "100 words in the Notebook", "the daily solved in all three languages on the same day".
- **Nothing to unlock**: no themes or prizes; milestones are a recognition, not a key.
- **Backup**: export and import of statistics, milestones and the Notebook to a file, since there are no accounts.
- **Sharing** a result without revealing the word, in a graphic format of the app's own.

## 12. A premium experience

- **More care in the graphics and more animation** than in the sister apps, as befits a game.
- **Animations** for every key moment: a letter entered, the feedback revealed, an invalid word, a win, a milestone reached.
- Discreet **vibration** and **sounds**, each with its own switch.
- **A guided first game**: the rules are understood by playing, without an instructions screen.
- **Accessibility**: shapes besides colours, TalkBack support, resizable text, an option to reduce the animations.
- **Coherence with the sister apps**: style, typography and components shared with the other apps of the ecosystem.
- Light and dark theme; the feedback's colours are fixed and the app's own.

## 13. Identity and legal aspects

Note: these are informed assessments, not legal advice.

- **Game rules**: mechanics are not protected by copyright; names, code, texts and specific graphics are.
- **"Wordle" is a trademark of the New York Times.** In 2024 the NYT sent many takedown requests to projects on GitHub, contesting the use of the name and of the look (the grid's arrangement and the green/grey/yellow scheme). The NYT stated it has no objection to similar word games that do not infringe its trademark and protected works.
- **Consequences for this project**:
  - that name is never used to present or describe the app: not in the interface, not in the README, not in the release notes, not in the code; this legal note and its link in the References are the only places in the repository where it appears;
  - no code, text or word list of that origin;
  - palette, shapes and style of its own, derived from the ecosystem's theme;
  - the remaining risk is low but not zero, because the repository is public on GitHub.
- **The code's licence**: GPL-3.0-or-later (the GPL version 3 or any later version). The app's name and logo are not covered by the licence.
- **The data's licences**: compatible with distribution together with GPLv3 code (CC BY-SA 4.0 is declared one-way compatible with the GPLv3).
- **Dependencies**: no proprietary library (no advertising, analytics or Google Play services).

## 14. Technology and distribution

- **Stack**: Kotlin, Jetpack Compose, Material 3, aligned with the sister apps when the repository is created. The original text asked for Material 3 Expressive; its APIs are not in a stable release yet, so the app uses stable Material 3, as the sisters do («The owner's answers», 3).
- **The game engine** in pure Kotlin, apart from the interface and covered by tests.
- **Data** included in the app in a compact format; no download.
- **CI on GitHub**: build and signing with the app's own key through secrets, following the sister apps' procedure.
- **Distribution**: releases on GitHub (compatible with Obtainium). The Play Store is foreseen in the plan but not scheduled; F-Droid is an option.
- **Development** assisted by Claude Code, starting from this document and from PLANNING.md.

## 15. Out of scope

- Multiplayer, online leaderboards, accounts, cloud sync.
- Advertising, in-app purchases, themes or unlockable content.
- Other games: the app does one thing.
- Languages beyond the three planned (the architecture must not prevent them, though).

## 16. Open questions

1. **The name's availability.** A web search found no app called "That's The Word"; there are, however, games with a close name ("What's The Word"). The stores and the trademark registers must be checked by hand before the first release.
2. **The modes' names** in the three interface languages.
3. **The exact licences** of the Spanish and English sources, and the version of the Wiktionary data's licence.
4. **The definitions' quality** in the three interface languages: to be judged on a sample before committing to them.
5. **The app's size** with three full vocabularies and definitions: to be measured with a prototype of the data.

## The owner's answers (12 Nov 2026)

The plan's review (PLANNING.md §15, 12 Nov 2026) asked seventeen questions, each with a proposal. The owner answered the first five; the others stay in PLANNING.md §15's Open list until answered.

1. **The name**: *That's The Word*, as this vision gives it (the owner's latest version, which replaced an earlier draft with another name). The repository stays `thats-the-word`; the package and applicationId are `com.callbackdev.thatstheword`.
2. **The documents' language**: English, as the family's ("segui la famiglia e fai tutto in inglese"). This translation is the result; the Italian original stays in the history.
3. **Material 3**: stable, as the sister apps ("usiamo Material 3 stabile come le app gemelle"); §14 says so.
4. **The mark**: still open ("per l'icona vediamo"). The owner's first idea: letter blocks that compose a word. To be drawn on a board with the family's marks, as the sisters' were.
5. **The feedback's colours and shapes**: the review's proposal is accepted (colour and shape together: square for the right place in the app's colour, round for elsewhere in the family's amber, the absent tile sunk and quiet; a reveal and share symbols of the game's own), with one condition: every solution proposed must be a premium app's ("L'importante è che mi proponi soluzioni da premium app"). Drawn on a board in Phase 3, before it is built.

## References

- The NYT's takedown requests (AP, 2024): https://wjla.com/news/offbeat/nyt-fighting-off-wordle-copycats-with-several-takedown-notices-word-guessing-game-new-york-times-look-alikes-digital-millenium-copycat-act-github-trademark-battle
- CC BY-SA 4.0 → GPLv3 compatibility: https://creativecommons.org/2015/10/08/cc-by-sa-4-0-now-one-way-compatible-with-gplv3/
- LibreOffice's Italian dictionary: https://github.com/LibreOffice/dictionaries/tree/master/it_IT
- wordfreq: https://github.com/rspeer/wordfreq
- Frequency lists (Wiktionary): https://en.wiktionary.org/wiki/Wiktionary:Frequency_lists
- Extracted Wiktionary data: https://kaikki.org/dictionary/rawdata.html
