package com.callbackdev.thatstheword.core.domain.words

import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Letter
import com.callbackdev.thatstheword.core.model.Word
import java.text.Normalizer

/**
 * A language's keys and its folding (PLANNING.md §4.1), as `tools/words/pipeline/folding.py`
 * writes the files: the two must agree letter for letter, or a word typed would not be found.
 *
 * The keys are A to Z in every language, indices 0 to 25, and Spanish's Ñ, 26. The Italian keyboard
 * has all 26 letters, for the loanwords a player types; a solution keeps to the Italian alphabet's
 * 21 (the owner, PLANNING §15, Open 6), which the files already ensure.
 */
class Alphabet private constructor(val language: Language, private val keys: String) {
    /** The keys, in their indices' order. */
    val letters: List<Letter> = keys.indices.map(::Letter)

    val size: Int get() = keys.length

    /** The key's character, in lower case: for the files' text records, never for a screen. */
    fun keyChar(letter: Letter): Char = keys[letter.index]

    /** The letter of a key typed (a hardware keyboard's, in either case), or null when it is none. */
    fun letterOf(char: Char): Letter? {
        val index = keys.indexOf(char.lowercaseChar())
        return if (index < 0) null else Letter(index)
    }

    /** A key form written in the keys' characters (`citta`) as a word, or null when it is not one. */
    fun parse(keyForm: String): Word? {
        val letters = keyForm.map { letterOf(it) ?: return null }
        return if (letters.isEmpty()) null else Word.of(letters)
    }

    /** The word's key form in lower case (`citta`): the files' keys. A screen draws its capitals. */
    fun keyForm(word: Word): String = buildString(word.length) {
        for (i in 0 until word.length) append(keys[word[i].index])
    }

    /**
     * The key form of a written word (*città*, *niño*): lower case, NFD, the combining marks
     * dropped, the ñ kept in Spanish before they go. Null when anything is left outside the keys
     * (an apostrophe, a hyphen, a space, a digit): then it is not a word of the game.
     */
    fun fold(written: String): Word? {
        val decomposed = Normalizer.normalize(written.lowercase(java.util.Locale.ROOT), Normalizer.Form.NFD)
        val out = StringBuilder(decomposed.length)
        for (ch in decomposed) {
            if (Character.getType(ch) == Character.NON_SPACING_MARK.toInt()) {
                if (language == Language.SPANISH && ch == COMBINING_TILDE && out.lastOrNull() == 'n') {
                    out.setCharAt(out.length - 1, 'ñ')
                }
                continue
            }
            out.append(ch)
        }
        return parse(out.toString())
    }

    /** Five bits a letter, the first letter highest: the files' packed form, sorted as the words. */
    fun pack(word: Word): Long {
        var value = 0L
        for (i in 0 until word.length) value = (value shl BITS_PER_LETTER) or word[i].index.toLong()
        return value
    }

    fun unpack(value: Long, length: Int): Word {
        val indices = IntArray(length)
        var rest = value
        for (i in length - 1 downTo 0) {
            indices[i] = (rest and LETTER_MASK).toInt()
            rest = rest ushr BITS_PER_LETTER
        }
        return Word.ofIndices(*indices)
    }

    companion object {
        const val BITS_PER_LETTER = 5
        private const val LETTER_MASK = 0x1FL
        private const val COMBINING_TILDE = '̃'
        private const val LATIN = "abcdefghijklmnopqrstuvwxyz"

        private val alphabets = Language.entries.associateWith { language ->
            Alphabet(language, if (language == Language.SPANISH) LATIN + "ñ" else LATIN)
        }

        fun of(language: Language): Alphabet = alphabets.getValue(language)
    }
}
