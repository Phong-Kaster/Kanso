package com.photo.editor.ui.screen.retouch.edit.component.toucher

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset

@Stable
class RetouchTouchState {
    val points = mutableStateListOf<Offset>()

    val pointsUndone = mutableStateListOf<Offset>()

    fun reset() {
        points.clear()
        pointsUndone.clear()
    }
}