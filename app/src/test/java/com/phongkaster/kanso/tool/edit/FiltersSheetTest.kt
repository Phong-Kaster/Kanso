package com.phongkaster.kanso.tool.edit

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.ui.tool.EDIT_INTENSITY_TAG
import com.phongkaster.kanso.ui.tool.EDIT_PANEL_TAG
import com.phongkaster.kanso.ui.tool.EditPanelContent
import com.phongkaster.kanso.ui.tool.editFilterTag
import com.phongkaster.kanso.ui.tool.editSliderValueTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks on Android 15 the Edit screen's Filters sheet, drawn by the stateless `EditPanelContent`
 * (DoD 16, R1, R2):
 *
 * - it offers None and the seven presets as chips, exactly one of them selected at a time, None
 *   at first;
 * - choosing a second filter replaces the first (filters no longer stack), each choice being one
 *   action (`onChange`);
 * - the first filter chosen from None starts at intensity 100; moving from one filter to another
 *   keeps the intensity set;
 * - the intensity runs from 0 to 100, with its number beside its name; it is unusable while None
 *   is chosen; dragging it calls `onPreview` (so the photo changes while dragged), letting go
 *   calls `onCommit`, and it stays where it was let go.
 *
 * The test holds the settings itself in a `mutableStateOf`, as the ViewModel holds them for the
 * real screen, so the chips and slider draw what the callbacks last set.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class FiltersSheetTest {

    /** Starts an empty activity to host the sheet. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The settings the sheet draws, held by the test as the ViewModel holds them in the app. */
    private var settings by mutableStateOf(EditSettings())

    /** Every set of settings handed to `onPreview`, in order. */
    private val previews = mutableListOf<EditSettings>()

    /** Every set of settings handed to `onChange`, in order. */
    private val changes = mutableListOf<EditSettings>()

    /** How many times `onCommit` was called. */
    private var commits = 0

    /** The sheet offers eight chips: None first, then the seven presets; None is selected at first. */
    @Test
    fun filters_offers_none_and_seven_presets() {
        show()
        assertEquals(FilterPreset.NONE, FilterPreset.entries.first())
        assertEquals(8, FilterPreset.entries.size)
        FilterPreset.entries.forEach { preset -> compose.onNodeWithTag(editFilterTag(preset)).assertExists() }
        compose.onAllNodesWithText("None").assertCountEquals(1)
        compose.onNodeWithTag(editFilterTag(FilterPreset.NONE)).assertIsSelected()
        assertSelectedCount(1)
    }

    /**
     * R1: choosing Kelvin and then Hudson leaves Hudson alone selected and alone in the settings:
     * each choice is one action, and the second replaced the first.
     */
    @Test
    fun choosing_a_second_filter_replaces_the_first() {
        show()
        choose(FilterPreset.KELVIN)
        compose.onNodeWithTag(editFilterTag(FilterPreset.KELVIN)).assertIsSelected()
        compose.onNodeWithTag(editFilterTag(FilterPreset.NONE)).assertIsNotSelected()
        assertSelectedCount(1)

        choose(FilterPreset.HUDSON)
        compose.onNodeWithTag(editFilterTag(FilterPreset.HUDSON)).assertIsSelected()
        compose.onNodeWithTag(editFilterTag(FilterPreset.KELVIN)).assertIsNotSelected()
        assertSelectedCount(1)
        assertEquals(FilterPreset.HUDSON, settings.filter)
        assertEquals(listOf(FilterPreset.KELVIN, FilterPreset.HUDSON), changes.map { it.filter })

        choose(FilterPreset.NONE)
        compose.onNodeWithTag(editFilterTag(FilterPreset.NONE)).assertIsSelected()
        assertSelectedCount(1)
        assertEquals(FilterPreset.NONE, settings.filter)
    }

    /**
     * The first filter chosen from None shows at full strength, 100, whatever intensity was left
     * before; moving on to another filter keeps the intensity the person set (here 50).
     */
    @Test
    fun the_first_filter_from_none_starts_at_100() {
        settings = EditSettings(filterIntensity = 0.3f)
        show()
        choose(FilterPreset.KELVIN)
        assertEquals(1f, settings.filterIntensity, 0f)
        compose.onNodeWithTag(editSliderValueTag(EDIT_INTENSITY_TAG)).assertTextEquals("100")

        setIntensity(0.5f)
        choose(FilterPreset.AMARO)
        assertEquals(0.5f, settings.filterIntensity, 0.0001f)
        compose.onNodeWithTag(editSliderValueTag(EDIT_INTENSITY_TAG)).assertTextEquals("50")
    }

    /**
     * The intensity runs from 0 to 100 with its number beside its name "Intensity", reads 0 and
     * 100 at its ends, and cannot be used while None is chosen.
     */
    @Test
    fun the_intensity_runs_from_0_to_100() {
        show()
        compose.onNodeWithTag(EDIT_INTENSITY_TAG).assertIsNotEnabled()
        compose.onAllNodesWithText("Intensity").assertCountEquals(1)

        choose(FilterPreset.WALDEN)
        compose.onNodeWithTag(EDIT_INTENSITY_TAG).assertIsEnabled()
        val info = rangeInfo()
        assertEquals(0f, info.range.start, 0f)
        assertEquals(1f, info.range.endInclusive, 0f)
        setIntensity(0f)
        compose.onNodeWithTag(editSliderValueTag(EDIT_INTENSITY_TAG)).assertTextEquals("0")
        setIntensity(1f)
        compose.onNodeWithTag(editSliderValueTag(EDIT_INTENSITY_TAG)).assertTextEquals("100")
    }

    /**
     * DoD 16 and R2: dragging the intensity calls `onPreview` with the chosen filter at the new
     * intensity while the finger moves (the screen renders each one, so the photo changes while
     * dragged), never `onCommit`; lifting the finger calls `onCommit`, and the slider stays there.
     */
    @Test
    fun dragging_the_intensity_previews_and_letting_go_commits() {
        show()
        choose(FilterPreset.KELVIN)
        val slider = compose.onNodeWithTag(EDIT_INTENSITY_TAG)
        slider.performTouchInput {
            // Intensity 100 puts the thumb at the right end; the drag starts in the middle and
            // moves left, so the value falls well below 100.
            down(center)
            repeat(DRAG_STEPS) { moveBy(Offset(-width * DRAG_STEP_FRACTION, 0f)) }
        }
        compose.waitForIdle()
        assertTrue("dragging made no preview", previews.isNotEmpty())
        assertTrue("a preview lost the filter", previews.all { it.filter == FilterPreset.KELVIN })
        assertTrue("the drag did not lower the intensity", settings.filterIntensity < 0.9f)
        assertEquals("a commit came before the finger was lifted", 0, commits)

        slider.performTouchInput { up() }
        compose.waitForIdle()
        assertTrue("letting go made no commit", commits >= 1)
        val released = settings.filterIntensity
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        assertEquals(released, rangeInfo().current, 0.0001f)
        assertEquals("only the chip was an action", 1, changes.size)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Draws the Filters sheet from [settings]; every callback updates or records the test's state. */
    private fun show() {
        compose.setContent {
            MaterialTheme {
                EditPanelContent(
                    openTool = EditTool.FILTERS,
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
        compose.waitForIdle()
    }

    /**
     * Taps [preset]'s chip. The row scrolls sideways, so a chip may be off screen; the click is
     * sent through its semantics action, as a screen reader would.
     */
    private fun choose(preset: FilterPreset) {
        compose.onNodeWithTag(editFilterTag(preset)).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    /** Moves the intensity slider to [value] (0..1) through its accessibility action. */
    private fun setIntensity(value: Float) {
        compose.onNodeWithTag(EDIT_INTENSITY_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(value) }
        compose.waitForIdle()
    }

    /** Asserts that exactly [count] nodes of the sheet are selected (the chips are the only ones). */
    private fun assertSelectedCount(count: Int) {
        compose.onAllNodes(hasAnyAncestor(hasTestTag(EDIT_PANEL_TAG)) and isSelected()).assertCountEquals(count)
    }

    /** The range and current value the intensity slider reports. */
    private fun rangeInfo(): ProgressBarRangeInfo =
        compose.onNodeWithTag(EDIT_INTENSITY_TAG).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]

    /** How the finger moves in the drag test. */
    private companion object {
        /** The number of moves in one drag. */
        const val DRAG_STEPS = 5

        /** Each move's length as a fraction of the slider's width. */
        const val DRAG_STEP_FRACTION = 0.07f
    }
}
