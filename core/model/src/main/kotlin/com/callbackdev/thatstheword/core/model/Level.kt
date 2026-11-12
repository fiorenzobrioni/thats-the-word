package com.callbackdev.thatstheword.core.model

/**
 * A level by frequency (VISION §9, PLANNING.md §4.7, §6.3). The levels are cumulative (the owner,
 * PLANNING §15, Open 12): a level holds its own words and every easier level's, and [COMPLETE]
 * holds every solution. Each game language has its own.
 *
 * [code] is the level's two bits in a solutions file (PLANNING.md §6.6).
 */
enum class Level(val code: Int) {
    BASE(0),
    INTERMEDIATE(1),
    ADVANCED(2),
    COMPLETE(3),
    ;

    /** Whether a word of [wordLevel] is drawn at this level. */
    fun includes(wordLevel: Level): Boolean = wordLevel.ordinal <= ordinal

    companion object {
        fun ofCode(code: Int): Level = entries.firstOrNull { it.code == code }
            ?: throw IllegalArgumentException("no level has the code $code")

        /** The dailies draw from these, because a daily is the same for everyone (VISION §9). */
        val DAILY: Level = INTERMEDIATE
    }
}
