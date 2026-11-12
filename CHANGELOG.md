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
