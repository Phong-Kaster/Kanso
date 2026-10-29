package com.phongkaster.kanso.tool.face

import com.phongkaster.kanso.data.edit.face.FaceCheekbones
import com.phongkaster.kanso.data.edit.face.FaceMeshIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FaceCheekbones] on a made-up upright oval face ([SampleFace]): only the outline's top three
 * points on each side move, inward for a positive amount and outward for a negative one, mirrored,
 * and in step with the face's size. Plain maths, so it runs on the JVM without Robolectric.
 */
class FaceCheekbonesTest {

    /** The face every test reshapes: centred at (500, 500), 200 to each cheek, 260 to the chin. */
    private val face = SampleFace.oval(centerX = 500f, centerY = 500f, halfWidth = 200f, halfHeight = 260f)

    /** Cheekbones 0 (and NaN) gives no pushes. */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(FaceCheekbones.pushesFor(face, 0f).isEmpty())
        assertTrue(FaceCheekbones.pushesFor(face, Float.NaN).isEmpty())
    }

    /**
     * A positive amount pulls the outline's top three points on each side inward (slimmer) and
     * leaves the jaw (positions 3 to 6) without a push.
     */
    @Test
    fun positive_pulls_the_cheekbones_in_and_leaves_the_jaw() {
        val pushes = FaceCheekbones.pushesFor(face, 1f)
        val centres = pushes.map(transform = { it.centerX to it.centerY })

        assertEquals(6, pushes.size)
        pushes.take(3).forEach(action = { assertTrue("left moved by ${it.moveX}", it.moveX > 0f) })
        pushes.drop(3).forEach(action = { assertTrue("right moved by ${it.moveX}", it.moveX < 0f) })
        (3 until FaceMeshIndex.LEFT_OUTLINE.size).forEach(
            action = { position ->
                val jaw = face[FaceMeshIndex.LEFT_OUTLINE[position]]
                assertTrue("the jaw at position $position was pushed", (jaw.x to jaw.y) !in centres)
            },
        )
    }

    /** A negative amount pushes the cheekbones outward, the same pushes turned round. */
    @Test
    fun negative_pushes_them_out() {
        FaceCheekbones.pushesFor(face, 0.6f).zip(FaceCheekbones.pushesFor(face, -0.6f)).forEach(
            action = { (slim, wide) -> assertEquals(-slim.moveX, wide.moveX, 0.001f) },
        )
    }

    /** A value beyond ±1 is clamped, so 5 moves exactly as far as 1. */
    @Test
    fun beyond_one_is_clamped() {
        assertEquals(FaceCheekbones.pushesFor(face, 1f), FaceCheekbones.pushesFor(face, 5f))
    }

    /** The same face twice as large is pushed twice as far, with twice the reach. */
    @Test
    fun the_moves_follow_the_face_size() {
        val large = SampleFace.oval(centerX = 1000f, centerY = 1000f, halfWidth = 400f, halfHeight = 520f)

        FaceCheekbones.pushesFor(face, 0.5f).zip(FaceCheekbones.pushesFor(large, 0.5f)).forEach(
            action = { (one, two) ->
                assertEquals(one.moveX * 2, two.moveX, 0.01f)
                assertEquals(one.radius * 2, two.radius, 0.01f)
            },
        )
    }
}
