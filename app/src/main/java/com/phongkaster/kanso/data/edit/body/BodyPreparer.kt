package com.phongkaster.kanso.data.edit.body

import android.graphics.Bitmap

/**
 * A tool processor that can find the bodies in a photo **before** a body slider is dragged, so
 * the first drag already follows the finger.
 *
 * **Why.** A live look of a body reshape needs the person's pose and mask, and the first time
 * for a photo the pose model must run on it: a few seconds on a phone like the Galaxy A13. Until
 * it is done no live frame can be drawn, so a first, short drag showed nothing until the finger
 * lifted (the owner saw Chest "not live", 2026-10-29). Starting the model as the Body group opens
 * hides that wait behind the person's own reach for a slider.
 *
 * The tool screen's ViewModel checks for this interface (`processor as? BodyPreparer`), as it
 * does for `LivePreviewSource`.
 */
interface BodyPreparer {

    /**
     * Finds and keeps the bodies in [photo] (the unedited photo as shown on screen, the same
     * bitmap later live looks are made for), so those looks need not wait for the pose model.
     * Nothing is thrown but a `CancellationException`: a failure here only means the first live
     * look will try again.
     */
    suspend fun prepareBodies(photo: Bitmap)
}
