package com.photo.editor.data.datastore

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.photo.editor.domain.model.Language
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single

@Single
class SettingsDatastore(
    context: Context,
) {
    companion object {
        private const val PREF_NAME = "settings"

        private val onboardedKey = booleanPreferencesKey("onboarded")
        private val isUserRatedKey = booleanPreferencesKey("isUserRated")
        private val experimentalFeature1Key = booleanPreferencesKey("experimentalFeature1")

        private val homePromotionVisitedKey = booleanPreferencesKey("homePromotionVisited")
        private val stickerPromotionShownKey = booleanPreferencesKey("stickerPromotionShown")
        private val filterPromotionShownKey = booleanPreferencesKey("filterPromotionShown")
        private val removeBgPromotionShownKey = booleanPreferencesKey("removeBgPromotionShown")
    }

    private val Context.datastore: DataStore<Preferences> by preferencesDataStore(name = PREF_NAME)
    private val datastore = context.datastore

    var language: Language
        get() {
            val applicationLocales = AppCompatDelegate.getApplicationLocales()
            val selectedLangCode =
                run { applicationLocales[0] ?: LocaleListCompat.getDefault()[0]!! }.language
            val selectedLanguage = Language.getByCode(selectedLangCode)
            return selectedLanguage
        }
        set(value) {
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(value.code)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }

    var onboarded: Boolean
        get() = runBlocking { datastore.data.first()[onboardedKey] ?: false }
        set(value) = runBlocking { datastore.edit { pref -> pref[onboardedKey] = value } }

    var isUserRated: Boolean
        get() = runBlocking { datastore.data.first()[isUserRatedKey] ?: false }
        set(value) = runBlocking { datastore.edit { pref -> pref[isUserRatedKey] = value } }

    var experimentalFeature1: Boolean
        get() = runBlocking { datastore.data.first()[experimentalFeature1Key] ?: false }
        set(value) = runBlocking {
            datastore.edit { pref ->
                pref[experimentalFeature1Key] = value
            }
        }

    var homePromotionVisited: Boolean
        get() = runBlocking { datastore.data.first()[homePromotionVisitedKey] ?: false }
        set(value) = runBlocking {
            datastore.edit { pref ->
                pref[homePromotionVisitedKey] = value
            }
        }

    var stickerPromotionShown: Boolean
        get() = runBlocking { datastore.data.first()[stickerPromotionShownKey] ?: false }
        set(value) = runBlocking {
            datastore.edit { pref ->
                pref[stickerPromotionShownKey] = value
            }
        }

    var filterPromotionShown: Boolean
        get() = runBlocking { datastore.data.first()[filterPromotionShownKey] ?: false }
        set(value) = runBlocking {
            datastore.edit { pref ->
                pref[filterPromotionShownKey] = value
            }
        }

    var removeBgPromotionShown: Boolean
        get() = runBlocking { datastore.data.first()[removeBgPromotionShownKey] ?: false }
        set(value) = runBlocking {
            datastore.edit { pref ->
                pref[removeBgPromotionShownKey] = value
            }
        }
}