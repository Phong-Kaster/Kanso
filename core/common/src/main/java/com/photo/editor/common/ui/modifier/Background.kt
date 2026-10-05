package com.photo.editor.common.ui.modifier

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

fun Modifier.backgroundColor(colorValue: Long): Modifier = this.background(Color(colorValue))