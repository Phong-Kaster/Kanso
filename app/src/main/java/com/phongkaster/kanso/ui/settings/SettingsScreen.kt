package com.phongkaster.kanso.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.library.colorpicker.HSVColorPicker
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.data.settings.isWallpaperColourAvailable
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.theme.seedColorScheme

/** The test tag on the root of the Settings screen, so tests can tell it apart from Home. */
const val SETTINGS_SCREEN_TAG = "settings-screen"

/** The test tag on the "System default" theme mode row. */
const val THEME_MODE_SYSTEM_TAG = "theme-mode-system"

/** The test tag on the "Light" theme mode row. */
const val THEME_MODE_LIGHT_TAG = "theme-mode-light"

/** The test tag on the "Dark" theme mode row. */
const val THEME_MODE_DARK_TAG = "theme-mode-dark"

/** The test tag on the Kanso blue theme colour row (first in the list, on every Android version). */
const val THEME_COLOUR_KANSO_BLUE_TAG = "theme-colour-kanso-blue"

/** The test tag on the wallpaper theme colour row (Android 12 and later only). */
const val THEME_COLOUR_WALLPAPER_TAG = "theme-colour-wallpaper"

/** The test tag on the custom theme colour row. */
const val THEME_COLOUR_CUSTOM_TAG = "theme-colour-custom"

/** The test tag on the node holding the colour picker, shown while Custom is selected. */
const val CUSTOM_COLOUR_PICKER_TAG = "custom-colour-picker"

/** The test tag on the rounded container that holds the three theme mode rows. */
const val SETTINGS_GROUP_APPEARANCE_TAG = "settings-group-appearance"

/** The test tag on the rounded container that holds the theme colour rows. */
const val SETTINGS_GROUP_COLOUR_TAG = "settings-group-colour"

/** The test tag on the swatch of the Kanso blue row: a circle of Kanso blue's light primary. */
const val THEME_COLOUR_KANSO_BLUE_SWATCH_TAG = "theme-colour-kanso-blue-swatch"

/** The test tag on the swatch of the wallpaper row: a circle of the wallpaper's light primary. */
const val THEME_COLOUR_WALLPAPER_SWATCH_TAG = "theme-colour-wallpaper-swatch"

/** The test tag on the swatch of the custom row: a circle of the custom seed colour. */
const val THEME_COLOUR_CUSTOM_SWATCH_TAG = "theme-colour-custom-swatch"

/**
 * Returns the test tag of the icon at the start of the row tagged [rowTag], such as
 * `theme-mode-dark-icon` for the "Dark" row.
 *
 * The row merges everything inside it into one node for screen readers (see
 * [SettingsChoiceRow]), so a test finds the icon in the unmerged tree, where every node keeps
 * its own place, by this tag.
 */
fun settingsRowIconTag(rowTag: String): String = "$rowTag-icon"

/**
 * Returns the test tag of the tinted rounded square that holds the icon of the row tagged
 * [rowTag], such as `theme-mode-dark-icon-badge` for the "Dark" row. Like the icon, it is found
 * in the unmerged tree.
 */
fun settingsRowIconBadgeTag(rowTag: String): String = "$rowTag-icon-badge"

/**
 * Returns the test tag of the one-line description under the title of the row tagged [rowTag],
 * such as `theme-mode-dark-description` for the "Dark" row. Like the icon, it is found in the
 * unmerged tree.
 */
fun settingsRowDescriptionTag(rowTag: String): String = "$rowTag-description"

/**
 * The colour of the surface the picker sits on. The picker draws its text in fixed black, so
 * it always gets a white surface, which keeps that text readable in dark mode too.
 *
 * This card is the one place in Settings that paints a colour not taken from the theme: the
 * engine's picker cannot be changed (its source is not Kanso's to edit), so its card has to
 * match the picker instead of the theme.
 */
private val PickerSurfaceColor = Color.White

/** The space between the screen's left and right edges and the grouped containers. */
private val SCREEN_PADDING = 16.dp

/**
 * Where the small "Theme mode" and "Theme colour" labels start: in line with the icons of the
 * rows below them (the container's edge, its own padding and the row's padding add up to this).
 */
private val GROUP_LABEL_START = 32.dp

/** The space between a grouped container's edge and the rows inside it. */
private val GROUP_PADDING = 4.dp

/** The space between the left and right edges of a row and its icon and radio button. */
private val ROW_PADDING = 12.dp

/** The smallest height of a row: 56 dp, comfortably above Android's 48 dp touch target. */
private val ROW_MIN_HEIGHT = 56.dp

/** The space above and below a row's title and description, inside the row. */
private val ROW_VERTICAL_PADDING = 8.dp

/** The side of the tinted rounded square that holds a row's icon. */
private val BADGE_SIZE = 40.dp

/** The size of a colour swatch: big enough to read the colour at a glance. */
private val SWATCH_SIZE = 28.dp

/**
 * The space above the large "Settings" title: 8 dp, the same as above Home's "Kanso" title, so the
 * two titles sit at the same height and a tab change does not make the title jump.
 */
private val TITLE_TOP_PADDING = 8.dp

/**
 * The Settings screen: one scrolling column that starts with a large "Settings" title, then the
 * "Appearance" section, where the user picks the theme mode and the theme colour. Each setting
 * is a rounded group of rows, and each colour row shows a small circle of its colour.
 *
 * Settings is one of the app's three tabs, reached from the floating tab bar the navigation host
 * draws over it, so it has no back arrow of its own: the system Back gesture or button takes the
 * person Home, and the bar takes them to any other tab.
 *
 * Like Home, this screen is split in two (state hoisting; see `HomeScreen` for the full story).
 * This function is the *stateful* part: it reads Settings' state (the saved theme mode and theme
 * colour, one [SettingsUiState]) from [viewModel], asks the phone for its wallpaper colour
 * (Android 12 and later), and hands all of that, with the ViewModel's functions as callbacks, to
 * the stateless [SettingsContent], which does all the drawing.
 *
 * What is specific here: the wallpaper colour is a fact about the phone, not a saved choice, so
 * it is gathered here and passed down as a plain colour. `LocalContext.current` is the Android
 * context the screen is drawn in, through which the system's colours are read, and
 * `dynamicLightColorScheme(context)` is the light colour scheme Android builds from the
 * wallpaper; its `primary` is the colour the wallpaper row's swatch shows. A phone older than
 * Android 12 has no wallpaper colours, so it gets null, which leaves the wallpaper row out. A
 * preview can pass any colour, or null, whatever Android version it is drawn on.
 *
 * The [viewModel] is passed in by the navigation entry, which owns it. [contentPadding] is the
 * extra space the caller asks for at the end of the list: the navigation host passes the height
 * the floating tab bar covers, so the last row can be scrolled up above the bar.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    // collectAsState reads the ViewModel's StateFlow as Compose state: when the saved mode or
    // colour changes, a new snapshot arrives and this screen re-draws with the new selection.
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    // The wallpaper colours exist only from Android 12 (API 31); older phones get null and are
    // not offered the wallpaper row. The check also tells Android's lint the call below is safe.
    val wallpaperColour = if (isWallpaperColourAvailable()) {
        dynamicLightColorScheme(context).primary
    } else {
        null
    }

    SettingsContent(
        state = state,
        wallpaperColour = wallpaperColour,
        onThemeModeSelected = viewModel::setThemeMode,
        onKansoBlueChosen = viewModel::onKansoBlueChosen,
        onWallpaperChosen = viewModel::onWallpaperChosen,
        onCustomChosen = viewModel::onCustomChosen,
        onCustomColourChosen = viewModel::onCustomColourChosen,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

/**
 * The stateless part of Settings (see [SettingsScreen] for why there are two): draws the large
 * "Settings" title and the "Appearance" section, with the theme mode and theme colour of [state]
 * marked as chosen.
 *
 * The section holds two rounded groups. The first holds the three theme mode rows; the second
 * the colour rows, in this order: Kanso blue, then From wallpaper when [wallpaperColour] is not
 * null, then Custom colour. Each colour row shows a swatch of its colour; the wallpaper row's is
 * [wallpaperColour], the light `primary` of the phone's wallpaper scheme. It is a separate
 * parameter, not part of [state], because it is a fact about the phone rather than a saved
 * choice; null means the phone has no wallpaper colours (below Android 12).
 *
 * The title is the first item of the scrolling column, not a top bar: Settings is a tab, so it
 * has no back arrow to put in one, and like Home's title it scrolls away with the rest. It uses
 * `headlineLargeEmphasized`, one of the bolder "Emphasized" text styles of Material 3 Expressive
 * (see `HomeTitle` for the idea), with 8 dp above it and 16 dp at each side: exactly Home's
 * title, so moving between the two tabs keeps the title in one place. With no top bar, the [Scaffold] adds the status
 * bar's height to `innerPadding` instead, so the title never sits under the clock.
 *
 * A plain scrolling Column holds the section: the list is short, so it is drawn all at once, and
 * every row exists for tests and screen readers even when it is scrolled out of view.
 *
 * Every tap is reported through a callback and nothing is saved here: [onThemeModeSelected] for a
 * mode row, [onKansoBlueChosen], [onWallpaperChosen] and [onCustomChosen] for the colour rows,
 * and [onCustomColourChosen] for each colour picked in the colour picker. [contentPadding]'s
 * bottom is added after the last row, inside the scrolling part, so the person can scroll the
 * last row up above the floating tab bar.
 */
@Composable
fun SettingsContent(
    state: SettingsUiState,
    wallpaperColour: Color?,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onKansoBlueChosen: () -> Unit,
    onWallpaperChosen: () -> Unit,
    onCustomChosen: () -> Unit,
    onCustomColourChosen: (Color) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Scaffold(
        modifier = modifier.testTag(SETTINGS_SCREEN_TAG),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // innerPadding is the space the status bar and the navigation bar take (there is
                // no top bar); consumeWindowInsets marks it as dealt with, so the inset padding
                // below only adds what is still missing.
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                // Empty space after the last row, so scrolled to its end the list comes to rest
                // above the navigation bar and the bottom gesture zone, whichever is taller (the
                // same rule as Home's column; see HomeContent).
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
                )
                // The caller's extra space at the end (the floating tab bar's height), inside the
                // scrolling content, so the last row can be scrolled up above the bar.
                .padding(bottom = contentPadding.calculateBottomPadding())
                // A bottom margin keeps the screen's bottom-left corner plain background.
                .padding(bottom = 24.dp),
        ) {
            SettingsTitle()
            SectionHeader(text = stringResource(R.string.appearance))
            ThemeModeChoice(
                selected = state.themeMode,
                onSelect = onThemeModeSelected,
            )
            ThemeColourChoice(
                selected = state.themeColour,
                customSeedArgb = state.customSeedArgb,
                wallpaperColour = wallpaperColour,
                onKansoBlue = onKansoBlueChosen,
                onWallpaper = onWallpaperChosen,
                onCustom = onCustomChosen,
                onCustomColourChanged = onCustomColourChosen,
            )
        }
    }
}

/**
 * Settings' large title, the word "Settings", as the first item of the scrolling column.
 *
 * It is drawn in the same style and with the same padding as Home's title (see [SettingsContent]):
 * `headlineLargeEmphasized`, 8 dp above, 16 dp at each side and nothing below, since the next
 * line (Home's greeting, here the "Appearance" header) brings its own space.
 * `heading()` marks it as a heading, so a screen reader announces it as one and lets the person
 * jump to it. The same word is also the label of the Settings tab on the floating bar, so the
 * text "Settings" is on screen twice; tests tell the two apart by the tab's test tag.
 */
@Composable
private fun SettingsTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.settings),
        style = MaterialTheme.typography.headlineLargeEmphasized,
        modifier = modifier
            .padding(start = SCREEN_PADDING, end = SCREEN_PADDING, top = TITLE_TOP_PADDING)
            .semantics { heading() },
    )
}

/**
 * The "Theme mode" setting: a small label, then a rounded group (tagged
 * [SETTINGS_GROUP_APPEARANCE_TAG]) with one row per [ThemeMode], each with its own icon, and
 * [selected] marked. Tapping a row calls [onSelect] with that row's mode.
 */
@Composable
private fun ThemeModeChoice(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    GroupLabel(text = stringResource(R.string.theme_mode))
    SettingsGroup(testTag = SETTINGS_GROUP_APPEARANCE_TAG) {
        ThemeMode.entries.forEach { mode ->
            SettingsChoiceRow(
                label = stringResource(mode.labelRes()),
                description = stringResource(mode.descriptionRes()),
                iconRes = mode.iconRes(),
                selected = mode == selected,
                onClick = { onSelect(mode) },
                testTag = mode.testTag(),
            )
        }
    }
}

/**
 * The theme colour setting: a small label, then a rounded group (tagged
 * [SETTINGS_GROUP_COLOUR_TAG]) holding, in this order, a Kanso blue row (on every Android
 * version), a wallpaper row when [wallpaperColour] is not null, and a custom colour row, with
 * [selected] marked. Each row ends with a swatch of its own colour:
 *
 * - Kanso blue: the light `primary` of the Kanso blue scheme, the blue the app's buttons show in
 *   light mode;
 * - From wallpaper: [wallpaperColour], the light `primary` the phone builds from its wallpaper;
 * - Custom colour: the last custom colour picked, whatever is selected. While Custom is
 *   selected that is its own seed; while another choice is selected it is [customSeedArgb], the
 *   seed the settings file keeps meanwhile, which a tap on Custom brings back. So the swatch
 *   always shows what Custom gives; for a person who has never picked a colour it is Kanso
 *   blue's seed, where the picker starts.
 *
 * Each swatch shows the light version of its choice in both themes, so the same choice always
 * shows the same circle. While Custom is selected, the colour picker is shown below the group,
 * and every colour picked there goes to [onCustomColourChanged]. Tapping a row calls
 * [onKansoBlue], [onWallpaper] or [onCustom].
 */
@Composable
private fun ThemeColourChoice(
    selected: ThemeColour,
    customSeedArgb: Int,
    wallpaperColour: Color?,
    onKansoBlue: () -> Unit,
    onWallpaper: () -> Unit,
    onCustom: () -> Unit,
    onCustomColourChanged: (Color) -> Unit,
) {
    // remember keeps a value across re-draws: generating a scheme takes real work, and Kanso
    // blue never changes, so its primary is worked out once.
    val kansoBlue = remember { seedColorScheme(KANSO_BLUE_SEED, dark = false).primary }
    // The last picked custom colour: Custom's own seed while it is selected (the same stored seed
    // customSeedArgb holds), else customSeedArgb, which the file keeps while another is selected.
    val customColour = Color(if (selected is ThemeColour.Custom) selected.seedArgb else customSeedArgb)

    GroupLabel(
        // stringResource looks the text up in the app's string resources (res/values/strings.xml,
        // strings_theme.xml and their translations).
        text = stringResource(R.string.theme_colour_title),
        modifier = Modifier.padding(top = 16.dp),
    )
    SettingsGroup(testTag = SETTINGS_GROUP_COLOUR_TAG) {
        // Kanso's own colours come first: they are what a fresh install shows.
        SettingsChoiceRow(
            label = stringResource(R.string.theme_colour_kanso_blue),
            description = stringResource(R.string.settings_rows_kanso_blue_description),
            iconRes = R.drawable.ic_palette,
            selected = selected == ThemeColour.KansoBlue,
            onClick = onKansoBlue,
            testTag = THEME_COLOUR_KANSO_BLUE_TAG,
            trailing = { ColourSwatch(colour = kansoBlue, testTag = THEME_COLOUR_KANSO_BLUE_SWATCH_TAG) },
        )
        if (wallpaperColour != null) {
            SettingsChoiceRow(
                label = stringResource(R.string.theme_colour_wallpaper),
                description = stringResource(R.string.settings_rows_wallpaper_description),
                // A picture icon: these colours come from the phone's wallpaper picture.
                iconRes = R.drawable.ic_photo_library,
                selected = selected == ThemeColour.Wallpaper,
                onClick = onWallpaper,
                testTag = THEME_COLOUR_WALLPAPER_TAG,
                trailing = {
                    ColourSwatch(colour = wallpaperColour, testTag = THEME_COLOUR_WALLPAPER_SWATCH_TAG)
                },
            )
        }
        SettingsChoiceRow(
            label = stringResource(R.string.theme_colour_custom),
            description = stringResource(R.string.settings_rows_custom_description),
            // A brush icon: here the person paints with a colour of their own.
            iconRes = R.drawable.ic_brush,
            selected = selected is ThemeColour.Custom,
            onClick = onCustom,
            testTag = THEME_COLOUR_CUSTOM_TAG,
            trailing = { ColourSwatch(colour = customColour, testTag = THEME_COLOUR_CUSTOM_SWATCH_TAG) },
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
    // is fixed white on purpose, not taken from the theme (see PickerSurfaceColor). No content
    // colour is set: the picker gives every piece of its own text an explicit colour and never
    // reads the surrounding one, so the white card is the only fixed colour this screen needs.
    Surface(
        color = PickerSurfaceColor,
        shape = MaterialTheme.shapes.largeIncreased,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SCREEN_PADDING, vertical = 8.dp)
            .testTag(CUSTOM_COLOUR_PICKER_TAG),
    ) {
        // The picker remembers its own colour once it is shown, so [seedArgb] only sets where it
        // starts; it does not jump back while the person drags. showAlpha = false hides the
        // transparency bar and its percentage box: a theme colour is always fully opaque.
        HSVColorPicker(
            color = Color(seedArgb),
            onColorChanged = onColorChanged,
            modifier = Modifier.padding(16.dp),
            showAlpha = false,
        )
    }
}

/**
 * A section title, such as "Appearance", drawn in the theme's primary colour and the emphasized
 * title style. `heading()` marks it as a heading for screen readers, so TalkBack can jump to it.
 */
@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = SCREEN_PADDING, end = SCREEN_PADDING, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

/**
 * The small label above a group of rows, such as "Theme mode", in the theme's softer text
 * colour and lined up with the icons of the rows below it.
 */
@Composable
private fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(
            start = GROUP_LABEL_START,
            end = SCREEN_PADDING,
            top = 8.dp,
            bottom = 8.dp,
        ),
    )
}

/**
 * A grouped container: one rounded block, tagged [testTag], holding the rows of one setting
 * ([content]) one under the other, the way the phone's own Settings app groups its rows.
 *
 * It is a `Surface`, a Material container that draws its colour in its shape: the theme's
 * `surfaceContainerHigh`, a surface colour two steps apart from the screen's background, with the
 * theme's `extraLarge` shape (28 dp corners), so the card reads as a card. (The plainer
 * `surfaceContainer` is so close to the light background, about 5 in each of red, green and blue
 * out of 255, that the card would barely show.) It keeps 16 dp from the screen's edges.
 *
 * `selectableGroup` tells screen readers the rows inside are one set of choices, of which
 * exactly one is picked, like a group of radio buttons on a web form. The group itself is not
 * tappable; its rows are.
 */
@Composable
private fun SettingsGroup(
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SCREEN_PADDING)
            .testTag(testTag),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .selectableGroup()
                .padding(GROUP_PADDING),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

/**
 * One choice inside a [SettingsGroup]: the icon [iconRes] in a tinted rounded square (see
 * [SettingsIconBadge]), the [label] with a one-line [description] under it, an optional
 * [trailing] element (a colour row's swatch), and a radio button at the end that [selected]
 * fills. Tapping anywhere on the row calls [onClick]. The row is tagged [testTag]; its square,
 * its icon and its description are tagged [settingsRowIconBadgeTag], [settingsRowIconTag] and
 * [settingsRowDescriptionTag] of it.
 *
 * `selectable` makes the whole row tappable and reports it to screen readers as a radio button
 * (`Role.RadioButton`) that is selected or not, so the row, not the small circle, is the control.
 * Like any tappable node it merges what is inside it into one node, which TalkBack reads as, for
 * example, "Dark, Always dark, radio button, selected". The icon is decorative (no
 * content description), because the label already names the row.
 *
 * The description is held to one line (`maxLines = 1`): a row stays one even height, and a text
 * too long for a narrow phone ends in "…" instead of pushing the row taller.
 *
 * [trailing] is a slot: a parameter that takes a piece of composable content instead of a plain
 * value, so the caller decides what goes there (a swatch for the colour rows, nothing for the
 * theme mode rows) while the row keeps its own layout.
 *
 * The chosen row is tinted with the theme's `secondaryContainer`, a soft tone of the theme
 * colour, and its text takes the matching `onSecondaryContainer`, so it stays readable; a row not
 * chosen has the group's own `surfaceContainerHigh`, so it does not show as a block of its own.
 * `animateColorAsState` turns a jump between the two tints into a short fade: it returns a
 * colour that moves towards each new target over a few frames, here with the motion scheme's
 * `defaultEffectsSpec`, the spring Material 3 Expressive uses for colour changes. The row is
 * clipped to rounded corners first, so the tint and the ripple a tap draws are rounded too.
 */
@Composable
private fun SettingsChoiceRow(
    label: String,
    description: String,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colours = MaterialTheme.colorScheme
    val containerColour by animateColorAsState(
        targetValue = if (selected) colours.secondaryContainer else colours.surfaceContainerHigh,
        // Read in composition: MaterialTheme.motionScheme is only available while composing.
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>(),
        label = "settingsRowContainer",
    )
    val textColour = if (selected) colours.onSecondaryContainer else colours.onSurface
    val descriptionColour = if (selected) colours.onSecondaryContainer else colours.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .clip(MaterialTheme.shapes.large)
            .background(containerColour)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag(testTag)
            .padding(horizontal = ROW_PADDING, vertical = ROW_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(iconRes = iconRes, rowTag = testTag)
        Spacer(Modifier.width(16.dp))
        // weight(1f) gives the title and description all the width left over, so a long title
        // wraps instead of pushing the swatch and the radio button off the row.
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = textColour,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = descriptionColour,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(settingsRowDescriptionTag(testTag)),
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
        Spacer(Modifier.width(12.dp))
        // onClick = null: the row above handles taps, so the button only draws the state and
        // screen readers announce one control instead of two.
        RadioButton(selected = selected, onClick = null)
    }
}

/**
 * The icon at the start of a row, drawn in a small tinted rounded square: a [BADGE_SIZE] square
 * with the theme's `medium` rounded corners (12 dp), filled with `tertiaryContainer`, with the
 * icon [iconRes] in the matching `onTertiaryContainer` in its middle. The square is tagged
 * [settingsRowIconBadgeTag] of [rowTag] and the icon [settingsRowIconTag] of it.
 *
 * Why `tertiaryContainer`: the tertiary colours are the theme's accent, a hue set apart from the
 * main one, so the square stands out both on the grey-blue card and on the chosen row, which is
 * tinted with the blue `secondaryContainer`. A blue square would all but vanish on that row.
 *
 * The drawable paints white; `Icon` tints it with the colour given, so it never draws white on
 * a light square.
 */
@Composable
private fun SettingsIconBadge(@DrawableRes iconRes: Int, rowTag: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(BADGE_SIZE)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .testTag(settingsRowIconBadgeTag(rowTag)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.testTag(settingsRowIconTag(rowTag)),
        )
    }
}

/**
 * A colour swatch: a filled circle of [colour], tagged [testTag], that shows a colour choice's
 * colour before it is picked.
 *
 * The thin ring on its edge, in the theme's `outline` colour, keeps a pale colour visible on the
 * row's own tint; the ring is drawn inside the circle's edge only, so the middle of the circle is
 * exactly [colour]. The swatch is decorative for screen readers: the row's label names the
 * choice.
 */
@Composable
private fun ColourSwatch(colour: Color, testTag: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(SWATCH_SIZE)
            .testTag(testTag)
            .background(color = colour, shape = CircleShape)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = CircleShape),
    )
}

/** Returns the string resource naming this theme mode on its row. */
@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.theme_mode_system
    ThemeMode.Light -> R.string.theme_mode_light
    ThemeMode.Dark -> R.string.theme_mode_dark
}

/** Returns the string resource of the one-line description under this theme mode's title. */
@StringRes
private fun ThemeMode.descriptionRes(): Int = when (this) {
    ThemeMode.System -> R.string.settings_rows_system_description
    ThemeMode.Light -> R.string.settings_rows_light_description
    ThemeMode.Dark -> R.string.settings_rows_dark_description
}

/**
 * Returns the icon at the start of this theme mode's row: a gear for "System default" (it
 * follows the phone's own setting), the half-filled circle of the contrast symbol for "Light",
 * and a moon for "Dark".
 */
@DrawableRes
private fun ThemeMode.iconRes(): Int = when (this) {
    ThemeMode.System -> R.drawable.ic_settings
    ThemeMode.Light -> R.drawable.ic_contrast
    ThemeMode.Dark -> R.drawable.ic_dark_mode
}

/** Returns the test tag of this theme mode's row. */
private fun ThemeMode.testTag(): String = when (this) {
    ThemeMode.System -> THEME_MODE_SYSTEM_TAG
    ThemeMode.Light -> THEME_MODE_LIGHT_TAG
    ThemeMode.Dark -> THEME_MODE_DARK_TAG
}

/**
 * A custom theme colour for the previews: a deep green, `#2E7D32`, written as an ARGB integer
 * (the leading `FF` means fully opaque, as every theme colour is).
 */
private val sampleCustomColour = ThemeColour.Custom(seedArgb = 0xFF2E7D32.toInt())

/**
 * A wallpaper colour for the previews: a warm amber, `#8B5000`, the kind of light primary a
 * sunset wallpaper gives. The real one comes from the phone, which a preview has no access to.
 */
private val sampleWallpaperColour = Color(0xFF8B5000)

/**
 * Settings as a fresh install shows it on a phone running Android 12 or later: "System default"
 * chosen as the theme mode and Kanso blue chosen as the colour, with the wallpaper row (in the
 * sample amber) offered below it but not picked, drawn once in light and once in dark.
 *
 * `@PreviewLightDark` gives two pictures, day and night; `ThemeMode.System` makes KansoTheme
 * follow them (see `HomePreview`). The taps do nothing: a preview only draws.
 */
@PreviewLightDark
@Composable
private fun SettingsSystemModePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SettingsContent(
            state = SettingsUiState(
                themeMode = ThemeMode.System,
                themeColour = ThemeColour.KansoBlue,
            ),
            wallpaperColour = sampleWallpaperColour,
            onThemeModeSelected = {},
            onKansoBlueChosen = {},
            onWallpaperChosen = {},
            onCustomChosen = {},
            onCustomColourChosen = {},
        )
    }
}

/**
 * Settings with "Light" chosen as the theme mode, on an older phone (below Android 12) where the
 * colour list has no wallpaper row, only Kanso blue (the one chosen) and Custom colour.
 *
 * Only the selection moves: the preview itself still follows its own light or dark picture,
 * so both pictures show the "Light" row marked.
 */
@PreviewLightDark
@Composable
private fun SettingsLightModePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SettingsContent(
            state = SettingsUiState(
                themeMode = ThemeMode.Light,
                themeColour = ThemeColour.KansoBlue,
            ),
            wallpaperColour = null,
            onThemeModeSelected = {},
            onKansoBlueChosen = {},
            onWallpaperChosen = {},
            onCustomChosen = {},
            onCustomColourChosen = {},
        )
    }
}

/**
 * Settings with "Dark" chosen as the theme mode and the wallpaper colour chosen, drawn once in
 * light and once in dark so the marked "Dark" row can be checked against both backgrounds.
 */
@PreviewLightDark
@Composable
private fun SettingsDarkModePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SettingsContent(
            state = SettingsUiState(
                themeMode = ThemeMode.Dark,
                themeColour = ThemeColour.Wallpaper,
            ),
            wallpaperColour = sampleWallpaperColour,
            onThemeModeSelected = {},
            onKansoBlueChosen = {},
            onWallpaperChosen = {},
            onCustomChosen = {},
            onCustomColourChosen = {},
        )
    }
}

/**
 * Settings with the wallpaper colour chosen (Android 12 and later), the colour choice this
 * preview is about; the theme mode is left at "System default".
 */
@PreviewLightDark
@Composable
private fun SettingsWallpaperColourPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SettingsContent(
            state = SettingsUiState(
                themeMode = ThemeMode.System,
                themeColour = ThemeColour.Wallpaper,
            ),
            wallpaperColour = sampleWallpaperColour,
            onThemeModeSelected = {},
            onKansoBlueChosen = {},
            onWallpaperChosen = {},
            onCustomChosen = {},
            onCustomColourChosen = {},
        )
    }
}

/**
 * Settings with the Custom colour row chosen, so the colour picker is shown below it, starting
 * at the sample green [sampleCustomColour], and the custom swatch shows that green. The theme is
 * tinted with that same green, as the real app is once a custom colour is picked. The picker
 * sits on its fixed white card in both pictures, which is how its black text stays readable in
 * dark mode.
 */
@PreviewLightDark
@Composable
private fun SettingsCustomColourPreview() {
    KansoTheme(themeMode = ThemeMode.System, themeColour = sampleCustomColour) {
        SettingsContent(
            state = SettingsUiState(
                themeMode = ThemeMode.System,
                themeColour = sampleCustomColour,
            ),
            wallpaperColour = sampleWallpaperColour,
            onThemeModeSelected = {},
            onKansoBlueChosen = {},
            onWallpaperChosen = {},
            onCustomChosen = {},
            onCustomColourChosen = {},
        )
    }
}

/**
 * The theme mode group on its own, with "Dark" chosen: the "Theme mode" label, then three rows,
 * each with its icon in a tinted rounded square, its title, its one-line description and its
 * radio button, on the rounded card, the chosen row tinted. Drawn once in light
 * and once in dark on the screen's background colour.
 */
@PreviewLightDark
@Composable
private fun SettingsGroupPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                ThemeModeChoice(selected = ThemeMode.Dark, onSelect = {})
            }
        }
    }
}

/**
 * Two colour rows in a group, each ending with its swatch: From wallpaper, in the sample amber
 * [sampleWallpaperColour], not chosen, and Custom colour, in the sample green
 * [sampleCustomColour], chosen and tinted. Drawn once in light and once in dark, so the swatches
 * and their rings can be checked on both tints of the row.
 */
@PreviewLightDark
@Composable
private fun ColourRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                SettingsGroup(testTag = SETTINGS_GROUP_COLOUR_TAG) {
                    SettingsChoiceRow(
                        label = stringResource(R.string.theme_colour_wallpaper),
                        description = stringResource(R.string.settings_rows_wallpaper_description),
                        iconRes = R.drawable.ic_photo_library,
                        selected = false,
                        onClick = {},
                        testTag = THEME_COLOUR_WALLPAPER_TAG,
                        trailing = {
                            ColourSwatch(
                                colour = sampleWallpaperColour,
                                testTag = THEME_COLOUR_WALLPAPER_SWATCH_TAG,
                            )
                        },
                    )
                    SettingsChoiceRow(
                        label = stringResource(R.string.theme_colour_custom),
                        description = stringResource(R.string.settings_rows_custom_description),
                        iconRes = R.drawable.ic_brush,
                        selected = true,
                        onClick = {},
                        testTag = THEME_COLOUR_CUSTOM_TAG,
                        trailing = {
                            ColourSwatch(
                                colour = Color(sampleCustomColour.seedArgb),
                                testTag = THEME_COLOUR_CUSTOM_SWATCH_TAG,
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * The tinted rounded squares on their own, one per kind of row icon (gear, contrast, moon,
 * palette, picture, brush), side by side on the card's colour, drawn once in light and once in
 * dark, so the square's tint and its icon can be checked against both.
 */
@PreviewLightDark
@Composable
private fun SettingsIconBadgePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(
                    R.drawable.ic_settings,
                    R.drawable.ic_contrast,
                    R.drawable.ic_dark_mode,
                    R.drawable.ic_palette,
                    R.drawable.ic_photo_library,
                    R.drawable.ic_brush,
                ).forEachIndexed { index, icon ->
                    SettingsIconBadge(iconRes = icon, rowTag = "sample-row-$index")
                }
            }
        }
    }
}
