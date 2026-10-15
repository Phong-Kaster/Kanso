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
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.ui.tool.EDIT_BRIGHTNESS_TAG
import com.phongkaster.kanso.ui.tool.EDIT_CONTRAST_TAG
import com.phongkaster.kanso.ui.tool.EDIT_PANEL_TAG
import com.phongkaster.kanso.ui.tool.EDIT_SATURATION_TAG
import com.phongkaster.kanso.ui.tool.EDIT_WARMTH_TAG
import com.phongkaster.kanso.ui.tool.EditPanelContent
import com.phongkaster.kanso.ui.tool.editSliderValueTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

/**
 * Checks on Android 15 the Edit screen's Adjust sheet, drawn by the stateless `EditPanelContent`
 * (DoD 5, 15, R2):
 *
 * - it holds four sliders, Brightness, Contrast, Saturation and Warmth, each running from -100
 *   through 0 to +100, starting at 0 in the centre, with its number right beside its name;
 * - brightness set to 70 is still 70 after the sheet is drawn afresh (another sheet opened, then
 *   Adjust again), because the sheet keeps no state of its own;
 * - a slider let go stays where it was let go (no spring back to the middle);
 * - dragging calls `onPreview` (the photo follows, no Undo step yet) and letting go calls
 *   `onCommit` (the drag becomes one Undo step).
 *
 * The test holds the settings itself in a `mutableStateOf`, the way the ViewModel holds them for
 * the real screen: every `onPreview` or `onChange` replaces them, and the sheet redraws from them.
 * No ViewModel, Koin or photo is involved.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class AdjustSheetTest {

    /** Starts an empty activity to host the sheet. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The settings the sheet draws, held by the test as the ViewModel holds them in the app. */
    private var settings by mutableStateOf(EditSettings())

    /** Which sheet is drawn; switching it away and back draws the Adjust sheet afresh. */
    private var openTool by mutableStateOf(EditTool.ADJUST)

    /** Every set of settings handed to `onPreview`, in order. */
    private val previews = mutableListOf<EditSettings>()

    /** Every set of settings handed to `onChange`, in order. */
    private val changes = mutableListOf<EditSettings>()

    /** How many times `onCommit` was called. */
    private var commits = 0

    /** The four sliders, by their tag, with the name shown beside each. */
    private val sliders = listOf(
        EDIT_BRIGHTNESS_TAG to "Brightness",
        EDIT_CONTRAST_TAG to "Contrast",
        EDIT_SATURATION_TAG to "Saturation",
        EDIT_WARMTH_TAG to "Warmth",
    )

    /** The sheet holds exactly four sliders, each named, each tagged as the screen expects. */
    @Test
    fun adjust_has_four_named_sliders() {
        show()
        compose.onAllNodes(
            hasAnyAncestor(hasTestTag(EDIT_PANEL_TAG)) and
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
        ).assertCountEquals(4)
        sliders.forEach { (tag, name) ->
            compose.onNodeWithTag(tag).assertExists()
            compose.onAllNodesWithText(name).assertCountEquals(1)
        }
    }

    /**
     * Each slider runs from -100 through 0 to +100: its range is the settings' -1..1, it starts at
     * 0, which is the middle of that range, and its number reads 0, then -100 and 100 at its ends.
     */
    @Test
    fun each_slider_runs_from_minus_100_through_0_to_100() {
        show()
        sliders.forEach { (tag, _) ->
            val info = rangeInfo(tag)
            assertEquals(EditSettings.MIN, info.range.start, 0f)
            assertEquals(EditSettings.MAX, info.range.endInclusive, 0f)
            assertEquals("$tag starts at the centre", 0f, info.current, 0f)
            assertEquals(0f, (info.range.start + info.range.endInclusive) / 2f, 0f)
            compose.onNodeWithTag(editSliderValueTag(tag)).assertTextEquals("0")

            setProgress(tag, EditSettings.MIN)
            compose.onNodeWithTag(editSliderValueTag(tag)).assertTextEquals("-100")
            setProgress(tag, EditSettings.MAX)
            compose.onNodeWithTag(editSliderValueTag(tag)).assertTextEquals("100")
        }
    }

    /**
     * Each slider's number sits right beside its name, on the same line: to the name's right,
     * before the slider's track, and vertically centred with it.
     */
    @Test
    fun each_number_is_shown_beside_its_name() {
        show()
        sliders.forEach { (tag, name) ->
            val label = compose.onAllNodesWithText(name)[0].fetchSemanticsNode()
            val number = compose.onNodeWithTag(editSliderValueTag(tag)).fetchSemanticsNode()
            val slider = compose.onNodeWithTag(tag).fetchSemanticsNode()
            assertTrue("$name: the number overlaps its name", left(number) >= right(label) - 1f)
            assertTrue("$name: the number reaches into the slider", right(number) <= left(slider) + SLIDER_TARGET_SLACK)
            val labelMiddle = middleY(label)
            assertTrue(
                "$name: the number is not on the name's line",
                labelMiddle >= top(number) && labelMiddle <= bottom(number),
            )
        }
    }

    /**
     * DoD 5: brightness set to 70 is still 70, on the slider and in its number, after the sheet
     * is replaced by the Filters sheet and Adjust is drawn again from scratch.
     */
    @Test
    fun brightness_70_is_still_70_when_adjust_is_opened_again() {
        show()
        setProgress(EDIT_BRIGHTNESS_TAG, 0.7f)
        compose.onNodeWithTag(editSliderValueTag(EDIT_BRIGHTNESS_TAG)).assertTextEquals("70")

        compose.runOnIdle { openTool = EditTool.FILTERS }
        compose.waitForIdle()
        compose.onNodeWithTag(EDIT_BRIGHTNESS_TAG).assertDoesNotExist()
        compose.runOnIdle { openTool = EditTool.ADJUST }
        compose.waitForIdle()

        compose.onNodeWithTag(editSliderValueTag(EDIT_BRIGHTNESS_TAG)).assertTextEquals("70")
        assertEquals(0.7f, rangeInfo(EDIT_BRIGHTNESS_TAG).current, 0.0001f)
    }

    /**
     * R2: a slider let go stays where it was let go, also a second later; it never springs back
     * to the middle.
     */
    @Test
    fun a_released_slider_does_not_spring_back() {
        show()
        setProgress(EDIT_WARMTH_TAG, -0.4f)
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        assertEquals(-0.4f, rangeInfo(EDIT_WARMTH_TAG).current, 0.0001f)
        compose.onNodeWithTag(editSliderValueTag(EDIT_WARMTH_TAG)).assertTextEquals("-40")
        assertEquals(-0.4f, settings.warmth, 0.0001f)
    }

    /**
     * Dragging the brightness slider with a finger calls `onPreview` while it moves and never
     * `onCommit`; lifting the finger calls `onCommit`. Afterwards the slider and its number stay
     * at the value the finger left, and no `onChange` (a whole action) was made.
     */
    @Test
    fun dragging_previews_and_letting_go_commits() {
        show()
        val slider = compose.onNodeWithTag(EDIT_BRIGHTNESS_TAG)
        slider.performTouchInput {
            down(center)
            // Several small moves, so the drag passes the touch slop and then follows the finger.
            repeat(DRAG_STEPS) { moveBy(Offset(width * DRAG_STEP_FRACTION, 0f)) }
        }
        compose.waitForIdle()
        assertTrue("dragging made no preview", previews.isNotEmpty())
        assertEquals("a commit came before the finger was lifted", 0, commits)
        assertTrue("the drag did not raise brightness", settings.brightness > 0.1f)

        slider.performTouchInput { up() }
        compose.waitForIdle()
        assertTrue("letting go made no commit", commits >= 1)
        assertTrue("a slider is never an action", changes.isEmpty())

        val released = settings.brightness
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        assertTrue("the slider sprang back", released > 0.1f)
        assertEquals(released, rangeInfo(EDIT_BRIGHTNESS_TAG).current, 0.0001f)
        compose.onNodeWithTag(editSliderValueTag(EDIT_BRIGHTNESS_TAG))
            .assertTextEquals("${(released * 100).roundToInt()}")
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Draws the sheet for [openTool] from [settings], drawn afresh (`key`) whenever the tool
     * changes, as the screen's sheet area does; every callback updates or records the test's
     * state.
     */
    private fun show() {
        compose.setContent {
            MaterialTheme {
                key(openTool) {
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
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    /** Moves the slider tagged [tag] to [value] through its accessibility action. */
    private fun setProgress(tag: String, value: Float) {
        compose.onNodeWithTag(tag)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(value) }
        compose.waitForIdle()
    }

    /** The range and current value the slider tagged [tag] reports. */
    private fun rangeInfo(tag: String): ProgressBarRangeInfo =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]

    /** The left edge of [node], in root pixels. */
    private fun left(node: SemanticsNode): Float = node.positionInRoot.x

    /** The right edge of [node], in root pixels. */
    private fun right(node: SemanticsNode): Float = node.positionInRoot.x + node.size.width

    /** The top edge of [node], in root pixels. */
    private fun top(node: SemanticsNode): Float = node.positionInRoot.y

    /** The bottom edge of [node], in root pixels. */
    private fun bottom(node: SemanticsNode): Float = node.positionInRoot.y + node.size.height

    /** The vertical middle of [node], in root pixels. */
    private fun middleY(node: SemanticsNode): Float = node.positionInRoot.y + node.size.height / 2f

    /** How the finger moves in the drag test. */
    private companion object {
        /** The number of moves in one drag. */
        const val DRAG_STEPS = 5

        /** Each move's length as a fraction of the slider's width. */
        const val DRAG_STEP_FRACTION = 0.07f

        /**
         * How far, in pixels, a slider's semantics bounds may reach left of its track: Material
         * grows a slider's bounds to its touch target, which the number's own padding sits beside.
         */
        const val SLIDER_TARGET_SLACK = 16f
    }
}
