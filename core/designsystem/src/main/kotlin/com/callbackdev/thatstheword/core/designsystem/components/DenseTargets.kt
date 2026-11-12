package com.callbackdev.thatstheword.core.designsystem.components

/**
 * Marks targets packed tighter than a finger's 48dp on purpose (none yet: the keyboard's keys are
 * measured, PLANNING.md §7). Each is still its own labelled node, reached one by one by swiping
 * in TalkBack; the UI tests' accessibility checks (`:core:testing`) measure everything else, and
 * these for their labels.
 */
const val DENSE_TARGETS_TAG = "thatstheword_dense_targets"
