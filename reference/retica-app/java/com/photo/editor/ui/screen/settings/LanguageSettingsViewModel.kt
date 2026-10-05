package com.photo.editor.ui.screen.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.photo.editor.data.repository.SettingsRepository
import com.photo.editor.domain.model.Language
import kotlinx.coroutines.runBlocking
import org.koin.android.annotation.KoinViewModel
import javax.inject.Inject

@KoinViewModel
class LanguageSettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    var selectedLanguage: Language by mutableStateOf(settingsRepository.getLanguage())

    fun onConfirmLanguage() {
        runBlocking { settingsRepository.setLanguage(selectedLanguage) }
    }
}