package com.phongkaster.kanso.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.library.colorpicker.HSVColorPicker
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.data.settings.isWallpaperColourAvailable
import com.phongkaster.kanso.ui.placeholder.BackArrowIcon

/** The test tag on the root of the Settings screen, so tests can tell it apart from Home. */
const val SETTINGS_SCREEN_TAG = "settings-screen"

/** The test tag on the "System default" theme mode row. */
const val THEME_MODE_SYSTEM_TAG = "theme-mode-system"

/** The test tag on the "Light" theme mode row. */
const val THEME_MODE_LIGHT_TAG = "theme-mode-light"

/** The test tag on the "Dark" theme mode row. */
const val THEME_MODE_DARK_TAG = "theme-mode-dark"

/** The test tag on the "From wallpaper" theme colour row (Android 12 and later only). */
const val THEME_COLOUR_WALLPAPER_TAG = "theme-colour-wallpaper"

/** The test tag on the "Default purple" theme colour row (below Android 12 only). */
const val THEME_COLOUR_BASELINE_TAG = "theme-colour-baseline"

/** The test tag on the "Custom colour" theme colour row. */
const val THEME_COLOUR_CUSTOM_TAG = "theme-colour-custom"

/** The test tag on the node holding the colour picker, shown while Custom is selected. */
const val CUSTOM_COLOUR_PICKER_TAG = "custom-colour-picker"

/**
 * The labels of the theme colour setting. They are kept here until they move to
 * `res/values/strings.xml` beside the other Settings labels.
 */
private object ThemeColourLabels {
    /** The setting's own label. */
    const val TITLE = "Theme colour"

    /** The wallpaper option. */
    const val WALLPAPER = "From wallpaper"

    /** The baseline option shown instead of the wallpaper one on older phones. */
    const val BASELINE = "Default purple"

    /** The option that opens the colour picker. */
    const val CUSTOM = "Custom colour"
}

/**
 * The colour of the surface the picker sits on. The picker draws its text in fixed black, so
 * it always gets a white surface, which keeps that text readable in dark mode too.
 */
private val PickerSurfaceColor = Color.White

/**
 * The Settings screen: a top bar titled "Settings" with a back arrow, and below it the
 * "Appearance" section, where the user picks the theme mode and the theme colour.
 *
 * [onBack] is called when the user taps the back arrow; the caller pops the back stack.
 * The [viewModel] is passed in by the navigation entry, which owns it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // collectAsState reads the ViewModel's StateFlow as Compose state: when the saved mode
    // changes, this screen re-draws with the new selection.
    val themeMode by viewModel.themeMode.collectAsState()
    val themeColour by viewModel.themeColour.collectAsState()

    Scaffold(
        modifier = modifier.testTag(SETTINGS_SCREEN_TAG),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = BackArrowIcon,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        // A plain scrolling Column: the settings list is short, so it is drawn all at once.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(text = stringResource(R.string.appearance))
            ThemeModeChoice(
                selected = themeMode,
                onSelect = viewModel::setThemeMode,
            )
            ThemeColourChoice(
                selected = themeColour,
                // The wallpaper colours exist only from Android 12 (API 31); older phones get
                // the baseline option in its place.
                wallpaperAvailable = isWallpaperColourAvailable(),
                onWallpaper = viewModel::onWallpaperChosen,
                onBaseline = viewModel::onBaselineChosen,
                onCustom = viewModel::onCustomChosen,
                onCustomColourChanged = viewModel::onCustomColourChosen,
            )
        }
    }
}

/**
 * The "Theme colour" setting: a label, a "From wallpaper" row when [wallpaperAvailable] (or a
 * "Default purple" row when it is not), and a "Custom colour" row, with [selected] marked.
 * While Custom is selected, the colour picker is shown below it, and every colour picked there
 * goes to [onCustomColourChanged]. Tapping a row calls [onWallpaper], [onBaseline] or [onCustom].
 */
@Composable
private fun ThemeColourChoice(
    selected: ThemeColour,
    wallpaperAvailable: Boolean,
    onWallpaper: () -> Unit,
    onBaseline: () -> Unit,
    onCustom: () -> Unit,
    onCustomColourChanged: (Color) -> Unit,
) {
    Text(
        text = ThemeColourLabels.TITLE,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
    Column(modifier = Modifier.selectableGroup()) {
        if (wallpaperAvailable) {
            RadioRow(
                label = ThemeColourLabels.WALLPAPER,
                selected = selected == ThemeColour.Wallpaper,
                onClick = onWallpaper,
                testTag = THEME_COLOUR_WALLPAPER_TAG,
            )
        } else {
            RadioRow(
                label = ThemeColourLabels.BASELINE,
                selected = selected == ThemeColour.Baseline,
                onClick = onBaseline,
                testTag = THEME_COLOUR_BASELINE_TAG,
            )
        }
        RadioRow(
            label = ThemeColourLabels.CUSTOM,
            selected = selected is ThemeColour.Custom,
            onClick = onCustom,
            testTag = THEME_COLOUR_CUSTOM_TAG,
        )
    }
    if (selected is ThemeColour.Custom) {
        CustomColourPicker(
            seedArgb = selected.seedArgb,
            onColorChanged = onCustomColourChanged,
        )
    }
}

/**
 * The engine's HSV colour picker, starting at the colour [seedArgb], on a white surface so its
 * fixed black text stays readable in light and dark mode. Every change calls [onColorChanged].
 */
@Composable
private fun CustomColourPicker(seedArgb: Int, onColorChanged: (Color) -> Unit) {
    // A Surface is a Material 3 container with its own background colour and shape. Its colour
    // is fixed white on purpose, not taken from the theme (see PickerSurfaceColor).
    Surface(
        color = PickerSurfaceColor,
        contentColor = Color.Black,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(CUSTOM_COLOUR_PICKER_TAG),
    ) {
        // The picker remembers its own colour once it is shown, so [seedArgb] only sets where it
        // starts; it does not jump back while the person drags.
        HSVColorPicker(
            color = Color(seedArgb),
            onColorChanged = onColorChanged,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** A section title, such as "Appearance", drawn small in the theme's primary colour. */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

/**
 * The "Theme mode" setting: a label and one radio row per [ThemeMode], with [selected] marked.
 * Tapping a row calls [onSelect] with that row's mode.
 */
@Composable
private fun ThemeModeChoice(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Text(
        text = stringResource(R.string.theme_mode),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    // selectableGroup tells accessibility services the rows below are one set of choices, of
    // which exactly one is picked (like a group of radio buttons on a web form).
    Column(modifier = Modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            RadioRow(
                label = stringResource(mode.labelRes()),
                selected = mode == selected,
                onClick = { onSelect(mode) },
                testTag = mode.testTag(),
            )
        }
    }
}

/**
 * One tappable row holding a radio button and its [label]; [selected] fills the button, and
 * tapping anywhere on the row calls [onClick].
 */
@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit, testTag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            // selectable makes the whole row tappable and reports it to accessibility services
            // as a radio button (Role.RadioButton) that is selected or not, so the row, not the
            // small circle, is the control.
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag(testTag)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // onClick = null: the row above handles taps, so the button only draws the state and
        // screen readers announce one control instead of two.
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Returns the string resource naming this theme mode on its radio row. */
@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.theme_mode_system
    ThemeMode.Light -> R.string.theme_mode_light
    ThemeMode.Dark -> R.string.theme_mode_dark
}

/** Returns the test tag of this theme mode's radio row. */
private fun ThemeMode.testTag(): String = when (this) {
    ThemeMode.System -> THEME_MODE_SYSTEM_TAG
    ThemeMode.Light -> THEME_MODE_LIGHT_TAG
    ThemeMode.Dark -> THEME_MODE_DARK_TAG
}
