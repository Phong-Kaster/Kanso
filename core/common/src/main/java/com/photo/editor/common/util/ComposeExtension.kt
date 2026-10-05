package com.photo.editor.common.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

@Composable
fun Dp.toPx(): Float = with(LocalDensity.current){ toPx() }

fun Dp.toPx(density: Density): Float = with(density){ toPx() }

@Composable
fun Dp.roundToPx(): Int = with(LocalDensity.current){ roundToPx() }