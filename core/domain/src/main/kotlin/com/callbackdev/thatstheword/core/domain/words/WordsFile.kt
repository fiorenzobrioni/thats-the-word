package com.callbackdev.thatstheword.core.domain.words

import com.callbackdev.thatstheword.core.model.Language
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The files `tools/words/` writes (PLANNING.md §6.6, docs/adr/0003-words.md, the format's own
 * description in `tools/words/pipeline/pack.py`), read in place from a buffer: mapped from the APK
 * on a phone, wrapped around bytes in a test. Every read is absolute, so one buffer serves any
 * thread and nothing is copied.
 *
 * Every file opens with sixteen bytes, big-endian: `TTW` and the file's kind, the version, the game
 * language, the words' length, an extra byte (a glossary's interface language), the count, four
 * reserved.
 */
class WordsFile(buffer: ByteBuffer) {
    val buffer: ByteBuffer = buffer.duplicate().order(ByteOrder.BIG_ENDIAN)

    enum class Kind(val tag: Char) {
        ACCEPTED('A'),
        SOLUTIONS('S'),
        DAILY('D'),
        FORMS('F'),
        GLOSSARY('G'),
    }

    val kind: Kind
    val language: Language
    val length: Int
    val extra: Int
    val count: Int

    init {
        if (this.buffer.limit() < HEADER ||
            this.buffer.get(0) != 'T'.code.toByte() ||
            this.buffer.get(1) != 'T'.code.toByte() ||
            this.buffer.get(2) != 'W'.code.toByte()
        ) {
            throw IllegalArgumentException("not a words file")
        }
        val tag = (this.buffer.get(3).toInt() and 0xFF).toChar()
        kind = Kind.entries.firstOrNull { it.tag == tag } ?: throw IllegalArgumentException("no kind $tag")
        val version = byte(4)
        require(version == VERSION) { "a words file of version $version: this app reads $VERSION" }
        language = Language.ofCode(byte(5))
        length = byte(6)
        extra = byte(7)
        count = this.buffer.getInt(8)
        require(count >= 0) { "a negative count" }
    }

    private fun byte(index: Int): Int = buffer.get(index).toInt() and 0xFF

    /** The bytes a packed word takes here: four up to six letters, eight from seven to twelve. */
    val width: Int get() = wordWidth(length)

    /** The packed value of slot [index] (a word's, a solution's with its level, a masked daily). */
    fun slot(index: Int): Long {
        val at = HEADER + index * width
        return if (width == 4) buffer.getInt(at).toLong() and 0xFFFF_FFFFL else buffer.getLong(at)
    }

    /** The index of [value] among sorted slots, or −1: a binary search with no decoding. */
    fun search(value: Long, transform: (Long) -> Long = { it }): Int {
        var low = 0
        var high = count - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val found = transform(slot(mid))
            when {
                found < value -> low = mid + 1
                found > value -> high = mid - 1
                else -> return mid
            }
        }
        return -1
    }

    companion object {
        const val HEADER = 16
        const val VERSION = 1
        const val MAX_LENGTH = 12

        fun wordWidth(length: Int): Int {
            require(length in 1..MAX_LENGTH) { "no word file for $length letters" }
            return if (length * Alphabet.BITS_PER_LETTER + 2 <= 32) 4 else 8
        }
    }
}
