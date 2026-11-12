package com.callbackdev.thatstheword.core.model

/**
 * A letter: its index among its language's keys (PLANNING.md §4.1). A to Z are 0 to 25 in every
 * language, and Spanish's Ñ is 26, a letter like any other: no `Char` arithmetic in the engine.
 */
@JvmInline
value class Letter(val index: Int) {
    init {
        require(index in 0 until MAX_KEYS) { "no key has the index $index" }
    }

    companion object {
        /** Five bits a letter in the words' files, so at most 32 keys. */
        const val MAX_KEYS = 32
    }
}

/**
 * A word in its key form, the letters as typed (PLANNING.md §4.1): *città* is the word C I T T A.
 * Its written forms, with their accents, are the words' files' (`WrittenForm`), never this.
 *
 * Equal by its letters. The letters are held as indices, so a word is compared, packed and
 * counted without strings.
 */
class Word private constructor(private val indices: IntArray) {
    val length: Int get() = indices.size

    operator fun get(position: Int): Letter = Letter(indices[position])

    val letters: List<Letter> get() = indices.map(::Letter)

    override fun equals(other: Any?): Boolean = other is Word && indices.contentEquals(other.indices)

    override fun hashCode(): Int = indices.contentHashCode()

    /** The indices, for a log or a failed test; the key form needs its language (`Alphabet`). */
    override fun toString(): String = indices.joinToString(prefix = "Word(", postfix = ")")

    companion object {
        fun of(letters: List<Letter>): Word = Word(IntArray(letters.size) { letters[it].index })

        fun ofIndices(vararg indices: Int): Word {
            indices.forEach { Letter(it) }
            return Word(indices.copyOf())
        }
    }
}
