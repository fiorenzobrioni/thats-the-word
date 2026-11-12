package com.callbackdev.thatstheword.core.domain

import com.callbackdev.thatstheword.core.domain.game.WordList
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.domain.words.WordsFile
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Word
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

/** A word from its key form in [language] (`"citta"`), for a test. */
fun word(key: String, language: Language = Language.ENGLISH): Word =
    requireNotNull(Alphabet.of(language).parse(key)) { "$key is not a key form" }

fun Word.key(language: Language = Language.ENGLISH): String = Alphabet.of(language).keyForm(this)

/** A word list of the given key forms. */
fun wordsOf(vararg keys: String, language: Language = Language.ENGLISH): WordList {
    val set = keys.mapTo(HashSet()) { word(it, language) }
    return WordList { it in set }
}

/**
 * Small words' files, written as `tools/words/pipeline/pack.py` writes them, so the decoder is
 * tested on the JVM without the real ones (PLANNING.md §6.6).
 */
object TestFiles {
    private fun header(kind: Char, language: Language, length: Int, count: Int, extra: Int = 0): ByteArray =
        ByteBuffer.allocate(WordsFile.HEADER).apply {
            put('T'.code.toByte())
            put('T'.code.toByte())
            put('W'.code.toByte())
            put(kind.code.toByte())
            put(WordsFile.VERSION.toByte())
            put(language.code.toByte())
            put(length.toByte())
            put(extra.toByte())
            putInt(count)
            putInt(0)
        }.array()

    private fun slots(kind: Char, language: Language, length: Int, values: List<Long>): ByteBuffer {
        val width = WordsFile.wordWidth(length)
        val out = ByteArrayOutputStream()
        out.write(header(kind, language, length, values.size))
        for (v in values) {
            val slot = ByteBuffer.allocate(8).putLong(v).array()
            out.write(slot, 8 - width, width)
        }
        return ByteBuffer.wrap(out.toByteArray())
    }

    private fun pack(key: String, language: Language) = Alphabet.of(language).pack(word(key, language))

    fun accepted(language: Language, keys: List<String>): ByteBuffer =
        slots('A', language, keys.first().length, keys.map { pack(it, language) }.sorted())

    fun solutions(language: Language, levels: Map<String, Int>): ByteBuffer = slots(
        'S',
        language,
        levels.keys.first().length,
        levels.map { (key, level) -> (pack(key, language) shl 2) or level.toLong() }.sorted(),
    )

    fun daily(language: Language, keys: List<String>): ByteBuffer {
        val width = WordsFile.wordWidth(keys.first().length)
        return slots(
            'D',
            language,
            keys.first().length,
            keys.mapIndexed { i, key ->
                val digest = MessageDigest.getInstance(
                    "SHA-256",
                ).digest("ttw-daily/${language.tag}/${i + 1}".toByteArray())
                var mask = 0L
                for (b in 0 until width) mask = (mask shl 8) or (digest[b].toLong() and 0xFF)
                pack(key, language) xor mask
            },
        )
    }

    /** Text records: each a key and its fields, a field a list of items. */
    fun table(
        kind: Char,
        language: Language,
        length: Int,
        records: Map<String, List<List<String>>>,
        extra: Int = 0,
    ): ByteBuffer {
        val encoded = records.map { (key, fields) ->
            val parts = listOf(key) + fields.map { it.joinToString("\u001e") }
            key.toByteArray(Charsets.UTF_8) to parts.joinToString("\u001f").toByteArray(Charsets.UTF_8)
        }.sortedWith { a, b -> compareBytes(a.first, b.first) }
        val out = ByteArrayOutputStream()
        out.write(header(kind, language, length, encoded.size, extra))
        var position = 0
        for ((_, record) in encoded) {
            out.write(ByteBuffer.allocate(4).putInt(position).array())
            position += record.size
        }
        out.write(ByteBuffer.allocate(4).putInt(position).array())
        encoded.forEach { out.write(it.second) }
        return ByteBuffer.wrap(out.toByteArray())
    }

    private fun compareBytes(a: ByteArray, b: ByteArray): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val d = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (d != 0) return d
        }
        return a.size - b.size
    }
}
