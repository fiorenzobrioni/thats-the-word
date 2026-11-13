package com.callbackdev.thatstheword.core.domain.widget

import kotlin.math.max
import kotlin.math.min

/**
 * **The widgets' type scale** (PLANNING.md §7.8): the sizes a home-screen card prints in, by role,
 * the family's steps, so a line of one kind is the same size on every card of the family.
 *
 * - [CAPTION]: read last; the eyebrow, a row's status under its tiles.
 * - [LABEL]: a row's status beside its tiles, the streak's line, a language's name in a small card.
 * - [BODY]: a language's name, the date.
 * - [TITLE]: the streak's figure in a one-row card.
 */
object WidgetType {
    const val CAPTION = 11f
    const val LABEL = 13f
    const val BODY = 16f
    const val TITLE = 22f

    /** Every step, for the test that holds the card to them. */
    val steps: Set<Float> = setOf(CAPTION, LABEL, BODY, TITLE)
}

/**
 * The card's three shapes, read off the size the launcher granted: [CELL] one cell (the three
 * languages, each its code and one tile), [ROW] one row high (three columns, and the streak's when
 * it fits), [TALL] two rows and up (the eyebrow, the date, a row per language, the streak's line).
 */
enum class CardForm { CELL, ROW, TALL }

/** How a language is named on its row: its name in the interface's language, or its code. */
enum class RowLabel { NAME, CODE }

/**
 * The card's words, measured in the launcher's face, in ems (a text's width at 1 sp, the font
 * scale left out): the widest of each kind, so whichever language a row shows, its words fit.
 *
 * @property nameEm the widest language's name, in the label's weight.
 * @property codeEm the widest code ("IT", "EN", "ES").
 * @property statusEm the widest status the three rows show ("Found in 4", "Under way").
 * @property eyebrowEm "TODAY’S WORD · #13", and [eyebrowShortEm] its short form ("#13").
 * @property dateEm the date in the locale's long form.
 * @property streakEm the streak's line ("7 days in a row in Italian"); 0 with no streak running;
 *   [streakShortEm] its short form ("7 days in a row"), for a narrow card.
 * @property streakFigureEm the streak's figure, bold; [streakWordEm] the word under it ("in a row").
 */
data class CardWords(
    val nameEm: Float,
    val codeEm: Float,
    val statusEm: Float,
    val eyebrowEm: Float,
    val eyebrowShortEm: Float,
    val dateEm: Float,
    val streakEm: Float,
    val streakShortEm: Float,
    val streakFigureEm: Float,
    val streakWordEm: Float,
) {
    val hasStreak: Boolean get() = streakEm > 0f
}

/** A grant and the words to set in it; [fontScale] is the reader's text size. */
data class CardSpec(val width: Float, val height: Float, val fontScale: Float, val words: CardWords)

/**
 * What the card draws at a size: every choice a form makes, decided here and only here, so a test
 * pins it at the family's reference grants.
 *
 * @property tile a tile's side in dp; [tiles] how many a language shows: the word's five, or one
 *   that stands for the day (the calendar's glyph) where five would be too small to read.
 * @property labelSp the language's name or code size; [status] whether its short status is said.
 * @property eyebrow whether the eyebrow is drawn whole ("TODAY’S WORD · #13") or short ("#13").
 * @property date the date under the eyebrow; [streak] the streak's line, or its column in a row;
 *   [streakShort] the line in its short form.
 * @property languages how many languages the card shows: the three, or on one cell at a large
 *   text size the game language's alone, rather than three cut.
 * @property rowGap the gap between a tall card's rows: the family's 6 dp, growing a little where
 *   the card is taller than its rows, so they stand apart rather than huddle in the middle.
 */
data class CardPlan(
    val form: CardForm,
    val label: RowLabel,
    val labelSp: Float,
    val tile: Float,
    val tiles: Int,
    val status: Boolean,
    val eyebrow: Boolean = true,
    val date: Boolean = false,
    val streak: Boolean = false,
    val streakShort: Boolean = false,
    val languages: Int = LANGUAGE_COUNT,
    val rowGap: Float = CardFit.ROW_GAP,
) {
    companion object {
        const val LANGUAGE_COUNT = 3
    }
}

/**
 * The card's arithmetic (PLANNING.md §7.8): the widget measures its words, this decides what fits.
 * Pure, so `CardFitTest` pins it at the family's reference grants: one cell 85 dp; rows 85, 189,
 * 293; two, three, four cells 159, 250, 340.
 *
 * A line is counted as the family's cards count it, 1.32 em; nothing is cut, a part is left out
 * instead, in the order the card can best spare it (`TALL_ORDER`), and last the five tiles for
 * the day's one.
 */
object CardFit {
    /** The family's insets: 14 dp on a card, 6 dp top and bottom on a row, 8 dp on one cell. */
    const val CARD_PADDING = 14f
    const val CARD_PADDING_SNUG = 6f
    const val CELL_PADDING = 8f

    /** From two rows high a card stands tall; under it, from two cells wide, it is a row. */
    const val TALL_MIN_HEIGHT = 130f
    const val ROW_MIN_WIDTH = 130f

    /** The family's line: 1.32 em. */
    const val LINE = 1.32f

    const val COLUMN_GAP = 10f
    const val ROW_GAP = 6f
    const val SECTION_GAP = 8f
    const val STACK_GAP = 4f

    /** A tile never grows past the start page's small tiles, nor shrinks past what an eye reads. */
    const val MAX_TILE = 28f
    const val MAX_ROW_TILE = 18f
    const val MAX_CELL_TILE = 16f
    const val GOOD_TILE = 16f
    const val MIN_TILE = 9f
    const val MIN_GLYPH = 8f

    /** The gap between two tiles of a row, as a share of a tile, never under 2 dp. */
    fun tileGap(tile: Float): Float = max(MIN_TILE_GAP, tile * TILE_GAP_SHARE)

    /** How wide [count] tiles of side [tile] stand. */
    fun tilesWidth(tile: Float, count: Int = WORD_LENGTH): Float = tile * count + tileGap(tile) * (count - 1)

    /** The largest tile [count] of which fit in [width]. */
    fun tileFor(width: Float, count: Int = WORD_LENGTH): Float {
        if (width <= 0f) return 0f
        val shared = width / (count + (count - 1) * TILE_GAP_SHARE)
        return if (shared * TILE_GAP_SHARE >= MIN_TILE_GAP) shared else (width - (count - 1) * MIN_TILE_GAP) / count
    }

    fun line(sp: Float, fontScale: Float): Float = sp * fontScale * LINE

    fun form(width: Float, height: Float): CardForm = when {
        height >= TALL_MIN_HEIGHT -> CardForm.TALL
        width >= ROW_MIN_WIDTH -> CardForm.ROW
        else -> CardForm.CELL
    }

    fun plan(spec: CardSpec): CardPlan = when (form(spec.width, spec.height)) {
        CardForm.TALL -> tall(spec)
        CardForm.ROW -> row(spec)
        CardForm.CELL -> cell(spec)
    }

    /** The eyebrow, a row per language, and around them the date and the streak's line where they fit. */
    private fun tall(spec: CardSpec): CardPlan {
        val s = spec.fontScale
        val w = spec.words
        val inner = spec.width - CARD_PADDING * 2
        val high = spec.height - CARD_PADDING * 2
        val eyebrow = w.eyebrowEm * WidgetType.CAPTION * s <= inner
        val dateFits = w.dateEm * WidgetType.BODY * s <= inner
        val streakLong = w.hasStreak && w.streakEm * WidgetType.LABEL * s <= inner
        val streakShort = !streakLong && w.hasStreak && w.streakShortEm * WidgetType.LABEL * s <= inner
        // Richest first; the first option whose tiles are comfortable wins, then the first readable.
        val options = TALL_ORDER
            .filter { !it.date || dateFits }
            .map { it.copy(streak = it.streak && (streakLong || streakShort)) }
            .flatMap { listOf(it, it.copy(labelSp = WidgetType.LABEL)) }
            .distinct()
        for (threshold in listOf(GOOD_TILE, MIN_TILE)) {
            for (option in options) {
                val tile = tallTile(option, inner, high, s, w) ?: continue
                if (tile >= threshold) {
                    val slot = (high - tallHeader(option, s) - 2 * ROW_GAP) / LANGUAGES
                    val row =
                        max(max(tile, line(option.labelSp, s)), if (option.status) line(WidgetType.LABEL, s) else 0f)
                    return option.plan(tile, WORD_LENGTH, eyebrow).copy(
                        streakShort = option.streak && streakShort,
                        rowGap = ROW_GAP + min(MAX_EXTRA_ROW_GAP, max(0f, slot - row)),
                    )
                }
            }
        }
        // Too narrow for five: one tile per language, the day's glyph, beside its code.
        val bare = TallOption(date = false, streak = false, RowLabel.CODE, status = false, labelSp = WidgetType.LABEL)
        val slot = (high - tallHeader(bare, s) - 2 * ROW_GAP) / LANGUAGES
        val room = inner - w.codeEm * bare.labelSp * s - COLUMN_GAP
        return bare.plan(min(min(slot, room), MAX_ROW_TILE).coerceAtLeast(MIN_GLYPH), 1, eyebrow)
    }

    private data class TallOption(
        val date: Boolean,
        val streak: Boolean,
        val label: RowLabel,
        val status: Boolean,
        val labelSp: Float,
    ) {
        fun plan(tile: Float, tiles: Int, eyebrow: Boolean) = CardPlan(
            form = CardForm.TALL,
            label = label,
            labelSp = labelSp,
            tile = tile,
            tiles = tiles,
            status = status,
            eyebrow = eyebrow,
            date = date,
            streak = streak,
        )
    }

    private fun tallHeader(option: TallOption, s: Float): Float = line(WidgetType.CAPTION, s) +
        (if (option.date) line(WidgetType.BODY, s) else 0f) +
        SECTION_GAP +
        (if (option.streak) SECTION_GAP + line(WidgetType.LABEL, s) else 0f)

    /** The tile [option] leaves, or null where its words do not fit at all. */
    private fun tallTile(option: TallOption, inner: Float, high: Float, s: Float, w: CardWords): Float? {
        val slot = (high - tallHeader(option, s) - 2 * ROW_GAP) / LANGUAGES
        val labelLine = line(option.labelSp, s)
        val statusLine = if (option.status) line(WidgetType.LABEL, s) else 0f
        if (slot < max(labelLine, statusLine)) return null
        val labelWidth = (if (option.label == RowLabel.NAME) w.nameEm else w.codeEm) * option.labelSp * s
        val statusWidth = if (option.status) w.statusEm * WidgetType.LABEL * s + COLUMN_GAP else 0f
        val room = inner - labelWidth - COLUMN_GAP - statusWidth
        return min(min(slot, MAX_TILE), tileFor(room)).takeIf { it > 0f }
    }

    /** One row high: a column per language, its label over its tiles over its status; the streak's own column. */
    private fun row(spec: CardSpec): CardPlan {
        val s = spec.fontScale
        val w = spec.words
        val inner = spec.width - CARD_PADDING * 2
        val high = spec.height - CARD_PADDING_SNUG * 2
        val streakColumn = w.hasStreak
        for (streak in listOf(streakColumn, false).distinct()) {
            val columns = LANGUAGES + if (streak) 1 else 0
            val column = (inner - (columns - 1) * COLUMN_GAP) / columns
            if (streak && !streakFitsColumn(column, high, s, w)) continue
            val label = if (w.nameEm * WidgetType.LABEL * s <= column) RowLabel.NAME else RowLabel.CODE
            // A code too wide for its column at large text steps down a size before anything is cut.
            val labelSp = if (label == RowLabel.NAME || w.codeEm * WidgetType.LABEL * s <= column) {
                WidgetType.LABEL
            } else {
                WidgetType.CAPTION
            }
            val labelLine = line(labelSp, s) + STACK_GAP
            for (status in listOf(true, false)) {
                if (status && w.statusEm * WidgetType.CAPTION * s > column) continue
                val statusLine = if (status) STACK_GAP + line(WidgetType.CAPTION, s) else 0f
                val tile = min(min(MAX_ROW_TILE, high - labelLine - statusLine), tileFor(column))
                if (tile >= MIN_TILE) {
                    return CardPlan(CardForm.ROW, label, labelSp, tile, WORD_LENGTH, status, streak = streak)
                }
            }
            // Too narrow for five: the day's one tile under the label.
            val glyph = min(min(MAX_ROW_TILE, high - labelLine), column)
            if (glyph >= MIN_GLYPH) return CardPlan(CardForm.ROW, label, labelSp, glyph, 1, false, streak = streak)
        }
        return CardPlan(CardForm.ROW, RowLabel.CODE, WidgetType.CAPTION, MIN_GLYPH, 1, false)
    }

    private fun streakFitsColumn(column: Float, high: Float, s: Float, w: CardWords): Boolean =
        w.streakFigureEm * WidgetType.TITLE * s <= column &&
            w.streakWordEm * WidgetType.CAPTION * s <= column &&
            line(WidgetType.TITLE, s) + line(WidgetType.CAPTION, s) <= high

    /**
     * One cell: the three languages, each its code and the day's one tile; where three lines do
     * not fit (a large text size), the game language's alone.
     */
    private fun cell(spec: CardSpec): CardPlan {
        val s = spec.fontScale
        val high = spec.height - CARD_PADDING_SNUG * 2
        val inner = spec.width - CELL_PADDING * 2
        val languages = if (LANGUAGES * line(WidgetType.CAPTION, s) + 2 * STACK_GAP <= high) LANGUAGES else 1
        val slot = (high - (languages - 1) * STACK_GAP) / languages
        val labelSp = if (line(WidgetType.LABEL, s) <= slot) WidgetType.LABEL else WidgetType.CAPTION
        val room = inner - spec.words.codeEm * labelSp * s - COLUMN_GAP
        val tile = min(min(MAX_CELL_TILE, slot), room).coerceAtLeast(MIN_GLYPH)
        return CardPlan(CardForm.CELL, RowLabel.CODE, labelSp, tile, 1, false, eyebrow = false, languages = languages)
    }

    /** How much a tall card's row gap may grow: three rows, two gaps, never more than the slots leave. */
    private const val MAX_EXTRA_ROW_GAP = 10f
    private const val WORD_LENGTH = 5
    private const val LANGUAGES = CardPlan.LANGUAGE_COUNT
    private const val TILE_GAP_SHARE = 0.16f
    private const val MIN_TILE_GAP = 2f

    /**
     * A tall card's options, richest first, each part given up in the order the card can best spare
     * it: the date, the status beside the tiles, the name for its code, the streak's line.
     */
    private val TALL_ORDER = listOf(
        TallOption(date = true, streak = true, RowLabel.NAME, status = true, labelSp = WidgetType.BODY),
        TallOption(date = false, streak = true, RowLabel.NAME, status = true, labelSp = WidgetType.BODY),
        TallOption(date = false, streak = true, RowLabel.NAME, status = false, labelSp = WidgetType.BODY),
        TallOption(date = false, streak = true, RowLabel.CODE, status = false, labelSp = WidgetType.BODY),
        TallOption(date = false, streak = false, RowLabel.NAME, status = false, labelSp = WidgetType.BODY),
        TallOption(date = false, streak = false, RowLabel.CODE, status = false, labelSp = WidgetType.BODY),
    )
}
