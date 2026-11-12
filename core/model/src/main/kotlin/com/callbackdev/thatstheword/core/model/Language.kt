package com.callbackdev.thatstheword.core.model

/**
 * A language of the game (VISION §5): a game is played in one, and the interface speaks one.
 *
 * [code] is the language's byte in a words file's header and [tag] its directory and BCP 47 tag
 * (PLANNING.md §6.6): both are written by `tools/words/` and must never change.
 */
enum class Language(val code: Int, val tag: String) {
    ITALIAN(0, "it"),
    ENGLISH(1, "en"),
    SPANISH(2, "es"),
    ;

    companion object {
        fun ofCode(code: Int): Language = entries.firstOrNull { it.code == code }
            ?: throw IllegalArgumentException("no language has the code $code")

        fun ofTag(tag: String): Language? = entries.firstOrNull { it.tag == tag }
    }
}
