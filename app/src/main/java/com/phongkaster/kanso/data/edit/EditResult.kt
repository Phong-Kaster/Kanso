package com.phongkaster.kanso.data.edit

import android.graphics.Bitmap

/**
 * What came of one run of a tool processor: either the edited picture or the reason there is
 * none. Returning a result instead of throwing keeps failure an ordinary value that the tool
 * screen's ViewModel can show, rather than an exception that might escape and crash the app.
 */
sealed interface EditResult {

    /**
     * The edit worked. [bitmap] is the new picture, at the working size the processor was given;
     * the caller keeps it as the photo's current state and can still undo back to the one before.
     */
    data class Edited(val bitmap: Bitmap) : EditResult

    /**
     * The edit did not work, for the reason in [failure]. The photo the caller had stays as it
     * was: nothing about it has changed.
     */
    data class Failed(val failure: EditFailure) : EditResult
}
