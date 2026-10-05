package com.photo.editor.data.repository

import com.photo.editor.data.datastore.SettingsDatastore
import com.photo.editor.domain.model.Language
import org.koin.core.annotation.Single

@Single
class SettingsRepository(
    private val settingsDatastore: SettingsDatastore,
) {
    fun getLanguage(): Language = settingsDatastore.language

    fun setLanguage(language: Language) {
        settingsDatastore.language = language
    }

    fun isOnboarded(): Boolean = settingsDatastore.onboarded

    fun setOnboarded() {
        settingsDatastore.onboarded = true
    }

    var experimentalFeature1: Boolean
        get() = settingsDatastore.experimentalFeature1
        set(value) {
            settingsDatastore.experimentalFeature1 = value
        }

    fun getPromotionDialogIndex(): Int {
        if (!settingsDatastore.homePromotionVisited) {
            settingsDatastore.homePromotionVisited = true
            return 0
        }

        if (!stickerPromotionShown) return 1
        if (!filterPromotionShown) return 2
        if (!removeBgPromotionShown) return 3
        return Int.MAX_VALUE
    }

    var stickerPromotionShown: Boolean
        get() = settingsDatastore.stickerPromotionShown
        set(value) {
            settingsDatastore.stickerPromotionShown = value
        }

    var filterPromotionShown: Boolean
        get() = settingsDatastore.filterPromotionShown
        set(value) {
            settingsDatastore.filterPromotionShown = value
        }

    var removeBgPromotionShown: Boolean
        get() = settingsDatastore.removeBgPromotionShown
        set(value) {
            settingsDatastore.removeBgPromotionShown = value
        }
}