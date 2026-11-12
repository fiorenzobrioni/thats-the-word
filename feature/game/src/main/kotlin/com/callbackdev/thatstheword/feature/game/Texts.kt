package com.callbackdev.thatstheword.feature.game

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.callbackdev.thatstheword.core.domain.game.Refusal
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Mark

@StringRes
internal fun languageName(language: Language): Int = when (language) {
    Language.ITALIAN -> R.string.language_it
    Language.ENGLISH -> R.string.language_en
    Language.SPANISH -> R.string.language_es
}

/** A mark's name in the key under the grid: a word or two, capitalised as a label. */
@StringRes
internal fun keyName(mark: Mark): Int = when (mark) {
    Mark.RIGHT_PLACE -> R.string.key_right_place
    Mark.ELSEWHERE -> R.string.key_elsewhere
    Mark.ABSENT -> R.string.key_absent
}

@StringRes
internal fun markName(mark: Mark): Int = when (mark) {
    Mark.RIGHT_PLACE -> R.string.mark_right_place
    Mark.ELSEWHERE -> R.string.mark_elsewhere
    Mark.ABSENT -> R.string.mark_absent
}

/** A revealed row as TalkBack reads it, one sentence (PLANNING.md §7.7): "Row 2: C, elsewhere; A, right place; …". */
@Composable
internal fun rowSentence(number: Int, row: RowUi): String {
    val resources = LocalResources.current
    val letters = row.letters.indices.joinToString("; ") { i ->
        resources.getString(
            R.string.letter_mark,
            row.letters[i].toString(),
            resources.getString(markName(row.marks[i])),
        )
    }
    return stringResource(R.string.row_revealed, number, letters)
}

/** The line above the keyboard: why the word was refused (PLANNING.md §7.3). */
@Composable
internal fun refusalText(refusal: RefusalUi): String? = when (val reason = refusal.reason) {
    Refusal.TooShort -> stringResource(R.string.refusal_too_short)

    Refusal.NotAWord -> stringResource(R.string.refusal_not_a_word)

    is Refusal.MustBeAt -> {
        val ordinals = stringArrayResource(R.array.ordinals)
        stringResource(R.string.refusal_must_be_at, ordinals[reason.position], refusal.letter.toString())
    }

    is Refusal.MustContain -> pluralStringResource(
        R.plurals.refusal_must_contain,
        reason.times,
        reason.times,
        refusal.letter.toString(),
    )

    Refusal.Finished -> null
}
