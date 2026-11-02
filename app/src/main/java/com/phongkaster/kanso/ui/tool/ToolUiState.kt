package com.phongkaster.kanso.ui.tool

import android.graphics.Bitmap
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.LivePreview
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.data.edit.face.FaceFocus
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.tool.model.AdjustSlider
import java.time.LocalDateTime

/**
 * Everything the tool screen shows at one moment, as one snapshot. What a UiState is, and why
 * every screen has exactly one, is explained on `HomeUiState`; see there.
 *
 * The tool screen's snapshot holds the screen's tool ([tool]), the photo being worked on
 * ([photoUri]), what is shown in place of the photo ([photo]), whether Done is preparing the
 * finished picture ([finishing]) and the editing part: the edited photo ([edited]), whether an
 * edit can be taken back ([canUndo]), whether an edit is running ([processing]) and why the last
 * one failed ([failure]). Saving is not done here any more: Done hands the finished picture to
 * the Result screen, which saves and shares it.
 * The Edit screen also keeps here which of its tool sheets is open ([openTool]) and whether the
 * person is comparing with the original ([comparing]), its current settings ([settings]) and
 * whether it is asking to discard them ([discardPrompt]), so the whole screen reads one snapshot.
 *
 * **How the Edit screen's photo is drawn.** No full-size render happens while the person edits:
 * the screen draws the original photo ([photo]), or its small filtered copy ([filterPreview]) once
 * a filter is chosen, and applies the crop, turns, flips and colour sliders of [settings] itself
 * as it draws. So [edited] stays null on the Edit screen; the full render from the original runs
 * only when the edit is finished, when the person taps Done.
 *
 * [tool] and [photoUri] are the two facts that came in the navigation key, and neither changes on
 * this screen: each tool has a screen of its own.
 *
 * Loading the photo and the work behind Done are separate jobs that can happen in any order, so
 * each one changes only its own field of the snapshot and leaves the others as they are.
 */
data class ToolUiState(
    /**
     * The screen's tool, the one in the navigation key, for the ViewModel's whole life: its name
     * is the screen's centred title, and its message fills the options strip under the photo.
     */
    val tool: Tool,
    /** The Uri (as text) of the photo being worked on; it marks the image node for tests. */
    val photoUri: String,
    /** What is shown in place of the photo: nothing yet, the photo itself, or a message. */
    val photo: PhotoState = PhotoState.Loading,
    /**
     * True while Done is rendering the Edit screen's settings from the decoded photo in [photo]
     * (shrunk to at most 2000 pixels on its longest edge). The Done button then shows a small spinner and ignores taps; when the render ends,
     * well or badly, it goes false again. The other four tools hand their picture over at once,
     * so for them it never becomes true.
     */
    val finishing: Boolean = false,
    /**
     * The photo as the person has edited it so far, or null while nothing has been edited (or
     * every edit has been undone). When it is set, the screen shows it instead of the photo in
     * [photo], and Done hands it to the Result screen; [photo] itself always keeps the unedited
     * original, so undoing every edit returns to it. On the Edit screen it stays null: that screen
     * draws [settings] live on top of [photo] (or [filterPreview]) and renders the full photo only
     * when Done is tapped, so no full-size picture is made while the person is still editing.
     */
    val edited: Bitmap? = null,
    /**
     * The Edit screen's filter preview: the original photo shrunk to the preview size (at most
     * 640 pixels on its longest edge) with the current filter applied at its intensity, and
     * nothing else (the crop, turns, flips and sliders are drawn on top of it by the screen).
     * It is null while no filter is chosen, and before the first filter render has finished; the
     * screen then draws the original photo instead.
     */
    val filterPreview: Bitmap? = null,
    /** True when there is an edit to take back, so the screen's Undo control can be used. */
    val canUndo: Boolean = false,
    /**
     * True when an undone edit can be brought back, so the Redo control can be used. Only the Edit
     * screen has Redo; on the other tools it stays false.
     */
    val canRedo: Boolean = false,
    /** Whether an edit is running right now, and how far along it is. */
    val processing: Processing = Processing.Idle,
    /**
     * Why the last edit did not work, or null when there is nothing to report. The photo shown
     * is unchanged when this is set: a failed edit never replaces it.
     */
    val failure: EditFailure? = null,
    /**
     * The Edit screen's tool whose sheet is open under the photo, or null when every sheet is
     * closed. Only one sheet is open at a time; its entry in the row of tools is highlighted.
     * Other tools' screens never open one, so for them this stays null.
     */
    val openTool: EditTool? = null,
    /**
     * The Edit screen's Adjust sub-tool whose slider is shown (Adjust shows one slider at a time,
     * picked by a chip). It is kept here rather than in the panel, so reopening Adjust shows the
     * slider the person used last.
     */
    val adjustSlider: AdjustSlider = AdjustSlider.BRIGHTNESS,
    /**
     * True while the Compare switch is on: the screen shows the untouched original photo instead
     * of the edit, and every editing control is disabled until it is switched off again.
     */
    val comparing: Boolean = false,
    /**
     * The Edit screen's settings as they are now: crop, turn, flips, the four sliders and the
     * filter, one object for the whole visit. The sheets draw every control from it, and the photo
     * is this applied to the original. Other tools' screens leave it at its neutral default.
     */
    val settings: EditSettings = EditSettings(),
    /**
     * True while the Edit screen asks "Discard your changes?" after Back was pressed with changes
     * made; it goes false again with Discard or Keep editing.
     */
    val discardPrompt: Boolean = false,
    /** The Stickers sheet's chosen category chip (Food first); it survives closing the sheet. */
    val stickerCategory: StickerCategory = StickerCategory.FOOD,
    /**
     * The key of the sticker just added and still selected on the photo, or null when none is.
     * It is cleared when the Stickers sheet closes.
     */
    val selectedLayerKey: Long? = null,
    /**
     * The key of the sticker a finger is moving, pinching, turning or resizing right now, or null
     * when no gesture is under way. It is set when the gesture's first change arrives and cleared
     * when the last finger lifts. While it is set the live picture draws that one sticker itself,
     * following the finger every frame, and the small filtered copy is rendered without it; when
     * the finger lifts the sticker joins the filtered copy.
     */
    val touchedLayerKey: Long? = null,
    /**
     * True for a moment after the person tried to add a sticker or a text beyond the limit: the
     * screen shows the "20 stickers, texts and dates at most" message, then calls the ViewModel to clear
     * this flag.
     */
    val layerLimitShown: Boolean = false,
    /**
     * True while the Text sheet's text area has the keyboard: the sheet then shows only the text
     * area above the keyboard (its font, colour and slider rows come back when it closes), and
     * every letter typed is previewed on the photo without an undo step until the keyboard closes.
     */
    val textEditing: Boolean = false,
    /**
     * Where the largest face is in the photo, found once after the photo is decoded, on a face
     * tool's screen (Beautify) only; null before that, on every other tool, and when no face was
     * found. The photo card zooms in on it, so a Face width change is easy to see.
     */
    val faceFocus: FaceFocus? = null,
    /**
     * True while the photo card must show the whole photo instead of zooming onto [faceFocus]: on
     * Beautify while the Body group is open, since the zoom onto the face would hide the waist.
     */
    val wholePhoto: Boolean = false,
    /**
     * A live look of a slider tool's edit (Beautify) while its slider is dragged: a recipe the card
     * draws on the graphics chip over the photo as shown (see [LivePreview]); null when there is
     * none. While it is set the card draws the original photo through it instead of [edited], so
     * the photo follows the finger. The quiet render that starts when the finger lifts replaces it
     * when it ends (unless the person dragged again), and Undo clears it.
     */
    val livePreview: LivePreview? = null,
    /**
     * True while a slider tool's full-quality render runs quietly in the background after a
     * slider was let go (see `ToolViewModel.renderRegistered`): no progress ring and the sliders
     * stay usable, but Done shows its small spinner and waits, and Undo waits too.
     */
    val rendering: Boolean = false,
    /**
     * The moment (date and time, to the minute) the Date sheet gives the NEXT date it adds, and
     * the one its format chips show while no date is selected. It becomes now each time the Date
     * sheet opens, and the sheet's date and time pickers change it while no date is selected
     * (with one selected they change that date instead). Null until the Date sheet first opens.
     */
    val dateMoment: LocalDateTime? = null,
) {

    /**
     * Whether an edit is running. While one runs, the screen shows its progress and does not let
     * the person tap Done or Undo, so a half-made edit is never handed over by mistake.
     */
    sealed interface Processing {

        /** No edit is running. */
        data object Idle : Processing

        /**
         * An edit is running; [progress] goes from 0 (just started) to 1 (about to finish).
         */
        data class Running(val progress: Float) : Processing
    }

    /**
     * What the tool screen shows in place of the photo: still loading, the photo, or a message.
     */
    sealed interface PhotoState {

        /** The photo is being decoded; nothing is shown yet. */
        data object Loading : PhotoState

        /** The photo is decoded: [bitmap] is upright and sized for the screen. */
        data class Ready(val bitmap: Bitmap) : PhotoState

        /** The photo is missing, not readable by Kanso, or not an image. */
        data object Unreadable : PhotoState
    }
}
