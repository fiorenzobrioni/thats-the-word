package com.callbackdev.thatstheword.core.data

import com.callbackdev.thatstheword.core.domain.words.Lexicon
import com.callbackdev.thatstheword.core.domain.words.WordFiles
import com.callbackdev.thatstheword.core.model.Language
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The three game languages' words, each opened the first time it is asked for and kept for the
 * app's life: a file is mapped once, and its pages are the system's to keep or drop (PLANNING.md §9).
 */
@Singleton
class Lexicons @Inject constructor(private val files: WordFiles) {
    private val lexicons = Language.entries.associateWith { lazy { Lexicon(files, it) } }

    fun of(language: Language): Lexicon = lexicons.getValue(language).value
}
