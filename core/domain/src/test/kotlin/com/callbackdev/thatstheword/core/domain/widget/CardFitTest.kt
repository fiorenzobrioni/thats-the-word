package com.callbackdev.thatstheword.core.domain.widget

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.max

/**
 * The card's arithmetic (PLANNING.md §7.8) at the family's reference grants: one cell 85 dp; rows
 * 85, 189, 293; two, three, four cells 159, 250, 340. Its words measured as an English phone's
 * face sets them (the widget measures them for real; these are their sizes there).
 */
class CardFitTest {
    private val english = CardWords(
        nameEm = 3.9f,
        codeEm = 1.45f,
        statusEm = 4.9f,
        eyebrowEm = 10.6f,
        eyebrowShortEm = 1.9f,
        dateEm = 9.4f,
        streakEm = 12.6f,
        streakShortEm = 7.2f,
        streakFigureEm = 0.6f,
        streakWordEm = 3.7f,
    )
    private val noStreak = english.copy(streakEm = 0f, streakShortEm = 0f, streakFigureEm = 0f, streakWordEm = 0f)

    private val widths = listOf(85f, 159f, 250f, 340f)
    private val heights = listOf(85f, 189f, 293f)

    private fun plan(width: Float, height: Float, scale: Float = 1f, words: CardWords = english) =
        CardFit.plan(CardSpec(width, height, scale, words))

    @Test
    fun `the forms by grant`() {
        assertThat(CardFit.form(85f, 85f)).isEqualTo(CardForm.CELL)
        assertThat(CardFit.form(159f, 85f)).isEqualTo(CardForm.ROW)
        assertThat(CardFit.form(340f, 85f)).isEqualTo(CardForm.ROW)
        assertThat(CardFit.form(85f, 189f)).isEqualTo(CardForm.TALL)
        assertThat(CardFit.form(340f, 189f)).isEqualTo(CardForm.TALL)
    }

    @Test
    fun `four by two, where a card is placed - everything, the tiles at ease`() {
        val plan = plan(340f, 189f)
        assertThat(plan.form).isEqualTo(CardForm.TALL)
        assertThat(plan.label).isEqualTo(RowLabel.NAME)
        assertThat(plan.labelSp).isEqualTo(WidgetType.BODY)
        assertThat(plan.status).isTrue()
        assertThat(plan.tiles).isEqualTo(5)
        assertThat(plan.tile).isAtLeast(CardFit.GOOD_TILE)
        assertThat(plan.date).isTrue()
        assertThat(plan.streak).isTrue()
        assertThat(plan.streakShort).isFalse()
        assertThat(plan.eyebrow).isTrue()
    }

    @Test
    fun `four by three - the tiles at their largest`() {
        val plan = plan(340f, 293f)
        assertThat(plan.tile).isEqualTo(CardFit.MAX_TILE)
        assertThat(plan.date).isTrue()
        assertThat(plan.status).isTrue()
    }

    @Test
    fun `two by two - five tiles, the words a narrow card has room for`() {
        val plan = plan(159f, 189f)
        assertThat(plan.form).isEqualTo(CardForm.TALL)
        assertThat(plan.tiles).isEqualTo(5)
        assertThat(plan.tile).isAtLeast(CardFit.MIN_TILE)
        assertThat(plan.status).isFalse()
        assertThat(plan.streak).isTrue()
        assertThat(plan.streakShort).isTrue()
    }

    @Test
    fun `one by two - too narrow for five, the day's one tile`() {
        val plan = plan(85f, 189f)
        assertThat(plan.tiles).isEqualTo(1)
        assertThat(plan.label).isEqualTo(RowLabel.CODE)
    }

    @Test
    fun `four by one - three columns and the streak's`() {
        val plan = plan(340f, 85f)
        assertThat(plan.form).isEqualTo(CardForm.ROW)
        assertThat(plan.streak).isTrue()
        assertThat(plan.tiles).isEqualTo(5)
        assertThat(plan.label).isEqualTo(RowLabel.NAME)
    }

    @Test
    fun `two by one - the codes, and no streak column`() {
        val plan = plan(159f, 85f)
        assertThat(plan.form).isEqualTo(CardForm.ROW)
        assertThat(plan.streak).isFalse()
        assertThat(plan.label).isEqualTo(RowLabel.CODE)
    }

    @Test
    fun `one cell - three codes and three tiles`() {
        val plan = plan(85f, 85f)
        assertThat(plan.form).isEqualTo(CardForm.CELL)
        assertThat(plan.tiles).isEqualTo(1)
        assertThat(plan.tile).isAtLeast(CardFit.MIN_GLYPH)
    }

    @Test
    fun `one cell at twice the text size - the game language alone, rather than three cut`() {
        val plan = plan(85f, 85f, scale = 2f)
        assertThat(plan.languages).isEqualTo(1)
        assertThat(plan(85f, 85f).languages).isEqualTo(3)
    }

    @Test
    fun `no streak running - no line for it`() {
        assertThat(plan(340f, 189f, words = noStreak).streak).isFalse()
        assertThat(plan(340f, 85f, words = noStreak).streak).isFalse()
    }

    @Test
    fun `twice the text size - the parts give way, nothing is cut`() {
        val plan = plan(340f, 189f, scale = 2f)
        assertThat(plan.form).isEqualTo(CardForm.TALL)
        assertThat(plan.tiles).isEqualTo(5)
        assertThat(plan.date).isFalse()
    }

    @Test
    fun `every grant, every text size - what is planned fits the card`() {
        for (scale in listOf(1f, 1.3f, 1.5f, 2f)) {
            for (words in listOf(english, noStreak)) {
                for (w in widths + listOf(188f)) {
                    for (h in heights + listOf(218f)) {
                        val plan = plan(w, h, scale, words)
                        val where = "${w}x$h at $scale"
                        assertWithMessage("tile, $where").that(plan.tile).isAtLeast(CardFit.MIN_GLYPH)
                        assertWithMessage("width, $where").that(usedWidth(plan, w, scale, words)).isAtMost(w + 0.01f)
                        if (plan.form == CardForm.TALL && plan.tiles == 5) {
                            assertWithMessage("height, $where").that(usedHeight(plan, scale)).isAtMost(h + 0.01f)
                        }
                    }
                }
            }
        }
    }

    /** The widest line a plan draws, with the card's insets. */
    private fun usedWidth(plan: CardPlan, width: Float, s: Float, w: CardWords): Float = when (plan.form) {
        CardForm.TALL -> {
            val label = (if (plan.label == RowLabel.NAME) w.nameEm else w.codeEm) * plan.labelSp * s
            val status = if (plan.status) CardFit.COLUMN_GAP + w.statusEm * WidgetType.LABEL * s else 0f
            CardFit.CARD_PADDING * 2 + label + CardFit.COLUMN_GAP + CardFit.tilesWidth(plan.tile, plan.tiles) + status
        }

        CardForm.ROW -> {
            val columns = 3 + if (plan.streak) 1 else 0
            val column = (width - CardFit.CARD_PADDING * 2 - (columns - 1) * CardFit.COLUMN_GAP) / columns
            val label = (if (plan.label == RowLabel.NAME) w.nameEm else w.codeEm) * plan.labelSp * s
            val widest = max(max(label, CardFit.tilesWidth(plan.tile, plan.tiles)), 0f)
            if (widest <= column) width else width + widest - column
        }

        CardForm.CELL -> CardFit.CELL_PADDING * 2 + w.codeEm * plan.labelSp * s + CardFit.COLUMN_GAP + plan.tile
    }

    private fun usedHeight(plan: CardPlan, s: Float): Float {
        val header = CardFit.line(WidgetType.CAPTION, s) +
            (if (plan.date) CardFit.line(WidgetType.BODY, s) else 0f) +
            CardFit.SECTION_GAP +
            (if (plan.streak) CardFit.SECTION_GAP + CardFit.line(WidgetType.LABEL, s) else 0f)
        val row = max(
            max(plan.tile, CardFit.line(plan.labelSp, s)),
            if (plan.status) CardFit.line(WidgetType.LABEL, s) else 0f,
        )
        return CardFit.CARD_PADDING * 2 + header + row * 3 + plan.rowGap * 2
    }
}
