package com.callbackdev.thatstheword.core.domain.words

import com.callbackdev.thatstheword.core.domain.game.WordList
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.Word
import java.security.MessageDigest

private fun WordsFile.expect(kind: WordsFile.Kind): WordsFile = apply {
    require(this.kind == kind) { "a ${this.kind} file where a $kind file was expected" }
}

/** The accepted words of a language and length, sorted: a guess is found by binary search. */
class AcceptedWords(file: WordsFile) : WordList {
    private val file = file.expect(WordsFile.Kind.ACCEPTED)
    private val alphabet = Alphabet.of(file.language)

    val language: Language get() = file.language
    val length: Int get() = file.length
    val size: Int get() = file.count

    override fun contains(word: Word): Boolean = word.length == file.length && file.search(alphabet.pack(word)) >= 0
}

/** The solutions of a language and length, each with its level in the two low bits. */
class Solutions(file: WordsFile) {
    private val file = file.expect(WordsFile.Kind.SOLUTIONS)
    private val alphabet = Alphabet.of(file.language)

    val language: Language get() = file.language
    val size: Int get() = file.count

    /** The word's level, or null when it is not a solution. */
    fun levelOf(word: Word): Level? {
        if (word.length != file.length) return null
        val index = file.search(alphabet.pack(word)) { it ushr 2 }
        return if (index < 0) null else Level.ofCode((file.slot(index) and 3L).toInt())
    }

    /** The words drawn at [level]: its own and every easier level's (the levels are cumulative). */
    fun at(level: Level): List<Word> = (0 until file.count).mapNotNull { i ->
        val slot = file.slot(i)
        if (level.includes(Level.ofCode((slot and 3L).toInt()))) alphabet.unpack(slot ushr 2, file.length) else null
    }
}

/**
 * A language's daily schedule, frozen and append-only (PLANNING.md §6.5): daily number `n` is entry
 * `n`. Each entry is XORed with the first bytes of SHA-256("ttw-daily/<language>/<n>"), so a glance
 * at the repository spoils nothing and any day is read alone.
 */
class DailySchedule(file: WordsFile) {
    private val file = file.expect(WordsFile.Kind.DAILY)
    private val alphabet = Alphabet.of(file.language)

    val language: Language get() = file.language

    /** How many dailies the schedule holds. */
    val size: Int get() = file.count

    /** Daily number [number]'s word, from 1 to [size]. */
    fun word(number: Int): Word {
        require(number in 1..file.count) { "no daily number $number in a schedule of ${file.count}" }
        return alphabet.unpack(file.slot(number - 1) xor mask(number), file.length)
    }

    private fun mask(number: Int): Long {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("ttw-daily/${file.language.tag}/$number".toByteArray(Charsets.UTF_8))
        var value = 0L
        for (i in 0 until file.width) value = (value shl 8) or (digest[i].toLong() and 0xFF)
        return value
    }
}
