package com.callbackdev.thatstheword.core.domain.words

/**
 * A table of text records sorted by their key's UTF-8 bytes (the forms, the glossaries): `count + 1`
 * offsets of four bytes after the header, then the text. A record is UTF-8 fields separated by
 * U+001F, its key first; a field that holds a list separates its items by U+001E.
 */
class TextTable(private val file: WordsFile) {
    private val buffer get() = file.buffer
    private val textStart = WordsFile.HEADER + 4 * (file.count + 1)

    val size: Int get() = file.count

    private fun offset(index: Int): Int = file.buffer.getInt(WordsFile.HEADER + 4 * index)

    /** The record's fields after its key, or null when no record has [key]. */
    fun find(key: String): List<String>? {
        val wanted = key.toByteArray(Charsets.UTF_8)
        var low = 0
        var high = file.count - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val order = compareKey(mid, wanted)
            when {
                order < 0 -> low = mid + 1
                order > 0 -> high = mid - 1
                else -> return fields(mid).drop(1)
            }
        }
        return null
    }

    /** Every key, in order: for a test, or a list that needs them all. */
    fun keys(): List<String> = (0 until file.count).map { fields(it).first() }

    /** The record's key against [wanted], byte by byte, unsigned, as Python sorts bytes. */
    private fun compareKey(index: Int, wanted: ByteArray): Int {
        val start = textStart + offset(index)
        val end = textStart + offset(index + 1)
        var i = 0
        while (true) {
            val at = start + i
            val ended = at >= end || buffer.get(at) == FIELD_BYTE
            if (ended || i >= wanted.size) {
                return when {
                    ended && i >= wanted.size -> 0
                    ended -> -1
                    else -> 1
                }
            }
            val diff = (buffer.get(at).toInt() and 0xFF) - (wanted[i].toInt() and 0xFF)
            if (diff != 0) return diff
            i++
        }
    }

    private fun fields(index: Int): List<String> {
        val start = textStart + offset(index)
        val end = textStart + offset(index + 1)
        val bytes = ByteArray(end - start) { buffer.get(start + it) }
        return String(bytes, Charsets.UTF_8).split(FIELD)
    }

    companion object {
        const val FIELD = '\u001f'
        const val ITEM = '\u001e'
        private const val FIELD_BYTE: Byte = 0x1f

        fun items(field: String): List<String> = if (field.isEmpty()) emptyList() else field.split(ITEM)
    }
}
