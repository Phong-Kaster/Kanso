package com.phongkaster.kanso.saved

import com.phongkaster.kanso.ui.saved.shareMimeTypeFor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Checks [shareMimeTypeFor] (Run 8): a photo is shared as `image/png` when its provider reports
 * `image/png`, or reports nothing and its name ends in `.png` (a saved cut-out), and as
 * `image/jpeg`, Kanso's usual format, otherwise. It is a plain function, so no Android is needed.
 */
class SharePhotoTest {

    /** A provider's own `image/png` wins, even over a `.jpg` name. */
    @Test
    fun a_reported_png_is_shared_as_png() {
        assertEquals("image/png", shareMimeTypeFor(reportedType = "image/png", fileName = "Kanso_1.jpg"))
        assertEquals("image/png", shareMimeTypeFor(reportedType = "image/png", fileName = null))
    }

    /** With no type reported, a `.png` name (any case) is shared as PNG. */
    @Test
    fun a_png_name_is_shared_as_png_when_nothing_is_reported() {
        assertEquals("image/png", shareMimeTypeFor(reportedType = null, fileName = "Kanso_20261014_120000.png"))
        assertEquals("image/png", shareMimeTypeFor(reportedType = null, fileName = "/storage/Pictures/Kanso/CUT.PNG"))
    }

    /**
     * Anything else is shared as JPEG: a reported JPEG, a `.jpg` name, no name at all, or a
     * report that is not an image type with a name that is not `.png`.
     */
    @Test
    fun everything_else_is_shared_as_jpeg() {
        assertEquals("image/jpeg", shareMimeTypeFor(reportedType = "image/jpeg", fileName = "Kanso_1.jpg"))
        assertEquals("image/jpeg", shareMimeTypeFor(reportedType = null, fileName = "Kanso_1.jpg"))
        assertEquals("image/jpeg", shareMimeTypeFor(reportedType = null, fileName = null))
        assertEquals("image/jpeg", shareMimeTypeFor(reportedType = "application/octet-stream", fileName = "42"))
    }

    /** A report that is not an image type is ignored, so a `.png` name still decides. */
    @Test
    fun a_non_image_report_falls_back_to_the_name() {
        assertEquals("image/png", shareMimeTypeFor(reportedType = "application/octet-stream", fileName = "Kanso_1.png"))
    }
}
