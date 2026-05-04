package com.trustedgelabs.trustguard.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleManager {

    private const val PREFS_NAME = "trustguard_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_TURKISH = "tr"
    const val LANGUAGE_ENGLISH = "en"

    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
    }

    fun setSelectedLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun applyLocale(context: Context): Context {
        val language = getSelectedLanguage(context)
        if (language == LANGUAGE_SYSTEM) return context

        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    fun getLocaleForLanguage(language: String): Locale {
        return when (language) {
            LANGUAGE_TURKISH -> Locale("tr")
            LANGUAGE_ENGLISH -> Locale("en")
            else -> Locale.getDefault()
        }
    }
}
