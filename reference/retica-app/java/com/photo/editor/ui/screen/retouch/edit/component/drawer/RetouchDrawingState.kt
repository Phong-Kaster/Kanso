package com.photo.editor.ui.screen.retouch.edit.component.drawer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Size
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import com.photo.editor.common.util.tryWithoutCatch
import kotlin.math.max

@Stable
class RetouchDrawingState {

    /**
     * Record drawing surface size so that drawing stroke can be scaled correctly when converted to bitmap
     * */
    private var surfaceSize = Size(0, 0)

    /**
     * Paths that are added, this is required to have paths with different options and paths
     *  ith erase to keep over each other.
     *  Each path is presented with points in range [0 - 1] and their property.
     */
    val paths = mutableStateListOf<Pair<List<Offset>, RetouchPathProperties>>()

    /**
     * Paths that are undone via button. These paths are restored if user pushes
     * redo button if there is no new path drawn.
     *
     * If new path is drawn after this list is cleared to not break paths after undoing previous
     * ones.
     */
    val pathsUndone = mutableStateListOf<Pair<List<Offset>, RetouchPathProperties>>()

    /**
     * Canvas touch state. [RetouchMotionEvent.Idle] by default, [RetouchMotionEvent.Down] at first contact,
     * [RetouchMotionEvent.Move] while dragging and [RetouchMotionEvent.Up] when first pointer is up
     */
    var motionEvent by mutableStateOf(RetouchMotionEvent.Idle)

    /**
     * Current position of the pointer that is pressed or being moved
     */
    var currentPosition by mutableStateOf(Offset.Unspecified)

    /**
     * Previous motion event before next touch is saved into this current position.
     */
    var previousPosition by mutableStateOf(Offset.Unspecified)

    /**
     * Path that is being drawn between [RetouchMotionEvent.Down] and [RetouchMotionEvent.Up]. When
     * pointer is up this path is saved to **paths** and new instance is created
     */
    var currentPath by mutableStateOf(Path())

    /**
     * Points of drawing path. Value of this point is presented in range [0 - 1] instead of position
     **/
    var currentPathPoints = mutableListOf<Offset>()

    /**
     * Properties of path that is currently being drawn between
     * [RetouchMotionEvent.Down] and [RetouchMotionEvent.Up].
     */
    var currentPathProperty by mutableStateOf(RetouchPathProperties())

    /**
     * Reset drawing state
     */
    fun reset() {
        paths.clear()
        pathsUndone.clear()
        currentPath = Path()
        currentPathPoints.clear()
        currentPathProperty = RetouchPathProperties()
    }

    fun setDrawingSurfaceSize(width: Int, height: Int) {
        surfaceSize = Size(width, height)
    }

    /**
     * Create a bitmap from drawn paths and canvas size
     * Mask if drawn in White Color
     */
    fun createBitmap(size: Size): Bitmap {
        try {
            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val scale = if (surfaceSize.width == 0 || surfaceSize.height == 0) 1f else
                max(size.width.toFloat() / surfaceSize.width, size.height.toFloat() / surfaceSize.height)
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

                val pathPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = property.strokeWidth * scale
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }

                canvas.drawPath(path.asAndroidPath(), pathPaint)
            }

            tryWithoutCatch {
                val path = Path().apply {
                    currentPathPoints.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x * size.width, point.y * size.height)
                        else quadraticTo(
                            (currentPathPoints[index - 1].x + point.x) / 2 * size.width,
                            (currentPathPoints[index - 1].y + point.y) / 2 * size.height,
                            point.x * size.width,
                            point.y * size.height
                        )
                    }
                }

                val pathPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = currentPathProperty.strokeWidth * scale
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }

                canvas.drawPath(path.asAndroidPath(), pathPaint)
            }

            return bitmap
        } catch (ex: Exception) {
            ex.printStackTrace()
            return Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        }
    }
}