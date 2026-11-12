# Third-party licenses

That's The Word's code is GPL-3.0-or-later (`LICENSE` at the repo root). The app's name and its mark
are not covered by that licence (VISION §13). What ships inside the APK and is not ours:

| What | License | Where |
|---|---|---|
| [Google Sans](https://fonts.google.com/specimen/Google+Sans), the app's default typeface, bundled as `core/designsystem/src/main/res/font/google_sans_variable.ttf`: the upstream `ofl/googlesans` variable font cut down to the scripts the family's apps print, the same file as theirs. Its Bold outlines also draw the letters of the launcher mark (`tools/draw_launcher_icon.py`) | SIL Open Font License 1.1 | `GoogleSans-OFL.txt` |
| [Inter](https://github.com/rsms/inter), the second typeface, bundled as `core/designsystem/src/main/res/font/inter_variable.ttf` | SIL Open Font License 1.1 | `Inter-OFL.txt` |

**The words.** The word lists, their frequencies, definitions and translations are not here yet:
they are generated from free sources by a script in this repository (Phase 1, PLANNING.md §6),
and each source joins this table with its licence, its version and the file it reaches, the day
its data first ships. The candidates and their licences are in VISION §10 and PLANNING.md §6.

Everything here is credited in Settings, About (a later phase), because a licence that asks for
credit is not satisfied by a file the player never opens.
