package com.phongkaster.kanso.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag on the root of the Text size screen. */
const val TEXT_SIZE_SCREEN_TAG = "text-size-screen"

/**
 * The Text size setting on a screen of its own, opened from the Settings menu: the slider, the
 * chosen percentage and a sample sentence. The size is saved when the finger lifts, and the whole
 * app changes at once; Back returns to the menu.
 *
 * This is the stateful half (see `HomeScreen` for the split): it reads the saved choices from
 * [viewModel] and hands them to [TextSizeContent]. [onBack] is the Back button.
 */
@Composable
fun TextSizeScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    TextSizeContent(
        state = state,
        onTextScaleChosen = viewModel::setTextScale,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * The stateless part of the Text size screen: the framed screen (see [SettingsDetailScreen])
 * holding [TextSizeChoice] at the size of [state]; [onTextScaleChosen] gets the new size when the
 * slider is let go.
 */
@Composable
fun TextSizeContent(
    state: SettingsUiState,
    onTextScaleChosen: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsDetailScreen(
        title = stringResource(R.string.text_size),
        onBack = onBack,
        testTag = TEXT_SIZE_SCREEN_TAG,
        modifier = modifier,
    ) {
        TextSizeChoice(savedScale = state.textScale, onScaleChosen = onTextScaleChosen)
    }
}

/** The Text size screen at 115%, in light and in dark. */
@PreviewLightDark
@Composable
private fun TextSizeContentPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.background) {
                TextSizeContent(
                    state = SettingsUiState(
                        themeMode = ThemeMode.System,
                        themeColour = ThemeColour.KansoBlue,
                        textScale = 1.15f,
                    ),
                    onTextScaleChosen = {},
                    onBack = {},
                )
            }
        }
    }
}
