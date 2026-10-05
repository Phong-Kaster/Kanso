package com.photo.editor.ui.screen.onboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.photo.editor.data.repository.SettingsRepository
import com.photo.editor.domain.model.Language
import org.koin.android.annotation.KoinViewModel
import javax.inject.Inject

@KoinViewModel
class OnboardViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    var selectedLanguage: Language? by mutableStateOf(null)
    var selectedHindiLanguage: Language? by mutableStateOf(null)

    var selectedWelcomeIndices = mutableStateListOf<OnboardWelcomeItem>()

    fun confirmLanguage() {
        selectedLanguage?.let {
            settingsRepository.setLanguage(it)
        }
    }

    fun confirmHindiLanguage() {
        selectedHindiLanguage?.let {
            settingsRepository.setLanguage(it)
        }
    }

    fun setOnboarded() {
        settingsRepository.setOnboarded()
    }
}