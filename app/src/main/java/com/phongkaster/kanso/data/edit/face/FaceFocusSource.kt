package com.phongkaster.kanso.data.edit.face

import android.graphics.Bitmap

/**
 * A tool processor that can say where the main face of a photo is, before any edit runs.
 *
 * The tool screen's ViewModel knows every processor only as a `ToolProcessor`; it checks for this
 * interface (`processor as? FaceFocusSource`) instead of naming the Beautify processor, so any face
 * tool added later gets the zoom by implementing it.
 */
interface FaceFocusSource {

    /**
     * Returns where the largest face in [photo] is, or null when there is none or it could not be
     * looked for. It never throws, except a `CancellationException` when the screen closes: a
     * missing zoom must never stop the tool from working.
     */
    suspend fun findFocus(photo: Bitmap): FaceFocus?
}
