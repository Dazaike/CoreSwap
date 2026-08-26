package com.coreswap.core

import android.app.Application
import com.coreswap.lib.bindings.LanguageIdentifier
import com.coreswap.mode.AppShortcuts
class CoreSwapApp : Application() {
    init {
        Native.initialize()
    }

    override fun onCreate() {
        super.onCreate()
        initializeI18n()
        AppShortcuts.setup(this)
    }

    /**
     * The engine returns translated option labels, so the i18n side of the library has to be
     * initialized before any Setting is read.
     */
    private fun initializeI18n() {
        val locales = resources.configuration.locales
        val languageIdentifiers = (0..<locales.size()).map { i ->
            val locale = locales[i]
            LanguageIdentifier(
                language = locale.language,
                script = locale.script.ifEmpty { null },
                region = locale.country.ifEmpty { null },
                variants = if (locale.variant.isEmpty()) emptyList() else listOf(locale.variant),
            )
        }
        Native.initializeI18n(languageIdentifiers)
    }
}
