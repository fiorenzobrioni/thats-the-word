# ADR 0001: Foundations (Phase 0)

- Status: accepted
- Date: 2026-11-12

## Context

That's The Word starts from a repository holding only the owner's approved vision (`VISION.md`,
in Italian, `cac9c5c`, an earlier draft under another name, replaced by the owner's latest the same
day, `0d7cacf`, and translated into English with the owner's answers), and the owner's request for the family's structure: the same
stack at its latest stable versions, the same repository layout, workflows and releases, the same
design, the same licence, so the six apps (Chiaro, Passo, Tempo, Saldo, Tetto, That's The Word) are
built and maintained one way. Tetto's build is the most recent of the family (Tempo's and Passo's,
with Room); a game keeps tables (its games, its notebook), so its build is Tetto's.

## Decisions

1. **Identity.** Developer namespace `callbackdev`, as the family: package and applicationId
   `com.callbackdev.thatstheword` (debug: `com.callbackdev.thatstheword.debug`), modules under
   `com.callbackdev.thatstheword.*`, the convention plugins `thatstheword.*`, the Gradle property
   `thatstheword.versionName`, the release properties `THATSTHEWORD_*`. Author Fiorenzo Brioni,
   licence **GPL-3.0-or-later** (VISION §3; the sisters say GPL-3.0), the full text in `LICENSE`;
   the name and the mark outside it (VISION §13). The repository is `fiorenzobrioni/thats-the-word`,
   the app's name as VISION §2 gives it. The applicationId is final from the first release. The
   scaffold was first written under the earlier draft's name and renamed the same day, its debug
   key made again under the new name before any build was installed anywhere.
2. **The family's build, carried over** from Tetto (`tetto@ffc1322`), at the latest stable
   versions of 12 Nov 2026, as the owner asked: Gradle 9.9.1, AGP 9.5.1 with its built-in Kotlin,
   Kotlin 2.4.21, KSP 2.3.13, Compose BOM 2026.09.00 (material3 1.4.0), Hilt 2.60.1, Room 2.8.5,
   Navigation 3 1.2.1, DataStore 1.3.0, kotlinx.serialization 1.13.0, coroutines 1.11.0, Spotless
   8.11.0 with ktlint 1.8.0; JUnit 4, Truth, Turbine, Robolectric 4.17 (its tests on SDK 35, as the
   sisters'). The sisters are on Gradle 9.8 and AGP 9.4.1: the family is one build a step apart,
   which the sisters close when they choose. No Glance and no WorkManager: the game has no widget
   (PLANNING §8).
3. **The SDK.** `minSdk 34`, the family's (the per-app language picker, themed icons and
   predictive back are native), target and compile SDK 37, Java 21 without a toolchain. AGP 9.5
   builds with build-tools 36.0.0 (so does AGP 9.4, the sisters'); the cloud environment's setup
   script installs them since 12 Nov 2026, beside 37.0.0.
4. **Material 3, stable.** The vision names Material 3 Expressive (VISION §14); its APIs are
   internal in material3 1.4.0 and public only in 1.5.0, a release candidate. The family's rule is
   the latest stable, so 1.4.0, with the game's own springs and shapes, as the sisters (owner,
   12 Nov 2026: "usiamo Material 3 stabile come le app gemelle").
5. **The permission gate, widened for a game.** The family's `checkForbiddenPermissions` on every
   variant's merged manifest, with Tetto's list (network, location, exact alarms, battery
   exemptions, camera, microphone, SMS, contacts, calendar, storage, installed apps, sensors) and
   what a game might be tempted by: the advertising id (Google's and the platform's ad services),
   accounts, the phone's state, sending SMS (PLANNING §10). "No ads, no tracking" is then something
   the build checks. Checked on 12 Nov 2026: `INTERNET` added to the app's manifest fails both
   variants' checks.
6. **A module joins the build with its first code.** Phase 0 has `:app`, `:core:model` (the
   appearance's enums, which the theme reads), `:core:designsystem` and `:core:testing`;
   `:core:domain` and `:core:words` join in Phase 2, `:core:data` and the features from Phase 3.
   Until the start page exists as a feature, it is drawn by the shell in `:app`.
7. **Signing, as the family.** The game's own debug keystore, committed for good
   (`thatstheword-debug` / `android`, PKCS12, RSA 2048, created 12 Nov 2026, SHA-256 in
   `keystore/README.md`). The release key is the owner's to make; `release.yml` checks the APK's
   signature against its fingerprint and refuses to publish while none is pinned.
8. **Versioning from the start.** `thatstheword.versionName` in `gradle.properties` (0.1.0 for the
   foundations), `versionCode = major * 10000 + minor * 100 + patch`; the release workflow refuses
   a tag that does not match.
9. **Three interface languages ship** (`localeFilters` en, it, es), where the family ships two.
10. **Backup is an allowlist from the first build** (`data_extraction_rules.xml`): the database and
    the settings file, named before they exist; the words, which ship in the APK, never.
11. **`android:appCategory="game"`**: the system's game features (game mode, the launcher's
    grouping) see it as one.
12. **Phase 0's screen is the start page**: VISION §2's reference texts in the three languages,
    a word on tiles arriving one by one. No button is drawn before it starts a game.

## Consequences

- AGP 9's DSL, as Tetto's, Tempo's and Passo's: snippets from an AGP 8 build need translating.
- A change in the sisters' build-logic is not inherited: it is copied here by hand when it
  matters, as they copy each other's.
- A rename of the app, if the owner chooses one, is a pass over the package, the plugin ids and
  the properties before 1.0; after 1.0 the applicationId stays whatever the app is called.
