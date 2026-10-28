package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.TextAlignment
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the Text sheet's text area. */
const val TEXT_FIELD_TAG = "text-field"

/** Test tag of the Text sheet's alignment button. */
const val TEXT_ALIGNMENT_TAG = "text-alignment"

/** How many lines the text area shows before it scrolls. */
private const val VISIBLE_LINES = 3

/**
 * The Text sheet's top row: the text area holding the selected text's [words], and at its end the
 * alignment button showing [alignment] (a tap moves it on: centre, left, right).
 *
 * **The keyboard.** [editing] says whether the keyboard should be up: when it turns true the area
 * asks for the focus (which opens the keyboard), when it turns false the focus is cleared. The
 * other way round, the area reports every focus change through [onEditingChange], and a keyboard
 * closed by the system (its own hide key or Back) clears the focus too, so the sheet always
 * knows. Enter makes a new line; the text wraps by itself at its block's width on the photo.
 *
 * **Typing.** The area keeps the cursor in a `TextFieldValue` of its own and hands each new string
 * to [onWords]; the photo shows it at once. Its [key] (the selected text's key) starts a fresh
 * value, so selecting another text shows that text's words with the cursor at their end. A text
 * is cut at [LayerContent.Text.MAX_LENGTH] characters.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TextInputRow(
    key: Long?,
    words: String,
    alignment: TextAlignment,
    editing: Boolean,
    enabled: Boolean,
    onWords: (String) -> Unit,
    onEditingChange: (Boolean) -> Unit,
    onAlignment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember(key) { mutableStateOf(TextFieldValue(words, TextRange(words.length))) }
    // Words changed from OUTSIDE (an undo, a cancel) replace what the area shows. Only a change of
    // [words] itself counts: while the person types, the area is a moment ahead of [words], and
    // that must not throw the new letter away.
    var lastWords by remember(key) { mutableStateOf(words) }
    if (words != lastWords) {
        lastWords = words
        if (value.text != words) value = TextFieldValue(words, TextRange(words.length))
    }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val latestOnEditingChange by rememberUpdatedState(onEditingChange)
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(
        editing,
        block = {
            if (editing && !focused) focusRequester.requestFocus() else if (!editing && focused) focusManager.clearFocus()
        },
    )

    // A keyboard hidden by the system (its own hide key, Back) leaves the focus behind: clear it,
    // so the sheet brings its rows back. Only a keyboard that WAS seen counts, so the moment
    // before it slides in is not taken for a close.
    val imeVisible = WindowInsets.isImeVisible
    var imeSeen by remember { mutableStateOf(false) }
    LaunchedEffect(
        imeVisible,
        block = {
            if (imeVisible) {
                imeSeen = true
            } else if (imeSeen && focused) {
                imeSeen = false
                focusManager.clearFocus()
            }
        },
    )

    val description = stringResource(R.string.text_field_description)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            OutlinedTextField(
                value = value,
                onValueChange = { changed ->
                    val cut = if (changed.text.length > LayerContent.Text.MAX_LENGTH) {
                        changed.copy(text = changed.text.take(LayerContent.Text.MAX_LENGTH))
                    } else {
                        changed
                    }
                    value = cut
                    if (cut.text != words) onWords(cut.text)
                },
                enabled = enabled,
                placeholder = { Text(text = stringResource(R.string.text_hint)) },
                maxLines = VISIBLE_LINES,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused != focused) {
                            focused = state.isFocused
                            latestOnEditingChange(state.isFocused)
                        }
                    }
                    .testTag(TEXT_FIELD_TAG)
                    .semantics { contentDescription = description },
            )
            IconButton(
                onClick = onAlignment,
                enabled = enabled && key != null,
                modifier = Modifier.size(48.dp).testTag(TEXT_ALIGNMENT_TAG),
                content = {
                    Icon(
                        painter = painterResource(alignment.icon()),
                        contentDescription = stringResource(alignment.description()),
                    )
                },
            )
        },
    )
}

/** The icon showing how this alignment lines the text up. */
private fun TextAlignment.icon(): Int = when (this) {
    TextAlignment.CENTRE -> R.drawable.ic_format_align_center
    TextAlignment.LEFT -> R.drawable.ic_format_align_left
    TextAlignment.RIGHT -> R.drawable.ic_format_align_right
}

/** What a screen reader says for the button: the alignment now and what a tap does. */
private fun TextAlignment.description(): Int = when (this) {
    TextAlignment.CENTRE -> R.string.text_alignment_centre
    TextAlignment.LEFT -> R.string.text_alignment_left
    TextAlignment.RIGHT -> R.string.text_alignment_right
}

/** The row with two lines of words, centred, in light and in dark. */
@PreviewLightDark
@Composable
private fun TextInputRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                TextInputRow(
                    key = 1L,
                    words = "Happy birthday\nMinh Anh!",
                    alignment = TextAlignment.CENTRE,
                    editing = false,
                    enabled = true,
                    onWords = {},
                    onEditingChange = {},
                    onAlignment = {},
                )
            },
        )
    }
}

/** The row with no text yet, showing its hint, in light and in dark. */
@PreviewLightDark
@Composable
private fun TextInputRowEmptyPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                TextInputRow(
                    key = 1L,
                    words = "",
                    alignment = TextAlignment.LEFT,
                    editing = false,
                    enabled = true,
                    onWords = {},
                    onEditingChange = {},
                    onAlignment = {},
                )
            },
        )
    }
}
