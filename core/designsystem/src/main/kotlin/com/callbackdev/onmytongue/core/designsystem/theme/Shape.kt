package com.callbackdev.onmytongue.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** The shape scale (`docs/adr/0002-design-language.md`). Chips and buttons are fully round at the call site. */
internal val OnMyTongueShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** A group of rows or a card on a screen's list: the 24dp group ground. */
val GroupShape = RoundedCornerShape(24.dp)

/** The list rhythm: what a section header costs above and below. */
val SectionTop = 16.dp
val SectionBottom = 4.dp

/** The screen margin. */
val ScreenMargin = 16.dp

/**
 * A letter's tile: a rounded square, rounder than a card's corner so a row of them reads as
 * pieces, not as a table's cells. Its corner is a fraction of its side, so a smaller tile (a
 * longer word, a narrow phone) keeps the same drawing. The feedback's shapes join it in Phase 3
 * (PLANNING.md §7).
 */
const val TILE_CORNER_PERCENT = 22
