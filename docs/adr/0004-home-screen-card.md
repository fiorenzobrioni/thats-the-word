# ADR 0004: The home-screen card (Phase 7b)

- Status: accepted (the owner asked for the card in 1.0, PLANNING §15, Open 15)
- Date: 2026-11-13

## Context

The owner wants a home-screen card in 1.0 (PLANNING §15, Open 15): today's word in each language
and the streak, on the family's card. Every sister has its cards; their widget modules (Passo's
ADR 0005, from Chiaro's; Tempo's and Tetto's ADR 0003) are the family's working answer to the
card itself (its corner, insets, grounds, colours, opacity and ink rule), to the arithmetic of what
fits measured in the launcher's face, to the settings page with the real card, and to a refresh
that keeps the process awake for exactly the redraw it asked for. What the game adds is the day in
three languages, drawn as its own tiles.

## Decisions

1. **The family's card, carried over** (PLANNING §7.8). The launcher's own corner
   (`system_app_widget_background_radius`), the insets (14 dp; 6 dp top and bottom on a row; 8 dp
   on one cell), the four grounds (light, dark, as the phone, one of six colours), the opacity and
   the ink rule (`widgetInk`: the card is its own ground from half opacity, the wallpaper's hint
   below it), every ink resolved against the ground the card really has; the measuring in the
   launcher's face (`textEm`, a 6% margin); the guarded load that never leaves Glance's spinner on
   screen; `WidgetRefresh`'s revision, so a live session reloads; the receiver's cleanup that never
   finishes a pending result twice. The six colours are the family's, hex for hex, in the design
   system's `WidgetPalette.kt`: blue `#0F3B6B` (the default), azure `#0F5580`, green `#17572E`,
   teal `#0F5B5B`, plum `#4A2C63`, clay `#7A3320`, each a dark ground under white ink.

2. **One card, «Today's word»**. Not the family's pair: the game has one thing to say on a home
   screen, how today stands in each language. Its forms are pure Kotlin (`CardFit`, `:core:domain`),
   pinned by tests at the family's reference grants (one cell 85 dp; rows 85, 189, 293; two,
   three, four cells 159, 250, 340): tall, a row, one cell (PLANNING §7.8).

3. **The day drawn as the start page draws it**, in tiles without letters, painted as a bitmap
   at the size shown (`TilesPainter`): rounded corners in the game's proportion, a circle and an
   outline are the drawings boxes cannot build, and a see-through card has no ground of its own
   to fake them with. The feedback's colours stay the game's own on every ground. Because the
   right place's ink blue is as dark as the card's colours, a word found's tiles carry a light rim
   on a dark ground, 3:1 from each of the six colours (`ContrastTest`). A word under way leads with
   the elsewhere's amber circle, the calendar's glyph for a game begun, so waiting and under way
   differ by shape as well as by their words. Every row says its state in words too, and TalkBack
   reads it as one sentence.

4. **Pushed, never polled** (PLANNING §9). After every game that changes what the card shows
   (`WidgetUpdater`: the games' and the settings' flows, the day computed and compared, so a guess
   that leaves the day as it stood redraws nothing); at midnight, one `AlarmManager.set(RTC)`,
   inexact and non-wakeup, a second past the next local midnight, armed after every render while a
   card is placed and disarmed with the last card; on the exempt broadcasts (time set, zone,
   language, package replaced); when the interface's language changes under a running game. No
   service of the game's, no periodic work, no wake lock of its own; Glance's WorkManager brings
   its own, as in the sisters.

5. **One door into a language's word** (`TodayDoor`, `:core:data`): the launcher's shortcuts, the
   daily reminder and a row of the card open the game with the same extra, and each row's intent
   carries its own data, so three rows are three pending intents.

6. **The settings page** is the family's: the real card, composed by Glance into `RemoteViews` and
   inflated as a launcher does, at the size it has and the reference sizes; what it stands on and
   its opacity, saved as chosen; reset. Settings' «Home screen» row asks the launcher to place one
   (`HomeScreenCards`, bound by `:widget`), and says how to add it by hand where the launcher
   cannot be asked.

7. **Not backed up**: the looks live in their own file (`widgets`), outside the backup's
   allowlist: a restore renews the cards' ids, and a look kept under an old id would dress nothing,
   or the wrong card.

## Consequences

- The merged manifest holds the services Glance and WorkManager declare (as the sisters'), and
  WorkManager's `WAKE_LOCK`, `FOREGROUND_SERVICE`, `ACCESS_NETWORK_STATE` and
  `RECEIVE_BOOT_COMPLETED`; none is forbidden (PLANNING §10), and there is no `INTERNET`.
- What only a phone can say waits for the owner's (PLANNING §11, Phase 7b's acceptance): the card
  beside the sisters' on a real launcher, its pin dialog, a game redrawing it, midnight with the
  screen off, TalkBack on its rows, the picker's generated preview.
