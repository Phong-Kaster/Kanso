package com.phongkaster.kanso.tool.face

import com.phongkaster.kanso.data.edit.face.FaceChinLength
import com.phongkaster.kanso.data.edit.face.FaceHairline
import com.phongkaster.kanso.data.edit.face.FaceMeshIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FaceChinLength] and [FaceHairline] (both built on `FaceVertical`) on a made-up upright oval face
 * ([SampleFace]): which way each moves its points along the face, that the chin and the middle of
 * the hairline move the most, and that the moves follow the face's size. Plain maths.
 */
class FaceChinAndHairlineTest {

    /** The face every test reshapes: centred at (500, 500), 200 to each cheek, 260 to the chin. */
    private val face = SampleFace.oval(centerX = 500f, centerY = 500f, halfWidth = 200f, halfHeight = 260f)

    /** 0 (and NaN) gives no pushes for either tool. */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(FaceChinLength.pushesFor(face, 0f).isEmpty())
        assertTrue(FaceChinLength.pushesFor(face, Float.NaN).isEmpty())
        assertTrue(FaceHairline.pushesFor(face, 0f).isEmpty())
        assertTrue(FaceHairline.pushesFor(face, Float.NaN).isEmpty())
    }

    /**
     * A positive chin length moves the chin's points up (a shorter chin), straight up on an upright
     * face, the chin's own point the most; a negative one moves them down.
     */
    @Test
    fun a_positive_chin_length_raises_the_chin() {
        val shorter = FaceChinLength.pushesFor(face, 1f)
        val longer = FaceChinLength.pushesFor(face, -1f)

        assertEquals(FaceMeshIndex.CHIN_AREA.size, shorter.size)
        shorter.forEach(
            action = { push ->
                assertTrue("moved by ${push.moveY} down", push.moveY < 0f)
                assertEquals(0f, push.moveX, 0.001f)
            },
        )
        longer.forEach(action = { assertTrue("moved by ${it.moveY} down", it.moveY > 0f) })
        assertTrue(shorter.first().moveY <= shorter.minOf(selector = { it.moveY }))
    }

    /** A positive hairline moves the hairline's points down (a lower hairline), the middle most. */
    @Test
    fun a_positive_hairline_lowers_the_hairline() {
        val lower = FaceHairline.pushesFor(face, 1f)
        val higher = FaceHairline.pushesFor(face, -1f)

        assertEquals(FaceMeshIndex.HAIRLINE.size, lower.size)
        lower.forEach(action = { assertTrue("moved by ${it.moveY} down", it.moveY > 0f) })
        higher.forEach(action = { assertTrue("moved by ${it.moveY} down", it.moveY < 0f) })
        assertTrue(lower.first().moveY >= lower.maxOf(selector = { it.moveY }))
        lower.forEach(action = { assertTrue("hairline push at y ${it.centerY} is below the eyes", it.centerY < 500f) })
    }

    /** Values beyond ±1 are clamped. */
    @Test
    fun beyond_one_is_clamped() {
        assertEquals(FaceChinLength.pushesFor(face, 1f), FaceChinLength.pushesFor(face, 5f))
        assertEquals(FaceHairline.pushesFor(face, -1f), FaceHairline.pushesFor(face, -5f))
    }

    /** The same face twice as large is moved twice as far, with twice the reach. */
    @Test
    fun the_moves_follow_the_face_size() {
        val large = SampleFace.oval(centerX = 1000f, centerY = 1000f, halfWidth = 400f, halfHeight = 520f)

        (FaceChinLength.pushesFor(face, 0.5f) + FaceHairline.pushesFor(face, 0.5f))
            .zip(FaceChinLength.pushesFor(large, 0.5f) + FaceHairline.pushesFor(large, 0.5f))
            .forEach(
                action = { (one, two) ->
                    assertEquals(one.moveY * 2, two.moveY, 0.01f)
                    assertEquals(one.radius * 2, two.radius, 0.01f)
                },
            )
    }
}
