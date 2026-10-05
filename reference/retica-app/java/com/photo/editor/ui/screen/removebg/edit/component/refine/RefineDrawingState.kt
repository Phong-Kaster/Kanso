package com.photo.editor.ui.screen.removebg.edit.component.refine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.Size
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat

@Stable
class RefineDrawingState {

    /**
     * Paths that are added, this is required to have paths with different options and paths
     *  ith erase to keep over each other.
     *  Each path is presented with points in range [0 - 1] and their property.
     */
    val paths = mutableStateListOf<Pair<List<Offset>, RefinePathProperties>>()

    /**
     * Paths that are undone via button. These paths are restored if user pushes
     * redo button if there is no new path drawn.
     *
     * If new path is drawn after this list is cleared to not break paths after undoing previous
     * ones.
     */
    val pathsUndone = mutableStateListOf<Pair<List<Offset>, RefinePathProperties>>()

    /**
     * Canvas touch state. [RefineMotionEvent.Idle] by default, [RefineMotionEvent.Down] at first contact,
     * [RefineMotionEvent.Move] while dragging and [RefineMotionEvent.Up] when first pointer is up
     */
    var motionEvent by mutableStateOf(RefineMotionEvent.Idle)

    /**
     * Current position of the pointer that is pressed or being moved
     */
    var currentPosition by mutableStateOf(Offset.Unspecified)

    /**
     * Previous motion event before next touch is saved into this current position.
     */
    var previousPosition by mutableStateOf(Offset.Unspecified)

    /**
     * Path that is being drawn between [RefineMotionEvent.Down] and [RefineMotionEvent.Up]. When
     * pointer is up this path is saved to **paths** and new instance is created
     */
    var currentPath by mutableStateOf(Path())

    /**
     * Points of drawing path. Value of this point is presented in range [0 - 1] instead of position
     **/
    var currentPathPoints = mutableListOf<Offset>()

    /**
     * Properties of path that is currently being drawn between
     * [RefineMotionEvent.Down] and [RefineMotionEvent.Up].
     */
    var currentPathProperty by mutableStateOf(RefinePathProperties())

    /**
     * Reset drawing state
     */
    fun reset() {
        paths.clear()
        pathsUndone.clear()
        currentPath = Path()
        currentPathPoints.clear()
        currentPathProperty = RefinePathProperties()
    }

    /**
     * Create a bitmap from drawn paths and canvas size
     * Red color represents Erase
     * Green color represents Restore
     */
    fun createBitmap(size: Size): Bitmap {
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Draw paths
        paths.forEach { (points, property) ->
            val path = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x * size.width, point.y * size.height)
                    else quadraticTo(
                        (points[index - 1].x + point.x) / 2 * size.width,
                        (points[index - 1].y + point.y) / 2 * size.height,
                        point.x * size.width,
                        point.y * size.height
                    )
                }
            }

            val clearPaint = Paint().apply {
                color = android.graphics.Color.TRANSPARENT

                style = Paint.Style.STROKE
                strokeWidth = property.strokeWidth
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    blendMode = android.graphics.BlendMode.CLEAR
                } else {
                    colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.CLEAR)
                }
            }

            val pathPaint = Paint().apply {
                color = if (property.eraseMode) android.graphics.Color.GREEN else android.graphics.Color.RED

                style = Paint.Style.STROKE
                strokeWidth = property.strokeWidth
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            canvas.drawPath(path.asAndroidPath(), clearPaint)
            canvas.drawPath(path.asAndroidPath(), pathPaint)
        }

        return bitmap
    }
}