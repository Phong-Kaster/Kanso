package com.phongkaster.kanso.data.edit.retouch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.photo.editor.common.geometry.UnifyPoint
import com.photo.editor.localml.facelandmark.DetectFaceLandmarkUC
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The real [FaceFinder]: the engine's face-landmark model (`DetectFaceLandmarkUC` in
 * `core/localml`). The model file, `face_landmarker.task`, is bundled in the app, so nothing is
 * downloaded.
 *
 * The engine does not give face rectangles: it gives, for each face, a list of landmark points
 * (the corners of the eyes, the outline of the jaw, the lips and so on). This class turns each
 * face's points into a rectangle: the smallest box holding them all, widened by [PADDING] of its
 * size on every side (so the forehead and chin are smoothed too), and cut to the photo.
 *
 * The points come back as fractions of the photo's width and height (0 to 1), as far as the
 * engine's bytecode shows; to be safe, if any coordinate is above 1.5 the points are read as
 * pixels instead.
 *
 * [createEngine] builds the engine once, on the first photo (not when the app starts), and it is
 * then kept: loading the model takes a noticeable moment, and a person often moves the strength
 * slider several times on one photo. A [Mutex] makes sure two runs can never build two engines.
 * This class names engine classes and never runs in a JVM test; tests use a fake [FaceFinder].
 */
class EngineFaceFinder(private val createEngine: () -> DetectFaceLandmarkUC) : FaceFinder {

    /**
     * Builds the finder on [context]. The application context is used, so the engine never holds
     * on to a screen that has closed.
     */
    constructor(context: Context) : this({ DetectFaceLandmarkUC(context.applicationContext) })

    /** The engine, once built and its model loaded; null before the first photo. */
    private var engine: DetectFaceLandmarkUC? = null

    /** Lets only one run at a time build the engine and load its model. */
    private val engineLock = Mutex()

    /**
     * Runs the face-landmark model on [photo] and returns one padded rectangle per face, in
     * pixels of [photo]; empty when there is no face.
     */
    override suspend fun findFaces(photo: Bitmap): List<Rect> {
        val ready = readyEngine()
        val faces: List<List<UnifyPoint>> = ready.invoke(photo)
        return faces.mapNotNull { points -> boundsOf(points, photo.width, photo.height) }
    }

    /**
     * Returns the engine with its model loaded, building it on the first call.
     *
     * initModel() starts loading the model in the coroutine scope it is given and returns that
     * work's Job; join() waits until it is done, so the model is ready before the first photo,
     * as Retica waits for it too. `coroutineScope { }` gives it a scope that lives only as long
     * as this call.
     */
    private suspend fun readyEngine(): DetectFaceLandmarkUC = engineLock.withLock {
        engine ?: createEngine().also { built ->
            coroutineScope { built.initModel(this)?.join() }
            engine = built
        }
    }

    /**
     * Turns one face's landmark [points] into a padded rectangle within the [width] x [height]
     * photo, or null when there are no points or the box is empty.
     */
    private fun boundsOf(points: List<UnifyPoint>, width: Int, height: Int): Rect? {
        if (points.isEmpty()) return null
        // Fractions of the photo (0..1) are scaled to pixels; values well above 1 already are.
        val inPixels = points.any { it.x > PIXELS_ABOVE || it.y > PIXELS_ABOVE }
        val scaleX = if (inPixels) 1f else width.toFloat()
        val scaleY = if (inPixels) 1f else height.toFloat()
        val minX = points.minOf { it.x } * scaleX
        val maxX = points.maxOf { it.x } * scaleX
        val minY = points.minOf { it.y } * scaleY
        val maxY = points.maxOf { it.y } * scaleY
        val padX = (maxX - minX) * PADDING
        val padY = (maxY - minY) * PADDING
        val box = Rect(
            floor(minX - padX).toInt().coerceIn(0, width),
            floor(minY - padY).toInt().coerceIn(0, height),
            ceil(maxX + padX).toInt().coerceIn(0, width),
            ceil(maxY + padY).toInt().coerceIn(0, height),
        )
        return if (box.right > box.left && box.bottom > box.top) box else null
    }

    /** The finder's fixed numbers. */
    private companion object {
        /** How much wider and taller, on each side, the rectangle is than the landmarks' box. */
        const val PADDING = 0.1f

        /** A coordinate above this means the engine gave pixels, not fractions of the photo. */
        const val PIXELS_ABOVE = 1.5f
    }
}
