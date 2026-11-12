package com.callbackdev.thatstheword.core.designsystem.components

/**
 * Marks targets packed tighter than a finger's 48dp on purpose: the game's keyboard, whose keys
 * are a row's share wide (ten to a row, narrower than 48dp on any phone, as every phone keyboard's
 * are) and taller than 48dp (PLANNING.md §7.3); and the calendar of the dailies, whose days are a
 * seventh of the column, as every calendar's are (under 48dp on a narrow phone, a little over on a
 * larger one). Each is still its own labelled node, reached one
 * by one by swiping in TalkBack; the UI tests' accessibility checks (`:core:testing`) measure
 * everything else, and these for their labels.
 */
const val DENSE_TARGETS_TAG = "thatstheword_dense_targets"
