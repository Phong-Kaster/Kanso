package com.phongkaster.kanso.tool.face

import com.phongkaster.kanso.data.edit.face.FaceAmounts
import com.phongkaster.kanso.data.edit.face.FaceLift
import com.phongkaster.kanso.data.edit.face.FaceShape
import com.phongkaster.kanso.data.edit.face.FaceTemple
import com.phongkaster.kanso.data.edit.face.FaceWidth
import com.phongkaster.kanso.data.edit.face.WarpPush
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FaceShape]: Width and Lift together. Each alone gives exactly its own pushes; together the
 * pushes on the same outline point are added into one, never listed twice (which LocalWarp would
 * average down to half of each).
 */
class FaceShapeTest {

    /** The face every test reshapes, the same oval as [FaceWidthTest] and [FaceLiftTest]. */
    private val face = SampleFace.oval(centerX = 500f, centerY = 500f, halfWidth = 200f, halfHeight = 260f)

    /** Both at 0 gives nothing; one at 0 gives exactly the other's pushes. */
    @Test
    fun one_alone_is_unchanged() {
        assertTrue(FaceShape.pushesFor(face, FaceAmounts()).isEmpty())
        assertEquals(FaceWidth.pushesFor(face, 0.7f), FaceShape.pushesFor(face, FaceAmounts(width = 0.7f)))
        assertEquals(FaceLift.pushesFor(face, 0.7f), FaceShape.pushesFor(face, FaceAmounts(lift = 0.7f)))
        assertEquals(FaceTemple.pushesFor(face, 0.7f), FaceShape.pushesFor(face, FaceAmounts(temple = 0.7f)))
    }

    /**
     * Together, every outline point has one push whose move is Width's plus Lift's; the cheek top,
     * which Lift leaves alone, keeps Width's push as it is.
     */
    @Test
    fun together_the_moves_add_up_point_by_point() {
        val width = FaceWidth.pushesFor(face, 0.5f)
        val lift = FaceLift.pushesFor(face, 0.8f)
        val both = FaceShape.pushesFor(face, FaceAmounts(width = 0.5f, lift = 0.8f))

        assertEquals(width.size, both.size)
        width.forEach(
            action = { push ->
                val other = lift.firstOrNull(predicate = { it.centerX == push.centerX && it.centerY == push.centerY })
                val merged = both.single(predicate = { it.centerX == push.centerX && it.centerY == push.centerY })
                assertEquals(push.moveX + (other?.moveX ?: 0f), merged.moveX, 0.0001f)
                assertEquals(push.moveY + (other?.moveY ?: 0f), merged.moveY, 0.0001f)
            },
        )
    }

    /** Merging keeps the larger reach and the order in which each centre first appeared. */
    @Test
    fun merging_keeps_the_larger_radius_and_the_first_order() {
        val pushes = listOf(
            WarpPush(centerX = 1f, centerY = 1f, moveX = 1f, moveY = 0f, radius = 5f),
            WarpPush(centerX = 2f, centerY = 2f, moveX = 0f, moveY = 1f, radius = 5f),
            WarpPush(centerX = 1f, centerY = 1f, moveX = 2f, moveY = 3f, radius = 8f),
        )

        val merged = FaceShape.merged(pushes)

        assertEquals(
            listOf(
                WarpPush(centerX = 1f, centerY = 1f, moveX = 3f, moveY = 3f, radius = 8f),
                WarpPush(centerX = 2f, centerY = 2f, moveX = 0f, moveY = 1f, radius = 5f),
            ),
            merged,
        )
    }
}
