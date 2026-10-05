package com.photo.editor.ui.screen.onboard

import com.photo.editor.R

enum class OnboardWelcomeItem(
    val imageId: Int,
    val titleId: Int,
) {
    Retro(
        R.drawable.welcome_retro,
        R.string.welcome_retro,
    ),
    Cinematic(
        R.drawable.welcome_cinematic,
        R.string.welcome_cinematic,
    ),
    Aesthetic(
        R.drawable.welcome_aesthetic,
        R.string.welcome_aesthetic,
    ),
    Dreamy(
        R.drawable.welcome_dreamy,
        R.string.welcome_dreamy,
    ),
    Natural(
        R.drawable.welcome_natural,
        R.string.welcome_natural,
    ),
    Infrared(
        R.drawable.welcome_infrared,
        R.string.welcome_infrared,
    ),
}