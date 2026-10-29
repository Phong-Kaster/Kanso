package com.phongkaster.kanso.data.edit.face

import android.graphics.Rect
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The box Beautify smooths for one face: the smallest box holding all its landmarks, widened by
 * [PADDING] of its size on every side (so the forehead and chin are in it), cut to the
 * [width] x [height] photo. Null when there are no points or nothing of the box lies in the photo.
 *
 * It is the same box the engine's face finder used to give, now made from the landmarks Face
 * width already found, so Beautify runs only one face model.
 */
fun paddedFaceBox(face: FaceLandmarks, width: Int, height: Int): Rect? {
    if (face.points.isEmpty()) return null
    val minX = face.points.minOf(selector = { it.x })
    val maxX = face.points.maxOf(selector = { it.x })
    val minY = face.points.minOf(selector = { it.y })
    val maxY = face.points.maxOf(selector = { it.y })
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

/** How much wider and taller, on each side, a face box is than its landmarks. */
private const val PADDING = 0.1f
