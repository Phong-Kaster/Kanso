package com.phongkaster.kanso.data.edit.beautify

import android.graphics.Bitmap
import android.graphics.Rect
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.LivePreview
import com.phongkaster.kanso.data.edit.LivePreviewSource
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.body.BodyFinder
import com.phongkaster.kanso.data.edit.body.BodyLandmarks
import com.phongkaster.kanso.data.edit.body.BodyPreparer
import com.phongkaster.kanso.data.edit.body.BodyShape
import com.phongkaster.kanso.data.edit.face.FaceFocus
import com.phongkaster.kanso.data.edit.face.FaceFocusSource
import com.phongkaster.kanso.data.edit.face.FaceLandmarks
import com.phongkaster.kanso.data.edit.face.FaceAmounts
import com.phongkaster.kanso.data.edit.face.FaceShape
import com.phongkaster.kanso.data.edit.face.WarpPush
import com.phongkaster.kanso.data.edit.face.paddedFaceBox
import kotlin.math.ceil
import kotlin.math.floor
import com.phongkaster.kanso.data.edit.face.LandmarkFinder
import com.phongkaster.kanso.data.edit.face.LocalWarp
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * The Beautify tool's edit: every face in the photo made narrower or wider, as much as the Face
 * width slider asks for, and lifted, as much as the Face lift slider asks for; every person's
 * waist, arms and legs made slimmer or wider and chest fuller or flatter, as much as the Waist,
 * Arms, Legs and Chest sliders ask for; then the faces' skin
 * smoothed, as much as the Smoothing slider asks for.
 *
 * The steps, in order:
 * 1. Scale the photo down so its longest edge is at most [maxEdgePx] (2000 pixels by default,
 *    [WorkingSize.BEAUTIFY]), so a 24-megapixel photo never runs the phone out of memory.
 * 2. Reshape, in one warp ([LocalWarp]) so the photo is resampled once:
 *    - only when a face value ([width], [lift], [temple], [cheekbones], [chinLength],
 *      [hairline]) is not 0: ask [landmarks] for each
 *      face's 478 landmarks (MediaPipe's face landmarker in the app) and turn each face into
 *      pushes ([FaceShape]: every face tool's added point by point). No face: [EditFailure.NoFace];
 *    - only when [waist], [arms], [chest] or [legs] is not 0: ask [bodies] for each person's 33
 *      pose landmarks and mask (MediaPipe's pose landmarker in the app) and turn each body into
 *      pushes ([BodyShape]: Waist's, Arms', Chest's and Legs' together).
 *      No person: [EditFailure.NoPerson].
 * 3. Only when [strength] is above 0: ask [finder] for the faces' rectangles on the reshaped photo
 *    (the same face landmarks, as boxes). No face means nothing to smooth: the result is
 *    [EditFailure.NoFace] and the photo stays as it was. With smoothing at 0 no face is needed,
 *    so a Waist edit works on a photo whose face is too small to find.
 * 4. Smooth each face's rectangle with [smoother] (OpenCV's bilateral filter in the app), one
 *    face after another, giving a fully smoothed copy in which everything outside the faces is
 *    untouched.
 * 5. Blend that copy over the reshaped photo by [strength], pixel by pixel, here in Kotlin:
 *    `out = reshaped + strength x (smoothed - reshaped)` for each colour channel. Strength 0 gives
 *    the reshaped photo back exactly, strength 1 the fully smoothed copy, and anything between a
 *    mix. Outside the faces smoothed equals reshaped, so those pixels never change.
 * 6. Mark the result opaque (`setHasAlpha(false)`), so Save writes a JPEG.
 *
 * Like every [ToolProcessor] it never throws for a failed edit: running out of memory becomes
 * [EditFailure.OutOfMemory], any other error from the engine [EditFailure.ModelUnavailable], and a
 * cancellation (the person left the screen) is passed on unchanged. The work runs on
 * [dispatcher], a background one by default. The input bitmap is never changed.
 */
class BeautifyProcessor(
    private val finder: FaceFinder,
    private val landmarks: LandmarkFinder,
    private val smoother: SkinSmoother,
    private val bodies: BodyFinder,
    private val maxEdgePx: Int = WorkingSize.BEAUTIFY,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ToolProcessor, FaceFocusSource, LivePreviewSource, BodyPreparer {

    /**
     * Finds the largest face in [photo] with [landmarks] and returns the smallest box holding all
     * its points (forehead to chin, cheek to cheek), as fractions of [photo]. The largest face is
     * the one the person most likely wants to look at. Null when there is no face, and also when
     * the model fails: the zoom is a help, never a reason for the tool to fail.
     *
     * The landmarks it finds are kept for [livePreview] (the screen hands both the same shown
     * photo), so the first live frame does not wait for the model.
     */
    override suspend fun findFocus(photo: Bitmap): FaceFocus? = withContext(dispatcher) {
        try {
            val width = photo.width.toFloat()
            val height = photo.height.toFloat()
            facesOf(photo)
                .filter(predicate = { it.points.isNotEmpty() })
                .map(
                    transform = { face ->
                        FaceFocus(
                            left = face.points.minOf(selector = { it.x }) / width,
                            top = face.points.minOf(selector = { it.y }) / height,
                            right = face.points.maxOf(selector = { it.x }) / width,
                            bottom = face.points.maxOf(selector = { it.y }) / height,
                        )
                    },
                )
                .maxByOrNull(selector = { it.width * it.height })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (outOfMemory: OutOfMemoryError) {
            null
        } catch (missing: LinkageError) {
            null
        } catch (failed: Exception) {
            null
        }
    }

    /**
     * The live look while a slider is dragged, for [photo] (the unedited photo as shown on screen)
     * with the current [width], [lift], [waist], [arms], [chest], [legs] and [strength]. It is a recipe, not a picture: the screen draws it
     * on the graphics chip every frame (see [LivePreview]), so it is cheap to make:
     * 1. the faces' landmarks are found once per photo and kept ([facesOf]);
     * 2. the reshape is the same [FaceShape] and [BodyShape] pushes the full render uses, in pixels
     *    of [photo]; the bodies are found only while a body slider is not 0, then kept ([bodiesOf]);
     * 3. when smoothing is on, [photo] fully smoothed inside the face boxes is made the first time
     *    and kept ([smoothedOf]); the screen lays it over the photo [strength] opaque.
     *
     * Null when neither a face nor (with a body slider off 0) a person is found, or the look cannot be made; a
     * live look never fails the tool.
     */
    override suspend fun livePreview(photo: Bitmap): LivePreview? = withContext(dispatcher) {
        try {
            val amount = strength.let { if (it.isNaN()) 1f else it.coerceIn(0f, 1f) }
            val reshape = width.let { if (it.isNaN()) 0f else it.coerceIn(-1f, 1f) }
            val raise = lift.let { if (it.isNaN()) 0f else it.coerceIn(-1f, 1f) }
            val slim = waist.let { if (it.isNaN()) 0f else it.coerceIn(-1f, 1f) }
            val limbs = arms.let { if (it.isNaN()) 0f else it.coerceIn(-1f, 1f) }
            val bust = chest.let { if (it.isNaN()) 0f else it.coerceIn(-1f, 1f) }
            val stride = legs.let { if (it.isNaN()) 0f else it.coerceIn(-1f, 1f) }
            val shape = faceAmounts(reshape, raise)
            val faces = facesOf(photo)
            val people = if (slim != 0f || limbs != 0f || bust != 0f || stride != 0f) bodiesOf(photo) else emptyList()
            if (faces.isEmpty() && people.isEmpty()) return@withContext null
            ensureActive()
            val pushes = faces.flatMap(transform = { FaceShape.pushesFor(it, shape) }) +
                people.flatMap(transform = { BodyShape.pushesFor(it, slim, limbs, bust, stride) })
            val region = regionOf(boxesOf(faces, photo), pushes, photo) ?: return@withContext null
            LivePreview(
                region = region,
                pushes = pushes,
                smoothed = if (amount > 0f && faces.isNotEmpty()) smoothedOf(photo, faces) else null,
                smoothing = amount,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (outOfMemory: OutOfMemoryError) {
            null
        } catch (missing: LinkageError) {
            null
        } catch (failed: Exception) {
            null
        }
    }

    /**
     * Finds the bodies in [photo] and keeps them ([bodiesOf]), as the Body group opens, so the
     * first live look of a body slider does not wait for the pose model. A failure is dropped: the
     * first live look simply tries again.
     */
    override suspend fun prepareBodies(photo: Bitmap) {
        withContext(dispatcher) {
            try {
                bodiesOf(photo)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (outOfMemory: OutOfMemoryError) {
                // Nothing kept; the first live look will try again.
            } catch (missing: LinkageError) {
                // Nothing kept; the first live look will try again.
            } catch (failed: Exception) {
                // Nothing kept; the first live look will try again.
            }
        }
    }

    /** Forgets the kept landmarks, bodies and smoothed copy, so the last photo is not held on to. */
    override fun releasePreview() {
        keptFaces = null
        keptBodies = null
        keptSmoothed = null
    }

    /**
     * The landmarks of every face in [photo], found with [landmarks] the first time and kept for
     * that same bitmap afterwards (the shown photo does not change while the screen is open).
     */
    private suspend fun facesOf(photo: Bitmap): List<FaceLandmarks> {
        keptFaces?.let { (source, faces) -> if (source === photo) return faces }
        val faces = landmarks.findLandmarks(photo)
        keptFaces = photo to faces
        return faces
    }

    /**
     * The bodies in [photo], found with [bodies] the first time and kept for that same bitmap
     * afterwards, as [facesOf] keeps the faces. Each comes with its mask, which can be a few MB on
     * a large photo, so it is forgotten with the rest in [releasePreview].
     */
    private suspend fun bodiesOf(photo: Bitmap): List<BodyLandmarks> {
        keptBodies?.let { (source, people) -> if (source === photo) return people }
        val people = bodies.findBodies(photo)
        keptBodies = photo to people
        return people
    }

    /**
     * [photo] fully smoothed inside every face box ([boxesOf]) with [smoother], made the first
     * time and kept for that same bitmap. It is never recycled here: the screen may be drawing it.
     */
    private fun smoothedOf(photo: Bitmap, faces: List<FaceLandmarks>): Bitmap {
        keptSmoothed?.let { (source, smoothed) -> if (source === photo) return smoothed }
        val smoothed = boxesOf(faces, photo).fold(initial = photo, operation = { current, box -> smoother.smooth(current, box) })
        keptSmoothed = photo to smoothed
        return smoothed
    }

    /** One padded box per face ([paddedFaceBox]), the same boxes the full render smooths. */
    private fun boxesOf(faces: List<FaceLandmarks>, photo: Bitmap): List<Rect> =
        faces.mapNotNull(transform = { face -> paddedFaceBox(face, photo.width, photo.height) })

    /**
     * The smallest box, cut to [photo], holding every face box in [boxes] and every push's circle
     * in [pushes]: everything a live look can change. Null when it is empty.
     */
    private fun regionOf(boxes: List<Rect>, pushes: List<WarpPush>, photo: Bitmap): Rect? {
        val region = Rect()
        boxes.forEach(action = { region.union(it) })
        pushes.forEach(
            action = { push ->
                region.union(
                    floor(push.centerX - push.radius).toInt(),
                    floor(push.centerY - push.radius).toInt(),
                    ceil(push.centerX + push.radius).toInt(),
                    ceil(push.centerY + push.radius).toInt(),
                )
            },
        )
        return clampTo(region, photo.width, photo.height)
    }

    /** The landmarks of the last photo [facesOf] looked at, with that photo; null before any. */
    @Volatile
    private var keptFaces: Pair<Bitmap, List<FaceLandmarks>>? = null

    /** The bodies of the last photo [bodiesOf] looked at, with that photo; null before any. */
    @Volatile
    private var keptBodies: Pair<Bitmap, List<BodyLandmarks>>? = null

    /** The last photo [smoothedOf] smoothed, with its smoothed copy; null before any. */
    @Volatile
    private var keptSmoothed: Pair<Bitmap, Bitmap>? = null

    /**
     * How much of the smoothing to show, from 0 (none: the photo as it was) to 1 (all of it).
     * The panel's slider sets it just before a run; a value outside 0..1 is clamped when used.
     *
     * `@Volatile` makes a value written on the main thread visible at once to the background
     * thread that runs [process].
     */
    @Volatile
    var strength: Float = 1f

    /**
     * How much to reshape each face, from -1 (widest) through 0 (unchanged, the default) to 1
     * (narrowest, a smaller face), like Meitu's Width slider at -100..+100. The panel sets it just before a run;
     * a value outside -1..1 is clamped when used. At 0 no landmarks are looked for at all.
     */
    @Volatile
    var width: Float = 0f

    /**
     * How much to lift each face, from -1 (the jaw dropped the most) through 0 (unchanged, the
     * default) to 1 (lifted the most), like Meitu's Lift slider at -100..+100. The panel sets it just
     * before a run; a value outside -1..1 is clamped when used. With it and [width] both at 0 no landmarks are looked for at all.
     */
    @Volatile
    var lift: Float = 0f

    /**
     * How full each face's temples become, from -1 (hollowest) through 0 (unchanged, the default)
     * to 1 (fullest), like Meitu's Temple slider at -100..+100. Clamped to -1..1 when used.
     */
    @Volatile
    var temple: Float = 0f

    /**
     * How much slimmer each face becomes across the cheekbones, from -1 (wider) through 0
     * (unchanged, the default) to 1 (slimmest), like Meitu's Cheekbones slider. Clamped when used.
     */
    @Volatile
    var cheekbones: Float = 0f

    /**
     * How much shorter each face's chin becomes, from -1 (longer) through 0 (unchanged, the default)
     * to 1 (shortest), like Meitu's Chin length slider. Clamped when used.
     */
    @Volatile
    var chinLength: Float = 0f

    /**
     * How much lower each face's hairline sits, from -1 (higher) through 0 (unchanged, the default)
     * to 1 (lowest), like Meitu's Hairline slider. Clamped when used.
     */
    @Volatile
    var hairline: Float = 0f

    /**
     * Every face value the processor holds, clamped to -1..1, as one [FaceAmounts] for [FaceShape].
     * The caller passes [width] and [lift] already read and clamped; the newer tools' values are
     * read here.
     */
    private fun faceAmounts(width: Float, lift: Float): FaceAmounts = FaceAmounts(
        width = width,
        lift = lift,
        temple = clamped(temple),
        cheekbones = clamped(cheekbones),
        chinLength = clamped(chinLength),
        hairline = clamped(hairline),
    )

    /** [value] clamped to -1..1, NaN counting as 0: how every two-sided slider value is read. */
    private fun clamped(value: Float): Float = if (value.isNaN()) 0f else value.coerceIn(-1f, 1f)

    /**
     * How much to reshape each person's waist, from -1 (widest) through 0 (unchanged, the default)
     * to 1 (slimmest), like Meitu's Waist slider at -100..+100. The panel sets it just before a
     * run; a value outside -1..1 is clamped when used. At 0 the pose model is never loaded.
     */
    @Volatile
    var waist: Float = 0f

    /**
     * How much to reshape each person's arms, from -1 (thickest) through 0 (unchanged, the default)
     * to 1 (slimmest), like Meitu's Arms slider at -100..+100. The panel sets it just before a run;
     * a value outside -1..1 is clamped when used. With it and [waist] at 0 the pose model is never
     * loaded.
     */
    @Volatile
    var arms: Float = 0f

    /**
     * How much to reshape each person's chest, from -1 (flattest) through 0 (unchanged, the
     * default) to 1 (fullest), like Meitu's Chest slider at -100..+100. The panel sets it just
     * before a run; a value outside -1..1 is clamped when used. With every body slider at 0 the
     * pose model is never loaded.
     */
    @Volatile
    var chest: Float = 0f

    /**
     * How much to reshape each person's legs, from -1 (thickest) through 0 (unchanged, the default)
     * to 1 (slimmest), like Meitu's Legs slider at -100..+100. The panel sets it just before a run;
     * a value outside -1..1 is clamped when used. With every body slider at 0 the pose model is
     * never loaded.
     */
    @Volatile
    var legs: Float = 0f

    /**
     * Runs the six steps above on [input] and returns the result; [input] is not changed.
     * [onProgress] only ever moves forward, from 0 to 1.
     */
    override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult =
        withContext(dispatcher) {
            try {
                var reported = 0f
                // Passes on a progress value only if it moves the bar forward.
                val report: (Float) -> Unit = { value ->
                    val next = value.coerceIn(0f, 1f)
                    if (next >= reported) {
                        reported = next
                        onProgress(next)
                    }
                }
                // Read once, so a slider moved during the run cannot change it half way.
                val amount = strength.let { if (it.isNaN()) 1f else it.coerceIn(0f, 1f) }
                val reshape = width.let { if (it.isNaN()) 0f else it.coerceIn(-1f, 1f) }
                val raise = lift.let { if (it.isNaN()) 0f else it.coerceIn(-1f, 1f) }
                val slim = waist.let { if (it.isNaN()) 0f else it.coerceIn(-1f, 1f) }
                val limbs = arms.let { if (it.isNaN()) 0f else it.coerceIn(-1f, 1f) }
                val bust = chest.let { if (it.isNaN()) 0f else it.coerceIn(-1f, 1f) }
                val stride = legs.let { if (it.isNaN()) 0f else it.coerceIn(-1f, 1f) }
                report(0f)
                val small = WorkingSize.fit(input, maxEdgePx)
                report(FIT_DONE)
                // Each model runs only when its sliders are off 0; null means it found nothing.
                val shape = faceAmounts(reshape, raise)
                val facePushes = if (!shape.isNone) facePushes(small, shape) else emptyList()
                val bodyPushes = if (facePushes != null && (slim != 0f || limbs != 0f || bust != 0f || stride != 0f)) {
                    bodyPushes(small, slim, limbs, bust, stride)
                } else {
                    emptyList()
                }
                if (facePushes == null || bodyPushes == null) {
                    if (small !== input) small.recycle()
                    return@withContext EditResult.Failed(if (facePushes == null) EditFailure.NoFace else EditFailure.NoPerson)
                }
                val pushes = facePushes + bodyPushes
                val shaped = if (pushes.isEmpty()) small else LocalWarp.warp(small, pushes)
                report(SHAPE_DONE)
                // With smoothing at 0 there is nothing to smooth, so no face is needed at all.
                if (amount == 0f) {
                    val result = blend(shaped, shaped, 0f)
                    if (shaped !== small) shaped.recycle()
                    if (small !== input) small.recycle()
                    report(1f)
                    return@withContext EditResult.Edited(result)
                }
                val faces = finder.findFaces(shaped).mapNotNull { clampTo(it, shaped.width, shaped.height) }
                report(FIND_DONE)
                if (faces.isEmpty()) {
                    if (shaped !== small) shaped.recycle()
                    if (small !== input) small.recycle()
                    return@withContext EditResult.Failed(EditFailure.NoFace)
                }
                ensureActive()
                val smoothed = smoothEveryFace(shaped, faces) { done ->
                    report(FIND_DONE + (SMOOTH_DONE - FIND_DONE) * done / faces.size)
                }
                ensureActive()
                val result = blend(shaped, smoothed, amount)
                // The working copies are no longer needed; freeing them at once matters on a phone,
                // where each one can be a 2000-pixel picture (16 MB).
                if (smoothed !== shaped && smoothed !== input) smoothed.recycle()
                if (shaped !== small) shaped.recycle()
                if (small !== input) small.recycle()
                report(1f)
                EditResult.Edited(result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (outOfMemory: OutOfMemoryError) {
                EditResult.Failed(EditFailure.OutOfMemory)
            } catch (missing: LinkageError) {
                // A native library or the model's class could not load on this phone.
                EditResult.Failed(EditFailure.ModelUnavailable)
            } catch (failed: Exception) {
                EditResult.Failed(EditFailure.ModelUnavailable)
            }
        }

    /**
     * Returns the pushes that change every face in [photo] by [amounts] (not all 0 here), or null
     * when [landmarks] finds no face. All faces' pushes go into the one warp, so the photo is
     * resampled only once. [photo] is not changed.
     */
    private suspend fun facePushes(photo: Bitmap, amounts: FaceAmounts): List<WarpPush>? {
        val found = landmarks.findLandmarks(photo)
        if (found.isEmpty()) return null
        return found.flatMap(transform = { face -> FaceShape.pushesFor(face, amounts) })
    }

    /**
     * Returns the pushes that change every person's waist by [waist], arms by [arms], chest by
     * [chest] and legs by [legs] in [photo] (-1..1, not all 0 here), or null when [bodies] finds no
     * person. [photo] is not changed.
     */
    private suspend fun bodyPushes(photo: Bitmap, waist: Float, arms: Float, chest: Float, legs: Float): List<WarpPush>? {
        val people = bodies.findBodies(photo)
        if (people.isEmpty()) return null
        return people.flatMap(transform = { body -> BodyShape.pushesFor(body, waist, arms, chest, legs) })
    }

    /**
     * Smooths each rectangle of [faces] on [photo], one after another, and returns the copy with
     * all of them smoothed. [onFaceDone] is told how many faces are done after each one. [photo]
     * is not changed; the in-between copies are freed as soon as the next one exists.
     */
    private fun smoothEveryFace(photo: Bitmap, faces: List<Rect>, onFaceDone: (Int) -> Unit): Bitmap {
        var current = photo
        faces.forEachIndexed { index, face ->
            val next = smoother.smooth(current, face)
            check(next.width == photo.width && next.height == photo.height) {
                "The smoother returned ${next.width} x ${next.height} for a ${photo.width} x ${photo.height} photo"
            }
            // Free the previous face's copy, but never the photo itself.
            if (current !== photo && current !== next) current.recycle()
            current = next
            onFaceDone(index + 1)
        }
        return current
    }

    /**
     * Returns a new opaque bitmap mixing [original] and [smoothed] (the same size) by [amount]:
     * each colour channel is `original + amount x (smoothed - original)`, rounded. At amount 0
     * every pixel equals [original]'s exactly.
     */
    private fun blend(original: Bitmap, smoothed: Bitmap, amount: Float): Bitmap {
        val width = original.width
        val height = original.height
        val before = IntArray(width * height)
        val after = IntArray(width * height)
        original.getPixels(before, 0, width, 0, 0, width, height)
        smoothed.getPixels(after, 0, width, 0, 0, width, height)
        for (i in before.indices) {
            val o = before[i]
            val s = after[i]
            val r = mix((o shr 16) and CHANNEL, (s shr 16) and CHANNEL, amount)
            val g = mix((o shr 8) and CHANNEL, (s shr 8) and CHANNEL, amount)
            val b = mix(o and CHANNEL, s and CHANNEL, amount)
            // Alpha is forced to fully opaque: a beautified photo has no see-through pixels.
            before[i] = (OPAQUE_ALPHA shl 24) or (r shl 16) or (g shl 8) or b
        }
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(before, 0, width, 0, 0, width, height)
        out.setHasAlpha(false)
        return out
    }

    /** One channel's mix: [from] moved toward [to] by [amount] (0..1), rounded to 0..255. */
    private fun mix(from: Int, to: Int, amount: Float): Int =
        (from + amount * (to - from)).roundToInt().coerceIn(0, CHANNEL)

    /**
     * Returns [face] cut to the [width] x [height] picture, or null when nothing of it lies inside
     * (a finder must not make the processor read outside the photo).
     */
    private fun clampTo(face: Rect, width: Int, height: Int): Rect? {
        val clamped = Rect(
            face.left.coerceIn(0, width),
            face.top.coerceIn(0, height),
            face.right.coerceIn(0, width),
            face.bottom.coerceIn(0, height),
        )
        return if (clamped.right > clamped.left && clamped.bottom > clamped.top) clamped else null
    }

    /** The processor's fixed numbers. */
    private companion object {
        /** The progress once the photo has been scaled to the working size. */
        const val FIT_DONE = 0.05f

        /** The progress once the faces have been reshaped (or there was nothing to reshape). */
        const val SHAPE_DONE = 0.25f

        /** The progress once the faces have been found. */
        const val FIND_DONE = 0.4f

        /** The progress once every face has been smoothed; the blend fills the rest. */
        const val SMOOTH_DONE = 0.9f

        /** The largest value of one colour channel, also the mask that reads one from a pixel. */
        const val CHANNEL = 0xFF

        /** The alpha of a fully opaque pixel. */
        const val OPAQUE_ALPHA = 0xFF
    }
}
