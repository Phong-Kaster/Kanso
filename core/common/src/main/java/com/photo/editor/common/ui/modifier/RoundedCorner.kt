package com.photo.editor.common.ui.modifier

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp

fun rounded(size: Dp): RoundedCornerShape = RoundedCornerShape(size)

fun Modifier.rounded(size: Dp): Modifier = clip(RoundedCornerShape(size))

fun Modifier.circle(): Modifier = clip(CircleShape)