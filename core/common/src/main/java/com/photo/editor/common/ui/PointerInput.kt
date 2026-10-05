package com.photo.editor.common.ui

import androidx.compose.ui.input.pointer.PointerEvent

fun PointerEvent.consumeAll() = changes.forEach { it.consume() }