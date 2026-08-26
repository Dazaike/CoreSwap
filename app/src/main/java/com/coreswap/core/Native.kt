package com.coreswap.core

import com.coreswap.lib.bindings.LanguageIdentifier
import com.coreswap.lib.bindings.initNativeI18n
import com.coreswap.lib.bindings.initNativeLogging

object Native {
    private var isInitialized = false

    @Synchronized
    fun initialize() {
        if (!isInitialized) {
            System.loadLibrary("openscq30_android")
            initNativeLogging()
            isInitialized = true
        }
    }

    private var isI18nInitialized = false

    @Synchronized
    fun initializeI18n(languageIdentifiers: List<LanguageIdentifier>) {
        if (!isI18nInitialized) {
            initNativeI18n(languageIdentifiers)
            isI18nInitialized = true
        }
    }
}
