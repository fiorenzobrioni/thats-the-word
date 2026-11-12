package com.callbackdev.thatstheword.core.data

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import com.callbackdev.thatstheword.core.model.Language
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * The language the interface speaks (VISION §5): the per-app language the player chose in the
 * system's picker, else the phone's. A language the game does not speak reads as English, as the
 * strings fall back to `values/`.
 */
fun interface InterfaceLanguage {
    fun current(): Language
}

class SystemInterfaceLanguage @Inject constructor(@ApplicationContext private val context: Context) :
    InterfaceLanguage {
    override fun current(): Language {
        val chosen = context.getSystemService(LocaleManager::class.java)?.applicationLocales
        val locale = chosen?.takeIf { !it.isEmpty }?.get(0) ?: Resources.getSystem().configuration.locales[0]
        return Language.ofTag(locale.language) ?: Language.ENGLISH
    }
}
