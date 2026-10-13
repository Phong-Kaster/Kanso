package com.phongkaster.kanso.ui.tool

import android.graphics.Bitmap
import com.phongkaster.kanso.navigation.Tool

/**
 * Everything the tool screen shows at one moment, as one snapshot. What a UiState is, and why
 * every screen has exactly one, is explained on `HomeUiState`; see there.
 *
 * The tool screen's snapshot holds the screen's tool ([tool]), the photo being worked on
 * ([photoUri]), what is shown in place of the photo ([photo]) and where saving stands ([save]).
 * [tool] and [photoUri] are the two facts that came in the navigation key, and neither changes on
 * this screen: each tool has a screen of its own.
 *
 * Loading the photo and saving it are separate jobs that can happen in any order, so each one
 * changes only its own field of the snapshot and leaves the others as they are: a photo decoded
 * while a save runs never forgets the save.
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
    /** Where saving the photo stands: nothing yet, running, done, failed or refused. */
    val save: SaveState = SaveState.Idle,
) {

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

    /**
     * Where saving the photo stands, for the Save action and the message under the photo.
     */
    sealed interface SaveState {

        /** Nothing saved yet on this visit, or the last message has been replaced. */
        data object Idle : SaveState

        /** A save is running; Save cannot be tapped again until it ends. */
        data object Saving : SaveState

        /**
         * The photo was saved. [keptOriginalOrientation] is true when it was too large to turn
         * upright and was copied as it is; the screen then says some apps may show it sideways.
         */
        data class Saved(val keptOriginalOrientation: Boolean) : SaveState

        /** The photo could not be saved; nothing was written. */
        data object Failed : SaveState

        /** The person refused the storage-write permission (Android 8.1 and 9); nothing was written. */
        data object PermissionDenied : SaveState
    }
}
