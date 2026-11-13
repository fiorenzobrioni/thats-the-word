<div align="center">

# 🔤 That's The Word

**One word a day. Three languages. Played on your phone, sent nowhere.**

Find the hidden word in six attempts, in Italian, English or Spanish: as a calm daily pastime,
or to practise a language you are learning, with its meaning in your own language when the game
ends. Free and open source, no account, no ads, no tracking, and no permission to use the
internet at all.

![Platform](https://img.shields.io/badge/platform-Android-7E1D68?labelColor=FCFAF6)
![Release](https://img.shields.io/github/v/release/fiorenzobrioni/thats-the-word?label=release&labelColor=FCFAF6&color=7E1D68)
![CI](https://img.shields.io/github/actions/workflow/status/fiorenzobrioni/thats-the-word/android-ci.yml?branch=main&label=CI&labelColor=FCFAF6&color=7E1D68)
![License](https://img.shields.io/badge/license-GPL--3.0--or--later-007DB6?labelColor=FCFAF6)
![minSdk](https://img.shields.io/badge/minSdk-34-70569C?labelColor=FCFAF6)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-F1A000?labelColor=FCFAF6)
![Compose](https://img.shields.io/badge/UI-Compose%20Material%203-007DB6?labelColor=FCFAF6)
![Internet](https://img.shields.io/badge/INTERNET%20permission-none-7E1D68?labelColor=FCFAF6)

</div>

> **Status: in the making.** The foundations are laid (Phase 0), the words are made (Phase 1),
> the game's rules are written and tested (Phase 2); the game, the start page, the practice of
> a language (hints, meanings, the notebook), your progress, your data and sharing are built
> (Phases 3 to 6) and wait for a look on a phone. There is nothing to install yet: see [Roadmap](#roadmap).

## What That's The Word is

You know the feeling: the word is right there, on the tip of your tongue, and then it comes.
"That's the word." The name is that moment, the best one in the game, and the line the game
greets every word found with: *Ecco la parola* in Italian, *Esa es la palabra* in Spanish.

Every day there is a word to find, the same for everyone, one for each language. Each guess must
be a real word; each letter then says whether it is in the right place, somewhere else in the
word, or not in it at all, by its colour and by its shape, never by colour alone. Six attempts,
one word.

What sets the game apart is the other language. Choose to play in a language that is not your
phone's, and the game turns into practice: a few hints, each paid with an attempt, the meaning
and the translation of every word in your own language when the game ends, and a notebook that
keeps the words you met. It is a vocabulary exercise, not a language course, and it says so.

## Features

What 1.0 will do ([VISION.md](./VISION.md) has the full scope):

- 📅 **The daily word**, the same for every player and one for each language, from the date
  alone: no server, nothing to download. It changes at your midnight.
- 🗓️ **The archive**: a calendar of every daily from the first one to today, each marked solved,
  missed or not played; play the ones you missed.
- ♾️ **Free play**, as many games as you like, at the level you choose: Base, Intermediate,
  Advanced or Complete.
- 🧠 **The Hard option**: every clue you found must be used in the next guesses.
- 🌍 **Three languages to play in, three to read in**: Italian, English and Spanish, chosen apart.
  Italian accents and Spanish accents are ignored as you type and shown when the game ends;
  the Spanish ñ is a letter of its own, with its own key.
- 🎓 **Practice in a foreign language**: a translation, a definition or a letter as hints; the
  meaning, the translation and the written form of every word at the end; games with hints
  still keep your streak, and are counted apart.
- 📒 **The notebook**: the words you met, with their translation, language and date, searchable
  like a small dictionary of your own.
- 📈 **Progress**: statistics per language and mode, a streak per language, and milestones that
  recognise steadiness and learning. Nothing to unlock: recognition, never a key.
- 📤 **Share a result** without giving the word away, in the game's own look.
- 💾 **Your data**: a backup file of your games, notebook and milestones, to keep or to move.
- ✨ **Made to feel good**: every key moment has its movement, discreet haptics and sounds, each
  with its own switch; light and dark, the family's dresses and typefaces.
- 🤝 **For every player**: shapes as well as colours, TalkBack throughout, text at twice its size,
  less motion on request, a centred column on an open foldable.

## Principles

| | |
|---|---|
| 🔒 **Offline and private** | no `INTERNET` permission: That's The Word sends nothing. The build fails if a network, location, advertising id, camera, microphone, contacts, accounts, storage or installed-apps permission appears |
| 🎯 **Few things, done very well** | one mode polished rather than three sketched |
| 🌍 **The other language is the heart** | playing in a language not your own without frustration, and leaving with something learnt |
| 📖 **Open** | the code under the GPL, the words from free and documented sources, rebuilt by a script in this repository, the APK built by CI |
| 🎨 **Its own identity** | its name, look, colours and word lists are its own, not taken from any existing game |
| 🔋 **Battery is a feature** | no service, no polling, nothing running while the game is not on screen |

## Install

Android 14 (API 34) or newer. There is no release yet: the first comes with 1.0 (see
[Roadmap](#roadmap)).

When it comes:

1. Download `thats-the-word-vX.Y.Z.apk` from the
   [latest release](https://github.com/fiorenzobrioni/thats-the-word/releases/latest).
2. Open it on the phone and allow installs from that source when Android asks.

**Verify the download.** Put the APK and its `.sha256` file in one folder and run
`sha256sum -c thats-the-word-vX.Y.Z.apk.sha256`. To check that the APK is genuine, compare its
signing certificate (`apksigner verify --print-certs`, or AppVerifier on the phone) with this
SHA-256 fingerprint:

```
14:D4:1B:20:DC:B0:B1:FC:DB:22:42:DE:0B:0C:3D:09:B5:55:07:7E:D5:4D:9B:34:13:F0:00:07:A3:D1:BE:4B
```

**Updates.** That's The Word has no network access, so it cannot check for updates. Use GitHub's
"Watch, Custom, Releases" notifications, or [Obtainium](https://github.com/ImranR98/Obtainium).
Every release will install over the previous one and keep your games and your notebook.

## Roadmap

The phased plan, with every decision and its reason, is in [PLANNING.md](./PLANNING.md) (the
closed periods in [docs/planning](./docs/planning/README.md)):

- **Phase 0**, foundations: the build, the design system, CI, the keys, the mark. *Done.*
- **Phase 1**, the words: three languages, their levels, meanings and translations, from free
  sources, measured before anything is drawn. *Done ([the report](./docs/words/README.md)).*
- **Phase 2**, the game engine: every rule, every edge case tested. *Done.*
- **Phase 3**, the game screen: the grid, the keyboard, the motion. *Built; to be looked at on a
  phone.*
- **Phase 4**, the start page, the daily word, the archive and free play. *Built; to be looked
  at on a phone.*
- **Phase 5**, practising a language: hints, meanings and the notebook. *Built; to be looked at
  on a phone.*
- **Phase 6**, progress, milestones, sharing and your data. *Built; to be looked at on a
  phone.*
- **Phase 7**, the first run (a guided first game), settings and sound.
- **Phase 8**, the accessibility pass and polish.
- **Phase 9**, the 1.0 release on GitHub.

After 1.0: **Translate**, **Bridge** (one idea in three languages, three grids) and **Review**
(1.1); then the variants (1.2 and later).

## Build

Requires JDK 21 (the one bundled with Android Studio works) and the Android SDK.

```bash
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew test                      # unit tests, every module
./gradlew :app:lintDebug            # lint, every module
./gradlew spotlessApply             # format the code
./gradlew :app:checkForbiddenPermissions
```

The words are rebuilt from their pinned sources by `tools/words/` (Python, nothing to install;
[its README](./tools/words/README.md)):

```bash
python3 tools/words/build.py           # fetch, build and write every word file and the report
python3 tools/words/build.py --check   # a second run must write the same bytes
```

For an installable minified build to test with:
`./gradlew :app:assembleRelease -PsignReleaseWithDebugKey`. It is signed with the debug key
committed in `keystore/`, on purpose, so builds from CI and any machine share one signature.
Debug builds carry `applicationIdSuffix ".debug"` and install side by side with the release.

CI runs formatting, the permission check, the tests and lint **before** building the APKs,
so a red suite never produces an installable artifact. A `vX.Y.Z` tag runs the same gates,
then publishes the signed APK, its checksum and the R8 mapping, with the matching
[CHANGELOG.md](./CHANGELOG.md) section as the release notes.

## Tech stack

- **Kotlin** 2.4, **Jetpack Compose** with Material 3, Gradle 9.9 and AGP 9.5, minSdk **34**
  (Android 14), target and compile SDK **37**
- **Room** (games, the notebook, milestones), **DataStore** (settings), **Hilt**, **Coroutines**
  and **Flow**, **Navigation 3**
- The game engine in pure Kotlin, apart from the interface, with a test for every rule
- The words built by a reproducible Python script from free sources, shipped as compact files
  read in place
- Compose UI tests with Robolectric, accessibility checks included

```text
Compose UI → ViewModel → :core:data (Room, DataStore)
                      ↘ :core:domain (pure Kotlin: the rules, the daily word, streaks)
                      ↘ :core:words (the word lists, built by tools/words/)
```

## Project structure

```text
thats-the-word/
├── app/                    # Application, MainActivity, the shell, DI
├── core/
│   ├── model/              # pure Kotlin data classes
│   ├── domain/             # pure Kotlin: the game engine, the daily word, streaks, statistics
│   ├── words/              # the word files and their reader
│   ├── data/               # Room, DataStore, repositories
│   ├── designsystem/       # theme, tiles, keyboard, motion, sounds
│   └── testing/            # shared UI test helpers (accessibility, page walks)
├── feature/                # game, home, notebook, progress, onboarding, settings
├── build-logic/            # convention plugins, the forbidden-permission check
├── tools/                  # the words' script, the launcher icon, the plan's index
└── keystore/               # the shared debug key (deliberately committed)
```

Each module joins the build with the phase that brings its first code. `:core:model` and
`:core:domain` are pure Kotlin: a class in them that needs a `Context` is in the wrong module.

## Project documentation

| File | Contents |
|---|---|
| [VISION.md](./VISION.md) | the product: principles, modes, practising a language, the words, identity, scope |
| [PLANNING.md](./PLANNING.md) | architecture, the game engine, the words, the look of the game, the phased plan and its decisions |
| [docs/planning](./docs/planning/README.md) | the closed periods of that plan's log, frozen, with an index of every section |
| [docs/adr/](./docs/adr/) | the architecture decision records |
| [docs/words](./docs/words/README.md) | the words: their sources, counts, levels, meanings in nine language pairs, sizes, and the samples a person reads |
| [CHANGELOG.md](./CHANGELOG.md) | what shipped, per version; a section is written before its tag |
| [CLAUDE.md](./CLAUDE.md) | the operating rules for AI-assisted development in this repo |

## The family

That's The Word is one of six focused apps with the same look and the same rules:
[Chiaro](https://github.com/fiorenzobrioni/chiaro) (weather),
[Passo](https://github.com/fiorenzobrioni/passo) (step counter),
[Tempo](https://github.com/fiorenzobrioni/tempo) (clock and agenda),
[Tetto](https://github.com/fiorenzobrioni/tetto) (budget) and
[Saldo](https://github.com/fiorenzobrioni/saldo) (personal finance). It is the family's first
game. Each stands alone.

## License

[GPL-3.0-or-later](./LICENSE) © 2026 Fiorenzo Brioni. The name *That's The Word* and its mark are
not covered by the licence.

[Google Sans](https://fonts.google.com/specimen/Google+Sans) and
[Inter](https://github.com/rsms/inter) under the SIL Open Font License 1.1. The word data will
come from free sources, each credited with its licence. Full attributions in
[licenses/](./licenses/).
