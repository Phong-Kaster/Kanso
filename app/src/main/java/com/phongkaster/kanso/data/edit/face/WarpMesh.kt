package com.phongkaster.kanso.data.edit.face

/**
 * A grid laid over one region of a picture whose points have been moved by [WarpPush]es, for
 * drawing a warp on the GPU while a slider is dragged, as Meitu does.
 *
 * **Why a mesh.** [LocalWarp] works out every pixel in Kotlin, which takes far too long to repeat
 * many times a second. Android's `Canvas.drawBitmapMesh` instead takes a grid of points and
 * stretches a picture between them on the graphics chip: moving about a thousand grid points is
 * enough, and the chip does the pixels. The saved picture still comes from [LocalWarp]; the mesh
 * only shows, live, what it will look like.
 *
 * **An even grid over one region.** `drawBitmapMesh` always cuts its picture into an even grid of
 * [columns] x [rows] cells and pins each cell's corners to the matching [vertices]; the grid
 * cannot be finer in one place than another. A face is often a small part of the photo, so the
 * grid covers only the region a live look changes (the faces and the pushes' circles): the screen
 * draws the whole photo as it is, then that region, cut out, through this mesh on top. The
 * region's edges do not move (no push reaches past them), so the two meet without a seam.
 *
 * **Forward, not backward.** [LocalWarp] asks, for each result pixel, where its colour comes from
 * (`source = pixel − move`). A mesh says where each grid point of the source goes, so each point
 * is drawn at `point + move`. For moves as small as a face reshape the two agree closely.
 *
 * [vertices] holds `(columns + 1) × (rows + 1)` points as x, y pairs, row after row, in pixels of
 * the whole picture (not of the region).
 */
class WarpMesh(val columns: Int, val rows: Int, val vertices: FloatArray) {

    /** Builds meshes. */
    companion object {

        /**
         * The mesh for the region from ([left], [top]) to ([right], [bottom]), in pixels of the
         * picture, moved by [pushes]: [STEPS] x [STEPS] even cells, each grid point moved by
         * [LocalWarp.moveAt]. With no pushes no point moves.
         */
        fun build(pushes: List<WarpPush>, left: Int, top: Int, right: Int, bottom: Int): WarpMesh {
            val vertices = FloatArray((STEPS + 1) * (STEPS + 1) * 2)
            val move = FloatArray(2)
            val cellWidth = (right - left).toFloat() / STEPS
            val cellHeight = (bottom - top).toFloat() / STEPS
            var i = 0
            for (row in 0..STEPS) {
                for (column in 0..STEPS) {
                    val x = left + column * cellWidth
                    val y = top + row * cellHeight
                    LocalWarp.moveAt(x, y, pushes, move)
                    vertices[i++] = x + move[0]
                    vertices[i++] = y + move[1]
                }
            }
            return WarpMesh(columns = STEPS, rows = STEPS, vertices = vertices)
        }

        /** How many cells the grid has across, and down, the region. */
        const val STEPS = 32
    }
}
