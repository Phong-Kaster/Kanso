package com.phongkaster.kanso.tool.edit

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.ui.tool.component.toolpanel.EDIT_PANEL_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EDIT_BLUE_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EDIT_GREEN_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EDIT_RED_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EditPanelContent
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.editRgbChipTag
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.editSliderValueTag
import com.phongkaster.kanso.ui.tool.model.RgbChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks on Android 15 the Edit screen's RGB sheet, drawn by the stateless `EditPanelContent`
 * (plan `docs/plans/rgb-tool.md`, decisions 3, 4 and 8):
 *
 * - it holds three channel chips, Red, Green and Blue, and one slider, for the chosen chip; there
 *   is no Opacity slider;
 * - each channel's slider runs from -100 through 0 to +100 and starts at 0;
 * - choosing a chip changes only which slider shows, never the settings;
 * - a value set on one channel is still there after the sheet is drawn afresh;
 * - dragging calls `onPreview` (the photo follows, no Undo step yet) and letting go calls
 *   `onCommit` (the drag becomes one Undo step).
 *
 * The test holds the settings and the chosen channel itself in `mutableStateOf`s, the way the
 * ViewModel holds them for the real screen. No ViewModel, Koin or photo is involved.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class RgbSheetTest {

    /** Starts an empty activity to host the sheet. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The settings the sheet draws, held by the test as the ViewModel holds them in the app. */
    private var settings by mutableStateOf(EditSettings())

    /** RGB's chosen channel, held by the test as the ViewModel holds it in the app. */
    private var channel by mutableStateOf(RgbChannel.RED)

    /** Which sheet is drawn; switching it away and back draws the RGB sheet afresh. */
    private var openTool by mutableStateOf(EditTool.RGB)

    /** Every set of settings handed to `onPreview`, in order. */
    private val previews = mutableListOf<EditSettings>()

    /** Every set of settings handed to `onChange`, in order. */
    private val changes = mutableListOf<EditSettings>()

    /** How many times `onCommit` was called. */
    private var commits = 0

    /** The three channels, with the slider tag and the name beside the slider and on the circle's description. */
    private val channels = listOf(
        Triple(RgbChannel.RED, EDIT_RED_TAG, "Red"),
        Triple(RgbChannel.GREEN, EDIT_GREEN_TAG, "Green"),
        Triple(RgbChannel.BLUE, EDIT_BLUE_TAG, "Blue"),
    )

    /**
     * The sheet holds three colour circles with no text and exactly one slider. Tapping a circle
     * selects it and shows that channel's slider in place of the others, with its name beside it:
     * the name is shown once, by the slider, and the circle carries it only as its description for
     * screen readers. Choosing a circle never changes the settings.
     */
    @Test
    fun rgb_has_three_chips_and_one_slider_for_the_chosen_one() {
        show()
        channels.forEach(
            action = { (chosen, tag, name) ->
                choose(chosen)
                compose.onNodeWithTag(editRgbChipTag(chosen)).assertIsSelected()
                compose.onAllNodes(
                    hasAnyAncestor(hasTestTag(EDIT_PANEL_TAG)) and
                        SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
                ).assertCountEquals(1)
                compose.onNodeWithTag(tag).assertExists()
                channels.filter(predicate = { it.second != tag }).forEach(
                    action = { other -> compose.onNodeWithTag(other.second).assertDoesNotExist() },
                )
                compose.onAllNodesWithText(name).assertCountEquals(1)
                compose.onNodeWithTag(editRgbChipTag(chosen)).assertContentDescriptionEquals(name)
            },
        )
        compose.onAllNodesWithText("Opacity").assertCountEquals(0)
        assertEquals("choosing a chip changed the settings", EditSettings(), settings)
        assertTrue("choosing a chip made a change", changes.isEmpty() && previews.isEmpty())
    }

    /**
     * Each channel's slider runs from -100 through 0 to +100: its range is the settings' -1..1, it
     * starts at 0, and its number reads 0, then -100 and 100 at its ends, moving only its own
     * channel in the settings.
     */
    @Test
    fun each_slider_runs_from_minus_100_through_0_to_100() {
        show()
        channels.forEach(
            action = { (chosen, tag, _) ->
                choose(chosen)
                val info = rangeInfo(tag)
                assertEquals(EditSettings.MIN, info.range.start, 0f)
                assertEquals(EditSettings.MAX, info.range.endInclusive, 0f)
                assertEquals("$tag starts at the centre", 0f, info.current, 0f)
                compose.onNodeWithTag(editSliderValueTag(tag)).assertTextEquals("0")

                setProgress(tag, EditSettings.MIN)
                compose.onNodeWithTag(editSliderValueTag(tag)).assertTextEquals("-100")
                setProgress(tag, EditSettings.MAX)
                compose.onNodeWithTag(editSliderValueTag(tag)).assertTextEquals("100")
                assertEquals(EditSettings.MAX, valueOf(chosen), 0f)
                // Back to the middle, so the next channel starts from neutral settings.
                setProgress(tag, EditSettings.MIDDLE)
            },
        )
        assertEquals(EditSettings(), settings)
    }

    /**
     * Green set to -40 is still -40, on the slider and in its number, after the sheet is replaced by
     * the Filters sheet and RGB is drawn again from scratch.
     */
    @Test
    fun a_channel_keeps_its_value_when_rgb_is_opened_again() {
        show()
        choose(RgbChannel.GREEN)
        setProgress(EDIT_GREEN_TAG, -0.4f)

        compose.runOnIdle(action = { openTool = EditTool.FILTERS })
        compose.waitForIdle()
        compose.onNodeWithTag(EDIT_GREEN_TAG).assertDoesNotExist()
        compose.runOnIdle(action = { openTool = EditTool.RGB })
        compose.waitForIdle()

        compose.onNodeWithTag(editSliderValueTag(EDIT_GREEN_TAG)).assertTextEquals("-40")
        assertEquals(-0.4f, rangeInfo(EDIT_GREEN_TAG).current, 0.0001f)
        assertEquals(EditSettings(green = -0.4f), settings)
    }

    /**
     * Dragging the red slider with a finger calls `onPreview` while it moves and never `onCommit`;
     * lifting the finger calls `onCommit`. No `onChange` (a whole action) is made.
     */
    @Test
    fun dragging_previews_and_letting_go_commits() {
        show()
        val slider = compose.onNodeWithTag(EDIT_RED_TAG)
        slider.performTouchInput(
            block = {
                down(center)
                // Several small moves, so the drag passes the touch slop and then follows the finger.
                repeat(times = DRAG_STEPS, action = { moveBy(Offset(width * DRAG_STEP_FRACTION, 0f)) })
            },
        )
        compose.waitForIdle()
        assertTrue("dragging made no preview", previews.isNotEmpty())
        assertEquals("a commit came before the finger was lifted", 0, commits)
        assertTrue("the drag did not raise red", settings.red > 0.1f)

        slider.performTouchInput(block = { up() })
        compose.waitForIdle()
        assertTrue("letting go made no commit", commits >= 1)
        assertTrue("a slider is never an action", changes.isEmpty())
        assertEquals("the drag moved another setting", EditSettings(red = settings.red), settings)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Draws the sheet for [openTool] from [settings], drawn afresh (`key`) whenever the tool
     * changes, as the screen's sheet area does; every callback updates or records the test's
     * state.
     */
    private fun show() {
        compose.setContent(
            composable = {
                MaterialTheme(
                    content = {
                        key(
                            openTool,
                            block = {
                                EditPanelContent(
                                    openTool = openTool,
                                    settings = settings,
                                    onChange = { new ->
                                        changes += new
                                        settings = new
                                    },
                                    onPreview = { new ->
                                        previews += new
                                        settings = new
                                    },
                                    onCommit = { commits++ },
                                    onRatio = {},
                                    rgbChannel = channel,
                                    onRgbChannel = { channel = it },
                                )
                            },
                        )
                    },
                )
            },
        )
        compose.waitForIdle()
    }

    /** Taps [chosen]'s chip, so its slider is the one shown. */
    private fun choose(chosen: RgbChannel) {
        compose.onNodeWithTag(editRgbChipTag(chosen)).performClick()
        compose.waitForIdle()
    }

    /** The current value of [of]'s setting in the test's settings. */
    private fun valueOf(of: RgbChannel): Float = when (of) {
        RgbChannel.RED -> settings.red
        RgbChannel.GREEN -> settings.green
        RgbChannel.BLUE -> settings.blue
    }

    /** Moves the slider tagged [tag] to [value] through its accessibility action. */
    private fun setProgress(tag: String, value: Float) {
        compose.onNodeWithTag(tag)
            .performSemanticsAction(SemanticsActions.SetProgress, invocation = { setProgress -> setProgress(value) })
        compose.waitForIdle()
    }

    /** The range and current value the slider tagged [tag] reports. */
    private fun rangeInfo(tag: String): ProgressBarRangeInfo =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]

    /** How the finger moves in the drag test. */
    private companion object {
        /** The number of moves in one drag. */
        const val DRAG_STEPS = 5

        /** Each move's length as a fraction of the slider's width. */
        const val DRAG_STEP_FRACTION = 0.07f
    }
}
