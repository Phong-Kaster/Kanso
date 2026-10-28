package com.phongkaster.kanso.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.library.colorpicker.HSVColorPicker
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/**
 * The colour of the surface the picker sits on. The picker draws its text in fixed black, so
 * it always gets a white surface, which keeps that text readable in dark mode too.
 *
 * This card is the one place that paints a colour not taken from the theme: the engine's picker
 * cannot be changed (its source is not Kanso's to edit), so its card has to match the picker
 * instead of the theme.
 */
private val PickerSurfaceColor = Color.White

/**
 * The engine's HSV colour picker (a colour square, a hue bar and a hex box), starting at the
 * colour [seedArgb], on a white surface so its fixed black text stays readable in light and dark
 * mode. Every change calls [onColorChanged]. Two screens use it: Settings for a custom theme
 * colour, and the Edit screen's Text sheet for any colour of a text's letters.
 */
@Composable
fun CustomColourPicker(seedArgb: Int, onColorChanged: (Color) -> Unit, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    // A Surface is a Material 3 container with its own background colour and shape. Its colour
    // is fixed white on purpose, not taken from the theme (see PickerSurfaceColor). No content
    // colour is set: the picker gives every piece of its own text an explicit colour and never
    // reads the surrounding one, so the white card is the only fixed colour this needs.
    Surface(
        color = PickerSurfaceColor,
        shape = MaterialTheme.shapes.largeIncreased,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            // Touching the picker while the hex box has the keyboard open: the first finger down
            // anywhere on the card takes the focus away from the box, which closes the keyboard,
            // so dragging the colour is never fought over by a typing session. It only watches
            // (the Initial pass sees the touch before the picker does) and consumes nothing, so
            // the picker still gets the touch.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed && !it.previousPressed }) focusManager.clearFocus()
                    }
                }
            },
        content = {
            // The picker remembers its own colour once it is shown, so [seedArgb] only sets where
            // it starts; it does not jump back while the person drags. showAlpha = false hides
            // the transparency bar and its percentage box: neither a theme colour nor a text's
            // letters take transparency here (a text has its own opacity slider).
            HSVColorPicker(
                color = Color(seedArgb),
                onColorChanged = onColorChanged,
                modifier = Modifier.padding(16.dp),
                showAlpha = false,
            )
        },
    )
}

/** The colour picker card on its own, starting at a deep green, in light and in dark. */
@PreviewLightDark
@Composable
private fun CustomColourPickerPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    content = { CustomColourPicker(seedArgb = 0xFF2E7D32.toInt(), onColorChanged = {}) },
                )
            },
        )
    }
}
