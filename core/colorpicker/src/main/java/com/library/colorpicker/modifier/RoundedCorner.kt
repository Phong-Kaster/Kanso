package com.library.colorpicker.modifier

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp

internal fun Modifier.rounded(size: Dp): Modifier = clip(RoundedCornerShape(size))

internal fun Modifier.circle(): Modifier = clip(CircleShape)