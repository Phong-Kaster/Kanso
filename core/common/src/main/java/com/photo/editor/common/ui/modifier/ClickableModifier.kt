package com.photo.editor.common.ui.modifier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay

fun Modifier.silentClickable(onClick: () -> Unit) = this.composed {
    Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

fun Modifier.rippleClickable(
    radius: Dp = Dp.Unspecified,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = this.composed {
    Modifier.clickable(
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = false, radius = radius),
        onClick = onClick
    )
}

/**
 * Clickable modifier that disable the component for an amount of time after it's clicked
 * @param delayTime The amount of time to wait before the component can be clicked again
 * @param initialDisabled Whether the component is initially disabled
 * @param onClick The action to perform when the component is clicked
 **/
fun Modifier.delayClickable(
    delayTime: Long = 1000L,
    initialDisabled: Boolean = false,
    onClick: () -> Unit
) = this.composed {
    var disabled by remember { mutableStateOf(initialDisabled) }

    LaunchedEffect(disabled) {
        if (disabled) {
            delay(delayTime)
            disabled = false
        }
    }

    Modifier.clickable(!disabled) {
        disabled = true
        onClick()
    }
}