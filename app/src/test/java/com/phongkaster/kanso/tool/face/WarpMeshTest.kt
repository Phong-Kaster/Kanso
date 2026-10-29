package com.phongkaster.kanso.tool.face

import com.phongkaster.kanso.data.edit.face.LocalWarp
import com.phongkaster.kanso.data.edit.face.WarpMesh
import com.phongkaster.kanso.data.edit.face.WarpPush
import kotlin.math.abs
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [WarpMesh]: the even grid the screen draws a live look's region through. Plain maths, so it runs
 * on the JVM without Robolectric.
 */
class WarpMeshTest {

    /** One push in the middle of the region (150, 100)-(250, 200), moving 10 pixels to the right. */
    private val push = WarpPush(centerX = 200f, centerY = 150f, moveX = 10f, moveY = 0f, radius = 50f)

    /** The grid has one point per column and row line, as `drawBitmapMesh` expects. */
    @Test
    fun the_vertex_count_matches_the_grid() {
        val mesh = WarpMesh.build(listOf(push), 150, 100, 250, 200)

        assertEquals(WarpMesh.STEPS, mesh.columns)
        assertEquals(WarpMesh.STEPS, mesh.rows)
        assertEquals((mesh.columns + 1) * (mesh.rows + 1) * 2, mesh.vertices.size)
    }

    /** With no pushes every grid point sits where the even grid puts it, corners on the region's. */
    @Test
    fun no_pushes_moves_nothing() {
        val mesh = WarpMesh.build(emptyList(), 150, 100, 250, 200)

        assertArrayEquals(floatArrayOf(150f, 100f), mesh.vertices.copyOfRange(0, 2), 0f)
        assertArrayEquals(floatArrayOf(250f, 200f), mesh.vertices.copyOfRange(mesh.vertices.size - 2, mesh.vertices.size), 0.001f)
        // The point one cell right of the top-left corner.
        assertEquals(150f + 100f / WarpMesh.STEPS, mesh.vertices[2], 0.001f)
    }

    /**
     * The region's corners, on the edge of the push's circle, stay where they are; the grid point
     * at the push's centre moves right by exactly what [LocalWarp.moveAt] gives there.
     */
    @Test
    fun only_points_inside_the_push_move() {
        val mesh = WarpMesh.build(listOf(push), 150, 100, 250, 200)
        val move = FloatArray(2)
        LocalWarp.moveAt(push.centerX, push.centerY, listOf(push), move)

        assertArrayEquals(floatArrayOf(150f, 100f), mesh.vertices.copyOfRange(0, 2), 0f)
        assertArrayEquals(floatArrayOf(250f, 200f), mesh.vertices.copyOfRange(mesh.vertices.size - 2, mesh.vertices.size), 0.001f)
        val atCentre = (0 until mesh.vertices.size / 2).any(
            predicate = { i ->
                abs(mesh.vertices[2 * i] - (push.centerX + move[0])) < 0.001f && abs(mesh.vertices[2 * i + 1] - push.centerY) < 0.001f
            },
        )
        assertTrue("the grid point at the push's centre must move right by ${move[0]}", move[0] > 0f && atCentre)
    }
}
