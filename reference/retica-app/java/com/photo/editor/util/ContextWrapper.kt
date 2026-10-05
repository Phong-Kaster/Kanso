package com.photo.editor.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.util.Log
import com.photo.editor.domain.model.Language
import com.photo.editor.data.repository.SettingsRepository
import org.koin.mp.KoinPlatformTools
import java.util.Locale

class ContextWrapper(private val base: Context) {
    companion object {
        private const val TAG = "ContextWrapper"
    }

    @SuppressLint("ObsoleteSdkInt")
    fun withLocale(locale: Locale): Context {
        // Set locale settings
        val config = Configuration()
        // Set new locale language
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            config.setLocale(locale)
        }
        return base.createConfigurationContext(config)
    }

    fun withLanguage(language: Language): Context {
        return withLocale(Locale.forLanguageTag(language.code))
    }

    fun withAppLanguage(): Context {
        // Get shared pref from entry point injection
        val settingsRepository: SettingsRepository = KoinPlatformTools.defaultContext().get().get()
        // Get saved language
        val currentLanguage = settingsRepository.getLanguage()
        Log.d(TAG, "withAppLanguage: $currentLanguage")
        // Initialize locale
        val locale: Locale = Locale.forLanguageTag(currentLanguage.code)
        Locale.setDefault(locale)
        // Wrap context with new locale
        return withLocale(locale)
    }
}

fun Context.getLocale(): Locale = resources.configuration.locales[0]