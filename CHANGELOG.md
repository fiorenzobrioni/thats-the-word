# Changelog

All notable changes to That's The Word are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed (`## [1.0.0]` for
`v1.0.0`) and uses it as the body of the GitHub Release, so a version's entry is written
**before** its tag, and kept to what somebody arriving at that page wants to read.

## [Unreleased]

### Added

- The game's rules, all of them: the marks, Hard, the three hints and what they cost, the daily
  word of each date, free play's draw at your level, streaks, statistics and milestones, and the
  result to share without the word.
- The words of the three languages, read straight from the app as they ship, with every word
  written as it should be: *città*, *caffè*, *árbol*.
- The game itself: today's word in your game language, from the start page. A grid that says
  each letter's mark by colour and shape (an ink square where it belongs, an amber disc when it
  is elsewhere in the word, a sunken tile when it is not in it), a keyboard that keeps every
  letter's best mark, Ñ beside L in Spanish, and a hardware keyboard that types too.
- A word refused is said in one line above the keyboard, and the row shakes; a word found is
  greeted with «That’s the word.», then the card with the word as it is written, your words and
  the result to share.
- Every guess is saved as it is made: close the app in the middle of a game and it is there,
  where you left it.
- TalkBack reads each row as one sentence and each key with its mark; with animations off every
  movement is a short fade.
- The start page: today's word in each of the three languages, how each stands (waiting, under
  way, found in so many, missed), and one touch to play in another.
- Your streak in the game language, the days in a row with the word found on its own day.
- The calendar of the dailies: every day from the first word to today, found, missed, under way
  or not played, each drawn as the game draws a letter; touch a past day to play it from the
  archive. Days caught up later carry a dot, and count everywhere but in the streak.
- Free play, at the level you choose: Base, Intermediate, Advanced or Complete, each language its
  own. A word left halfway is waiting for you when you come back.
- Hard, from the start page, for the next game.
- When a day's word is over, play it again as practice: the day keeps its first result. When a
  free word is over, another one.
- The start page sees midnight come while it is open, and shows the new day's word.
- Long-press the icon for today's word in each language, straight to its game.
- Under the grid, the key of the three marks; the next tile to fill is framed; a game in Hard says
  so above its date.
- The first daily word is the one of 1 November 2026: the calendar already has days to play.
- The shared result marks a letter in its right place with a blue square, 🟦.
