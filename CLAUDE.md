# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in
this repository.

## What this repository is

**That's The Word** (what one says when the word on the tip of the tongue finally comes; the
line the game greets every word found with) is a private, offline Android word game (Kotlin /
Jetpack Compose Material 3): a hidden word to find in a few attempts, in Italian, English or
Spanish, as a daily pastime or to practise a language being learnt. The interface speaks the same
three languages; the language a game is played in is chosen apart, and when the two differ the
game turns into practice: hints, meanings in the player's own language, a notebook of the words
met. No account, no ads, no tracking, no internet permission at all. It belongs to a small family
of single-purpose apps by callbackdev, with **Chiaro** (weather), **Passo** (step counter),
**Tempo** (clock and agenda), **Tetto** (budget) and **Saldo** (finance): same developer identity,
same build and release setup, and the same visual language, from which a game departs only where
it must (more motion, tiles, sound). Its build and structure are Tetto's (itself Tempo's and
Passo's).

**State (12 Nov 2026): Phase 0 is done**, Phase 1 (the words) is next. The vision is approved and
reviewed; the build, the design system, CI, the keys and the mark are in place; the modules in the build are `:app`, `:core:model`,
`:core:designsystem` and `:core:testing`. The owner answered the first questions of the review on
12 Nov 2026 (VISION, «The owner's answers»); **twelve smaller questions are still open**
(PLANNING §15, Open): read them before building anything they touch.

Source of truth:

- `VISION.md`: the product, as the owner approved it on 12 Nov 2026, in English with «The owner's
  answers» (the owner's Italian original is in the history, `0d7cacf`). Principles, the modes of 1.0, 1.1 and 1.2, practising a language, the
  words and their sources, progression, the premium experience, identity and the legal notes,
  what is out of scope, its own open questions.
- `PLANNING.md`: the architecture, the game engine (§4), the data model (§5), the words (§6), the
  look and feel of the game (§7), notifications (§8), battery rules (§9), permissions (§10), and
  the phased plan with checkable steps (§11). **Keep it updated as work progresses**: tick the
  boxes, and record every decision and deviation with its reason in §15 (and as an ADR in
  `docs/adr/` when it is architectural). It is kept as the family's:
  - **Write only in `PLANNING.md`**: a new `###` section at the end of §15's log, dated, with
    who asked. A box ticked or opened goes in §11 (a phase's step) or in §15's Open list.
    §1-§10 and §12-§14 are the living specification: they change in place, with the code.
  - **`docs/planning/NN-<period>.md` is the archive**, the closed periods of the log with the
    phases they closed, **frozen**: never edit it. A decision that changes an archived one is
    written in `PLANNING.md`, naming the archived file and the section or entry it changes.
  - **To find a section** cited by name or date in the code ("PLANNING.md §11 Phase 3",
    "§15, 12 Nov 2026"), grep `PLANNING.md` first, then `docs/planning/README.md`, the index of
    every archived heading and dated entry with its file and line, and read from that line.
    Never load an archive file whole.
  - **Archive** when the log passes ~80 KB or at a release, whichever first, as `PLANNING.md`
    "How it is made" says, then run `python3 tools/planning_index.py` (`--check` tells if the
    index is behind).

Work one phase at a time, starting from the phase's section in PLANNING.md §11. A new question is
written in PLANNING §15's Open list and asked before it is built. Never invent an answer the owner
has not given.

## Build and commands

Stack: Kotlin 2.4 (compiled by AGP 9's built-in Kotlin), Compose Material 3, Hilt, Room (KSP, from
Phase 3), DataStore, kotlinx.serialization, Navigation 3; Gradle 9.9 / AGP 9.5, version catalog
in `gradle/libs.versions.toml`, convention plugins in `build-logic/`. Package/applicationId:
`com.callbackdev.thatstheword`. minSdk 34, compile/targetSdk 37 (the
family's). Java 21.

- Build debug APK: `./gradlew :app:assembleDebug` (output: `app/build/outputs/apk/debug/app-debug.apk`)
- All unit tests: `./gradlew test`
- One module: `./gradlew :core:designsystem:test`; one class: `--tests "com.callbackdev.thatstheword.core.designsystem.ContrastTest"`
- Lint (every module, via `checkDependencies`): `./gradlew :app:lintDebug`
- Format: `./gradlew spotlessApply` (CI runs `spotlessCheck`; rules in `.editorconfig`)
- Forbidden-permission check on the merged manifests: `./gradlew :app:checkForbiddenPermissions`
- Installable minified build: `./gradlew :app:assembleRelease -PsignReleaseWithDebugKey`
- Launcher icon: `python3 tools/draw_launcher_icon.py`
- The words (from Phase 1): `tools/words/`, its own README; never edit its outputs by hand.
- On a machine with no system JDK, prepend `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
- **Maven Central answers HTTP 429** (Too Many Requests; it happens in the Claude Code cloud
  sandbox): add `--init-script gradle/google-maven-mirror.init.gradle.kts` to any command. It
  puts Google's mirror of Maven Central first, and points Robolectric's own `android-all`
  download at it, for that run only; nothing in the build changes. In the sandbox the SDK is at
  `/opt/android-sdk` (`local.properties`, not committed). AGP 9.5 builds with the SDK's
  build-tools 36.0.0 and downloads them at the first build if the environment lacks them.

**Convention plugins** (`build-logic/convention`): `thatstheword.android.application`,
`thatstheword.android.library`, `thatstheword.android.compose`, `thatstheword.android.feature`,
`thatstheword.android.hilt`, `thatstheword.android.room`, `thatstheword.jvm.library`. SDK levels and the
Java version live in `ThatsTheWordSdk` / `ThatsTheWord.kt`; a module's own build file only says what is
specific to it. The forbidden-permission list is `ForbiddenPermissionPatterns` in
`ForbiddenPermissions.kt`.

**Version**: `thatstheword.versionName` in `gradle.properties` is the only place it is written;
`versionCode` is derived (major * 10000 + minor * 100 + patch). `release.yml` refuses a tag
that does not match it.

## Modules (PLANNING.md §2)

| Module | Kind | Holds |
|---|---|---|
| `:core:model` | pure Kotlin/JVM | data classes shared by everything: the appearance's enums (Phase 0); `Language`, `Letter`, `Word`, `Mark`, `Guess`, `GameSetup`, `GameState`, `Level`, `HintKind`, `NotebookEntry`, `Milestone`, `UserSettings` (Phase 2) |
| `:core:domain` | pure Kotlin/JVM (Phase 2) | the game engine: alphabets and folding, the feedback, `Game` (guesses, refusals, Hard, hints, the end), the daily's number and word, free play's draw, streaks, statistics, milestones, the share text, the words' decoder, `BackupCodec` |
| `:core:words` | Android library (Phase 2) | the word files in assets, written by `tools/words/`, mapped and handed to the decoder |
| `:core:data` | Android library (Phase 3) | Room (`ThatsTheWordDatabase`: games, guesses, hints, notebook, milestones; schema exported to `core/data/schemas/`), DataStore (`settings`), the repositories |
| `:core:designsystem` | Android library | M3 theme (Chiaro's dresses through the sisters), typography with the game's roles (`wordmark`, `eyebrow`, `tileLetter`, `heroNumber`), shapes, motion (the family's springs and the game's `playful` one), page gutter (`PageWidth.kt`), `LetterTile`; later the feedback colours, the keyboard's keys, sounds and haptics |
| `:core:testing` | Android library, test-only | `assertAccessible()` (labels, 48dp touch targets) and `walkPage()`, shared by the UI tests as `testImplementation` |
| `:feature:*` | Android library (from Phase 3) | `game`, `home`, `notebook`, `progress`, `onboarding`, `settings` |
| `:app` | application | `Application`, `MainActivity`, the shell (the start page in Phase 0; Navigation 3 from Phase 3), DI entry points; wires everything |

Rules: `:core:model` and `:core:domain` stay pure Kotlin/JVM; if a class there needs a `Context`
or a `Resources`, it is in the wrong module. Feature modules depend on `core:*`, never on each
other. All game logic lives in `:core:domain`, with unit tests. A module joins the build in the
phase that brings its first code.

## Invariants (non-negotiable)

- **Never add a forbidden permission** (PLANNING.md §10): `INTERNET`, any `ACCESS_*LOCATION`,
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, the
  advertising id (`com.google.android.gms.permission.AD_ID`, `ACCESS_ADSERVICES_*`), `CAMERA`,
  `RECORD_AUDIO`, the SMS permissions, `READ_CONTACTS`, `WRITE_CONTACTS`, `GET_ACCOUNTS`,
  `READ_CALENDAR`, `WRITE_CALENDAR`, `READ_PHONE_STATE`, the storage permissions
  (`READ_`/`WRITE_`/`MANAGE_EXTERNAL_STORAGE`, `READ_MEDIA_*`), `QUERY_ALL_PACKAGES`,
  `ACTIVITY_RECOGNITION`, `HIGH_SAMPLING_RATE_SENSORS`, `BODY_SENSORS*`. The build fails on the
  merged manifest; a library that adds one is removed or patched with `tools:node="remove"`.
  Files go through the Storage Access Framework, sharing through the share sheet.
- **No ads, no analytics, no accounts, no Play Services, no proprietary library** (VISION §3,
  §13), and nothing that phones home.
- **The genre's trademark** (VISION §13): the name of the newspaper's game that made the genre
  famous is written **nowhere** in this repository but VISION §13 and its reference: not in the
  app, the README, the release notes, the code, the comments, the tests, this file or PLANNING.
  No code, text, word list or drawing of that origin; the game's palette, shapes, reveal and
  share symbols are its own (PLANNING §7). Say "the genre" when one must be named.
- **The feedback is said by colour and shape together**, never by colour alone (VISION §6): in
  the grid, on the keys, in the share text, to TalkBack.
- **The engine is the domain's, and only the domain's** (PLANNING §4): the folding, the marks
  (two passes, duplicates counted), Hard's rule, the hints' cost, the daily's number. Don't change
  it without its tests for every edge case in PLANNING.md §4.9. Marks are computed from the
  solution and the guesses, never stored.
- **Dates are local dates** from an injected clock and zone; a daily belongs to the date it was
  started on; its word is the frozen schedule's at `date − epoch + 1`.
- **The words are generated, never hand-edited** (PLANNING §6): `tools/words/` writes every file
  from pinned, free, credited sources, the same bytes on a second run. **The daily schedule is
  append-only**: no entry already published is ever changed. Every source's licence is
  GPL-3.0-compatible and recorded in `licenses/README.md` and in the app's About.
- **The database is a contract.** The Room schema is exported and committed; every migration has
  a test (`MigrationTestHelper`) and the upgrade from the first release's schema is tested.
  Never `fallbackToDestructiveMigration`.
- **Battery first** (PLANNING.md §9): no service of any kind, no polling, no wakelocks, no exact
  alarms, no periodic workers. Nothing runs while the app is off screen; the change of day is seen
  by a ticker in a visible page's lifecycle. Sounds are loaded only while the game is on screen.
- **Backup is an allowlist** (`app/src/main/res/xml/data_extraction_rules.xml`): the database and
  the settings file, nothing else. Android sends the copy, so the no-`INTERNET` rule stands; say
  "That's The Word sends nothing", never "nothing leaves the phone". A restore from a file is read
  whole and checked before anything is written, and replaces only after the player's confirmation.
- **Every user-facing string is a resource, in English, Italian and Spanish** (`values/`,
  `values-it/`, `values-es/`), with plurals where counts appear, each written in its language with
  the same rhythm, never translated word for word (VISION §2). The name stays *That's The Word* in
  every language (`translatable="false"`). The interface language follows the system per-app
  language picker (`locales_config.xml`); the game language is the player's choice in the app.
  Dates are formatted with the locale's own patterns, never a hard-coded order.
- **The voice** (VISION §2): short, calm, a little theatrical. Short sentences, no exclamation
  marks, no gamer slang. Capitals are drawn (`uppercase(locale)`), never written in a string, so
  TalkBack reads words.
- **No new third-party dependency without approval**, and its license must be
  GPL-3.0-compatible (Apache-2.0, MIT, BSD).

## Code style

Kotlin official style (ktlint `intellij_idea` via Spotless), no `!!`, `Flow` over `LiveData`,
immutable UI state, MVVM with unidirectional data flow. Code and documentation in English.
Comments explain *why*, not what. In the code a guess is a `Guess`, a letter's verdict a `Mark`
(right place, elsewhere, absent), the word to find the `solution`, a word as typed its key form
and as written its display form (PLANNING §4.1).

## Design

The game keeps the visual language of Chiaro, Passo, Tempo and Tetto, for its pages, so the family
reads as one (`docs/adr/0002-design-language.md`), and departs from it where a game must (PLANNING
§7.1): capitals with wide tracking for its name and its short lines, tiles, a keyboard, livelier
springs, sound. `:core:designsystem` holds it as roles (color, type, shape), never as hexes inside
a composable: Chiaro's two generated dresses (`theme/Scheme.kt`, copied through the sisters, never
hand-edited), Google Sans / Inter / system type, the family's shapes and springs, and every
animation collapses to a fade under reduced motion (the system's setting, and the in-app switch
when it comes). **The feedback's colours are the game's own** and follow neither the dress nor the
wallpaper; each letter ink on them is pinned at 4.5:1 by `ContrastTest`. The principles hold: one
sentence before any number, every number with the line that says what it means, no dead tab and
no switch for a feature that has not shipped, no button drawn before it does something. A short
text that stands on its own takes `balanced()`: no line ends on one word alone, in any of the
three languages.

The launcher mark (four letter tiles spelling WO / RD, the last one, the D, amber and turned as if
just set down, on the app's berry: the owner's idea and choice, 12 Nov 2026) is written by
`tools/draw_launcher_icon.py`, in the family's series of 9 Nov 2026 (the app's colour as the
ground, a white symbol with one amber accent, within the 28-unit keyline): change the script and
re-run it, never the XML. Its letters are Google Sans Bold's outlines, written into the script. The
app shows its mark nowhere inside itself.

**Motion is a feature here** (VISION §12): every key moment has its movement (a letter set, a row
revealed, a word refused, a word found, a milestone), on `ThatsTheWordMotion`'s springs, the row's
tiles staggered; never the genre's flip; no animation ever gates information.

**Accessibility and foldables** (the family's, from the start): a screen's tests call
`assertAccessible()` on what they draw, and walk the page at twice the text size and on an open
foldable (`walkPage()`); text inks are pinned at 4.5:1 by `ContrastTest`. A page leaves
`pageGutter()` on each side (in a list's content padding, never by narrowing the list), so an open
foldable shows a centred column. A tile's letter is sized by its tile, not by the text size. TalkBack
reads a row as one sentence, a key with its mark.

The Compose UI tests write screenshots to each module's `build/screenshots`: look at them after
changing a screen, **every one the change draws, before committing**, in the three languages where
the words differ in length.

**README screenshots** (`docs/screenshots/`, shown in the root `README.md`, from Phase 8): drawn by
`ReadmeScreenshots` test classes from realistic sample games, in English, and only on request:
`./gradlew test -PupdateScreenshots` (plus the mirror init script in the sandbox). The family's
standing rules: **regenerate them** whenever a change alters what an existing one shows, and look
at them before committing; **add one** when a phase brings something worth showing, with its
caption in the README's table; keep the set small.

## The family in the code

The sister apps (Chiaro, Passo, Tempo, Tetto, Saldo) are named **only** in the root `README.md`
(«The family») and in the project's documents (VISION, PLANNING, the ADRs, this file): never in a
string the app shows, and never in the code, comments and tests included (the family's rule). A
comment says why a value is what it is, not which sister has it too; where a value's provenance
matters (the generated schemes), it is written in its ADR.

## Signing and CI

- **Debug signing**: `keystore/debug.keystore` is intentionally committed (alias
  `thatstheword-debug`, passwords `android`; the game's own, made in Phase 0, not a copy of a
  sister's) so debug APKs from CI and any machine share one signature. Do not regenerate it.
  Debug builds carry `applicationIdSuffix ".debug"`. Debug builds and the debug-signed release
  carry a crash page (`app/src/crashpage`): an uncaught exception shows its stack trace, to copy or
  share, so a test device can report a crash without adb. Never in a release signed with the
  release key.
- **Release signing**: the real keystore lives OUTSIDE the repo; the `release` signingConfig is
  created only when the four `THATSTHEWORD_KEYSTORE*` / `THATSTHEWORD_KEY_*` properties are all set
  (from `~/.gradle/gradle.properties` locally, from `ORG_GRADLE_PROJECT_*` env vars in CI). Without
  them the release build is unsigned; `-PsignReleaseWithDebugKey` signs it with the debug key for
  testing only. **The release key** was made by the owner for this app alone and put in the four
  secrets on 12 Nov 2026 (`CN=callbackdev`). Its SHA-256 fingerprint (PLANNING §15, 12 Nov 2026) is
  `14:D4:1B:20:DC:B0:B1:FC:DB:22:42:DE:0B:0C:3D:09:B5:55:07:7E:D5:4D:9B:34:13:F0:00:07:A3:D1:BE:4B`:
  published in `keystore/README.md` and the root README, and pinned in `release.yml`, which refuses
  an APK signed by any other key. Never regenerate it: a future Google Play listing reuses it, and
  a new key would mean no player could update without uninstalling.
- **Release key in CI**: the four repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD` (the same names as the sister apps'); `release.yml` decodes the
  keystore into the runner's temp folder and fails by name if one is missing. Never commit the
  real key.
- **CI** (`.github/workflows/android-ci.yml`, every push and PR): formatting, forbidden
  permissions, unit tests and lint run *before* the APKs; a red suite must never produce an
  installable artifact. **Release** (`release.yml`, on `v*` tags): the same gates, then the signed
  APK, its SHA-256 and the R8 mapping on a GitHub Release, with the body taken from the tag's
  `CHANGELOG.md` section (write it before tagging).

## Writing `README.md` (root file only)

**No em dashes (`—`) or en dashes (`–`) in the root `README.md`.** Rewrite the sentence rather
than swapping in a hyphen: use a colon when the clause explains, a full stop when the thoughts
are separate, parentheses for an aside. Same house style as the family, deliberately scoped to
that one file: every other file keeps normal punctuation.
