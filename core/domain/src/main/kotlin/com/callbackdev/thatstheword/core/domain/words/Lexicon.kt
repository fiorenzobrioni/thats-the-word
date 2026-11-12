package com.callbackdev.thatstheword.core.domain.words

import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Word
import java.nio.ByteBuffer

/**
 * Where the words' files come from: the APK's assets, mapped, on a phone (`:core:words`); bytes a
 * test writes, on the JVM. [path] is relative to the words' directory: `it/accepted-5.bin`.
 */
fun interface WordFiles {
    fun open(path: String): ByteBuffer
}

/**
 * A game language's words of one length (PLANNING.md §6): the accepted set, the solutions with
 * their levels, the daily schedule, the written forms and a glossary per interface language, each
 * opened the first time it is asked for.
 */
class Lexicon(private val files: WordFiles, val language: Language, val length: Int = 5) {
    private fun file(name: String): WordsFile {
        val file = WordsFile(files.open("${language.tag}/$name"))
        require(file.language == language) { "$name is a ${file.language} file" }
        return file
    }

    val accepted: AcceptedWords by lazy { AcceptedWords(file("accepted-$length.bin")) }
    val solutions: Solutions by lazy { Solutions(file("solutions-$length.bin")) }
    val daily: DailySchedule by lazy { DailySchedule(file("daily.bin")) }
    val forms: Forms by lazy { Forms(file("forms-$length.bin")) }

    private val glossaries = Language.entries.associateWith { lazy { Glossary(file("gloss-${it.tag}.bin")) } }

    fun glossary(interfaceLanguage: Language): Glossary = glossaries.getValue(interfaceLanguage).value

    /** The solution's meaning in [interfaceLanguage], by its own written form. */
    fun gloss(solution: Word, interfaceLanguage: Language): GlossEntry? =
        glossary(interfaceLanguage).entry(forms.solutionDisplay(solution))

    /**
     * The hints [solution] can give a player of [interfaceLanguage] (PLANNING.md §4.5): none in the
     * game's own language; the translation and the definition only where the glossary has them.
     */
    fun hintKinds(solution: Word, interfaceLanguage: Language): Set<HintKind> {
        if (interfaceLanguage == language) return emptySet()
        val entry = gloss(solution, interfaceLanguage)
        return buildSet {
            if (!entry?.translations.isNullOrEmpty()) add(HintKind.TRANSLATION)
            if (entry?.meaning != null) add(HintKind.DEFINITION)
            add(HintKind.LETTER)
        }
    }
}
