package com.phongkaster.kanso.tool.face

import com.phongkaster.kanso.data.edit.face.FaceFocus
import com.phongkaster.kanso.ui.tool.model.PhotoZoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [PhotoZoom.toward]: how close the card zooms on a face and where it slides the photo. Plain
 * maths, so it runs on the JVM without Robolectric.
 */
class PhotoZoomTest {

    /**
     * A face of 20% by 30% of the photo in the middle: the scale fills 60% of the frame in the
     * tighter direction (0.6 / 0.3 = 2) and nothing slides.
     */
    @Test
    fun a_centred_face_zooms_without_sliding() {
        val zoom = PhotoZoom.toward(FaceFocus(left = 0.4f, top = 0.35f, right = 0.6f, bottom = 0.65f))

        assertEquals(2f, zoom.scale, 0.001f)
        assertEquals(0f, zoom.shiftX, 0.001f)
        assertEquals(0f, zoom.shiftY, 0.001f)
    }

    /** The face's centre lands in the frame's centre: 0.5 + scale × (centre − 0.5) + shift = 0.5. */
    @Test
    fun an_off_centre_face_is_brought_to_the_middle() {
        val focus = FaceFocus(left = 0.3f, top = 0.3f, right = 0.4f, bottom = 0.45f)
        val zoom = PhotoZoom.toward(focus)

        assertEquals(0.5f, 0.5f + zoom.scale * (focus.centerX - 0.5f) + zoom.shiftX, 0.001f)
        assertEquals(0.5f, 0.5f + zoom.scale * (focus.centerY - 0.5f) + zoom.shiftY, 0.001f)
    }

    /** A face in a corner slides only as far as keeps the photo's edge out of the frame. */
    @Test
    fun a_face_in_a_corner_never_shows_past_the_edge() {
        val zoom = PhotoZoom.toward(FaceFocus(left = 0f, top = 0f, right = 0.2f, bottom = 0.2f))
        val limit = (zoom.scale - 1f) / 2f

        assertEquals(limit, zoom.shiftX, 0.001f)
        assertEquals(limit, zoom.shiftY, 0.001f)
    }

    /** A tiny face stops at 4 times, and a face that already fills the photo is not zoomed. */
    @Test
    fun the_scale_stays_between_one_and_four() {
        val tiny = PhotoZoom.toward(FaceFocus(left = 0.5f, top = 0.5f, right = 0.51f, bottom = 0.51f))
        val huge = PhotoZoom.toward(FaceFocus(left = 0f, top = 0f, right = 1f, bottom = 1f))

        assertEquals(4f, tiny.scale, 0f)
        assertEquals(PhotoZoom.NONE, huge)
    }

    /** An empty box (no width or height) gives the whole photo. */
    @Test
    fun an_empty_face_gives_no_zoom() {
        assertEquals(PhotoZoom.NONE, PhotoZoom.toward(FaceFocus(left = 0.5f, top = 0.5f, right = 0.5f, bottom = 0.7f)))
        assertTrue(PhotoZoom.NONE.scale == 1f)
    }
}
