package com.astronex.photoedit.core.reshape

import com.astronex.photoedit.core.BodyReshape
import com.astronex.photoedit.core.PhotoEdit
import com.astronex.photoedit.core.FaceReshape
import kotlin.math.ceil
import kotlin.math.floor

/**
 * What the finders found in one original photo, [width] x [height] pixels: every face's landmarks
 * ([faces]) and every person's pose and mask ([bodies]), in pixels of that photo. Null means not
 * looked for yet (or the look failed); an empty list means looked for and none found.
 *
 * It turns the reshape amounts of an [PhotoEdit] into [WarpHandle]es for a picture of any size with
 * the original's shape: the pushes are worked out in the original's pixels and then scaled, which
 * is exact because every reshape measures itself on the face or body (a push's place, move and
 * reach all grow with the picture). So the live preview (the original, or the small filter copy)
 * and Done (the working size) move the same parts by the same share.
 */
internal data class ReshapeShapes(
    val width: Int,
    val height: Int,
    val faces: List<FaceMesh>? = null,
    val bodies: List<PoseLandmarks>? = null,
) {

    /**
     * The pushes that apply the reshapes of [settings], in pixels of a picture [targetWidth] pixels
     * wide with the original's shape. Empty when every reshape is at its middle or nothing was
     * found.
     */
    fun pushesFor(settings: PhotoEdit, targetWidth: Int): List<WarpHandle> =
        scaled(
            faces.orEmpty().flatMap(transform = { FaceWarps.pushesFor(it, settings.faceAmounts()) }) +
                bodies.orEmpty().flatMap(
                    transform = { BodyWarps.pushesFor(it, settings.bodyWaist, settings.bodyArms, settings.bodyChest, settings.bodyLegs) },
                ),
            targetWidth,
        )

    /**
     * The smallest box, in pixels of a [targetWidth] x [targetHeight] picture and cut to it, that
     * holds every push's circle of the reshapes [settings] has on (away from their middle), each at
     * its full amount: the most a live preview of them can change. A push's place and reach do not
     * depend on its amount, so this box stays the same while a slider moves, and its edges never
     * move; it changes only when a reshape is turned on or back to its middle. Null when nothing was
     * found or no reshape is on.
     *
     * Only the reshapes that are on count, so the preview's grid ([GridWarp]) stays fine over a
     * small feature: with every reshape counted, Head size's reach covered nearly the whole photo
     * and the grid became too coarse to show Eye and Lip size at their full strength.
     */
    fun reach(settings: PhotoEdit, targetWidth: Int, targetHeight: Int): Region? {
        // The reshapes that are on, at full: a push's place and reach are the same at any amount but 0.
        val full = FaceReshape.entries.filter(predicate = { settings.amountOf(it) != PhotoEdit.MIDDLE })
            .fold(PhotoEdit(), operation = { s, r -> s.withAmount(r, 1f) })
            .let(
                block = { start ->
                    BodyReshape.entries.filter(predicate = { settings.amountOf(it) != PhotoEdit.MIDDLE })
                        .fold(start, operation = { s, r -> s.withAmount(r, 1f) })
                },
            )
        val pushes = pushesFor(full, targetWidth)
        if (pushes.isEmpty()) return null
        val left = floor(pushes.minOf { it.centerX - it.radius }).toInt().coerceIn(0, targetWidth)
        val top = floor(pushes.minOf { it.centerY - it.radius }).toInt().coerceIn(0, targetHeight)
        val right = ceil(pushes.maxOf { it.centerX + it.radius }).toInt().coerceIn(0, targetWidth)
        val bottom = ceil(pushes.maxOf { it.centerY + it.radius }).toInt().coerceIn(0, targetHeight)
        return if (right > left && bottom > top) Region(left, top, right, bottom) else null
    }

    /** The Face amounts of these settings, as [FaceWarps] takes them. */
    private fun PhotoEdit.faceAmounts(): FaceWarpAmounts = FaceWarpAmounts(
        width = faceWidth,
        lift = faceLift,
        temple = faceTemple,
        cheekbones = faceCheekbones,
        chinLength = faceChinLength,
        hairline = faceHairline,
        eyeSize = faceEyeSize,
        noseSize = faceNoseSize,
        lipSize = faceLipSize,
        headSize = faceHeadSize,
        jawline = faceJawline,
        chinShape = faceChinShape,
        smooth = faceSmooth,
        chinTip = faceChinTip,
        jawAngle = faceJawAngle,
        doubleChin = faceDoubleChin,
    )

    /** [pushes], found in the original's pixels, in pixels of a picture [targetWidth] wide. */
    private fun scaled(pushes: List<WarpHandle>, targetWidth: Int): List<WarpHandle> {
        if (pushes.isEmpty() || targetWidth == width || width <= 0) return pushes
        val scale = targetWidth.toFloat() / width
        return pushes.map {
            it.copy(
                centerX = it.centerX * scale,
                centerY = it.centerY * scale,
                moveX = it.moveX * scale,
                moveY = it.moveY * scale,
                radius = it.radius * scale,
            )
        }
    }

    /** A box in whole pixels, from ([left], [top]) up to but not including ([right], [bottom]). */
    data class Region(val left: Int, val top: Int, val right: Int, val bottom: Int) {

        /** The box's width in pixels. */
        val width: Int get() = right - left

        /** The box's height in pixels. */
        val height: Int get() = bottom - top
    }

    /** Boxes around what was found. */
    companion object {

        /**
         * The box around the largest of [found] (each a list of points as x, y pairs in pixels of a
         * [width] x [height] photo), as fractions of the photo. Null when [found] is empty.
         */
        fun largestBox(found: List<List<Pair<Float, Float>>>, width: Int, height: Int): PhotoEdit.CropRect? =
            found
                .filter { it.isNotEmpty() }
                .map { points ->
                    PhotoEdit.CropRect(
                        left = (points.minOf { it.first } / width).coerceIn(0f, 1f),
                        top = (points.minOf { it.second } / height).coerceIn(0f, 1f),
                        right = (points.maxOf { it.first } / width).coerceIn(0f, 1f),
                        bottom = (points.maxOf { it.second } / height).coerceIn(0f, 1f),
                    )
                }
                .maxByOrNull { (it.right - it.left) * (it.bottom - it.top) }
    }
}
