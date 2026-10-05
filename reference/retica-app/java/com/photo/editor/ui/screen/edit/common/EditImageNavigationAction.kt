package com.photo.editor.ui.screen.edit.common

import com.photo.editor.editimage.model.ImageFrame
import com.photo.editor.editimage.model.filter.FilterPreset

sealed class EditImageNavigationAction {
    data object OpenMakeup : EditImageNavigationAction()

    data object OpenBeauty : EditImageNavigationAction()

    data object OpenFilter : EditImageNavigationAction()

    data object OpenSticker : EditImageNavigationAction()

    data object OpenText : EditImageNavigationAction()

    data object OpenAdjust : EditImageNavigationAction()

    data object OpenBrush : EditImageNavigationAction()

    data object OpenFrame : EditImageNavigationAction()

    data class UseFilter(val filter: FilterPreset) : EditImageNavigationAction()
    data class UseFrame(val frame: ImageFrame) : EditImageNavigationAction()
}