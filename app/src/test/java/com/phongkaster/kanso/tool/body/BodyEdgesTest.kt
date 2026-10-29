package com.phongkaster.kanso.tool.body

import com.phongkaster.kanso.data.edit.body.BodyEdges
import com.phongkaster.kanso.data.edit.body.BodyMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [BodyEdges] and [BodyMask]: walking across a mask finds the first background pixel, gives up
 * past its limit, and a mask smaller than the photo is read in the photo's pixels.
 */
class BodyEdgesTest {

    /** A 100 x 100 mask whose person is the column band x = 40..60. */
    private val mask = BodyMask(100, 100, FloatArray(100 * 100, init = { i -> if (i % 100 in 40..60) 1f else 0f }), 100, 100)

    /** From x = 50 going right, x = 61 is the first background pixel: 11 steps. */
    @Test
    fun finds_the_first_background_pixel() {
        assertEquals(11f, BodyEdges.distanceToEdge(mask, 50f, 50f, 1f, 0f, maxDistance = 50f))
        assertEquals(11f, BodyEdges.distanceToEdge(mask, 50f, 50f, -1f, 0f, maxDistance = 50f))
    }

    /** The walk gives up after its limit, and a start outside the person finds nothing. */
    @Test
    fun gives_up_past_its_limit_or_outside_the_person() {
        assertNull(BodyEdges.distanceToEdge(mask, 50f, 50f, 1f, 0f, maxDistance = 5f))
        assertNull(BodyEdges.distanceToEdge(mask, 10f, 50f, 1f, 0f, maxDistance = 50f))
    }

    /** A 10 x 10 mask over a 100 x 100 photo: photo pixel (55, 55) reads mask cell (5, 5). */
    @Test
    fun a_small_mask_is_read_in_the_photos_pixels() {
        val values = FloatArray(10 * 10)
        values[5 * 10 + 5] = 1f
        val small = BodyMask(10, 10, values, 100, 100)

        assertEquals(1f, small.at(55f, 55f))
        assertEquals(0f, small.at(45f, 55f))
        assertEquals(0f, small.at(-1f, 55f))
        assertEquals(0f, small.at(150f, 55f))
    }
}
