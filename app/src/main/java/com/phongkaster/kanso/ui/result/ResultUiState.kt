package com.phongkaster.kanso.ui.result

/**
 * Everything the Result screen shows at one moment, as one snapshot.
 *
 * The Result screen has two uses. Right after Done on a tool screen ([viewer] false) it shows the
 * picture Done rendered, which waits in the hand-off store (`RenderedPhotoStore`), and offers
 * Share, Save and Go home. As the photo viewer ([viewer] true), a photo opened from the My edits
 * tab, it shows a photo already in the gallery and offers Share, Edit and the round close button.
 *
 * [photoUri] is the text of the viewer's photo Uri: the viewer shares that photo and marks the
 * image with it, so tests can tell which photo is shown. It is null right after Done, where the
 * picture exists only in memory until it is saved.
 *
 * [photo] is where the picture stands: [ResultPhoto.Ready] with it, [ResultPhoto.Empty] when the
 * hand-off store held nothing, and for the viewer [ResultPhoto.Loading] until it is decoded, then
 * [ResultPhoto.Ready] or [ResultPhoto.Unreadable].
 *
 * [save] is where saving the picture to the gallery stands (right after Done only; the viewer's
 * photo is already saved).
 *
 * Every screen in the app describes itself this way; `HomeUiState` explains the idea in full.
 */
data class ResultUiState(
    val photoUri: String? = null,
    val photo: ResultPhoto = ResultPhoto.Loading,
    val viewer: Boolean = false,
    val save: SaveState = SaveState.Idle,
) {

    /** Where saving the rendered picture into `Pictures/Kanso` stands. */
    sealed interface SaveState {

        /** Nothing has been saved yet; Save can be tapped. */
        data object Idle : SaveState

        /** The picture is being written; further taps on Save are ignored. */
        data object Saving : SaveState

        /**
         * The picture is in the gallery; Save is switched off so it cannot write a second copy.
         * [keptOriginalOrientation] is true when the saver had to copy the picture as it was, so
         * the screen warns that some apps may show it sideways.
         */
        data class Saved(val keptOriginalOrientation: Boolean = false) : SaveState

        /** Writing failed and nothing was saved; Save can be tapped again. */
        data object Failed : SaveState

        /**
         * The person refused the storage-write permission (Android 8.1 and 9 only), so nothing
         * was saved; Save can be tapped again, which asks again.
         */
        data object PermissionDenied : SaveState
    }
}
