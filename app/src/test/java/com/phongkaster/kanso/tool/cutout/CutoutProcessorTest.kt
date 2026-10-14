package com.phongkaster.kanso.tool.cutout

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.cutout.CutoutProcessor
import com.phongkaster.kanso.data.edit.cutout.Segmenter
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 6, 17 and 22 for Remove background's processor, with a fake segmenter in place of the
 * engine (which cannot load in a JVM test).
 *
 * The photo is 200 x 200: its left half red, its right half blue, so "kept its colour" is checked
 * on two colours. The fake mask is 1 on a disc of radius 60 around the centre and 0 elsewhere.
 * Pixels are read straight from the result bitmap.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CutoutProcessorTest {

    /**
     * Outside the disc every pixel has alpha 0; well inside it every pixel has alpha 255 and the
     * photo's own colour; the result has an alpha channel; progress ends at 1.
     */
    @Test
    fun background_becomes_transparent_and_the_subject_keeps_its_colour() = runTest {
        val photo = twoHalves(SIZE, SIZE)
        val progress = mutableListOf<Float>()
        val result = CutoutProcessor(FakeSegmenter { discMask(it.width, it.height) })
            .process(photo) { progress += it }

        val cutout = (result as? EditResult.Edited)?.bitmap
        assertNotNull("Expected an edited picture, got $result", cutout)
        cutout!!
        assertEquals(SIZE, cutout.width)
        assertEquals(SIZE, cutout.height)
        assertTrue("The cut-out has no alpha channel", cutout.hasAlpha())
        val centre = SIZE / 2f
        for (y in 0 until SIZE step 2) {
            for (x in 0 until SIZE step 2) {
                val distance = Math.hypot((x + 0.5 - centre), (y + 0.5 - centre))
                val pixel = cutout.getPixel(x, y)
                if (distance > RADIUS + EDGE_MARGIN) {
                    assertEquals("alpha at ($x, $y), outside the disc", 0, Color.alpha(pixel))
                } else if (distance < RADIUS - EDGE_MARGIN) {
                    assertEquals("alpha at ($x, $y), inside the disc", 255, Color.alpha(pixel))
                    assertEquals("colour at ($x, $y)", photo.getPixel(x, y), pixel)
                }
            }
        }
        assertEquals(1f, progress.last(), 0f)
        assertTrue("Progress went backwards: $progress", progress.zipWithNext().all { (a, b) -> a <= b })
    }

    /** A segmenter that finds nobody (null) gives the "no subject" failure. */
    @Test
    fun no_mask_is_no_subject() = runTest {
        val result = CutoutProcessor(FakeSegmenter { null }).process(twoHalves(SIZE, SIZE)) {}
        assertEquals(EditResult.Failed(EditFailure.NoSubject), result)
    }

    /** A mask that is background everywhere also gives the "no subject" failure. */
    @Test
    fun an_all_background_mask_is_no_subject() = runTest {
        val result = CutoutProcessor(FakeSegmenter { FloatArray(it.width * it.height) })
            .process(twoHalves(SIZE, SIZE)) {}
        assertEquals(EditResult.Failed(EditFailure.NoSubject), result)
    }

    /** An out-of-memory error thrown by the engine step gives the "out of memory" failure. */
    @Test
    fun out_of_memory_in_the_segmenter_is_out_of_memory() = runTest {
        val result = CutoutProcessor(FakeSegmenter { throw OutOfMemoryError("fake") })
            .process(twoHalves(SIZE, SIZE)) {}
        assertEquals(EditResult.Failed(EditFailure.OutOfMemory), result)
    }

    /** Any other exception from the engine step means the model is unavailable. */
    @Test
    fun another_exception_in_the_segmenter_is_model_unavailable() = runTest {
        val result = CutoutProcessor(FakeSegmenter { throw IllegalStateException("model would not load") })
            .process(twoHalves(SIZE, SIZE)) {}
        assertEquals(EditResult.Failed(EditFailure.ModelUnavailable), result)
    }

    /** A cancellation is passed on, never turned into a failure. */
    @Test
    fun a_cancellation_is_passed_on() = runTest {
        var caught: Throwable? = null
        try {
            CutoutProcessor(FakeSegmenter { throw CancellationException("left the screen") })
                .process(twoHalves(SIZE, SIZE)) {}
        } catch (cancelled: CancellationException) {
            caught = cancelled
        }
        assertNotNull("The cancellation did not come through", caught)
    }

    /**
     * DoD 22's cap test: given a 6000 x 4000 photo, the segmenter receives a picture whose longest
     * edge is at most [WorkingSize.REMOVE_BACKGROUND], and the result has that size too. The photo
     * is RGB_565 (2 bytes a pixel, 48 MB) to spare the test's memory; the shrinking does not care.
     */
    @Test
    fun a_huge_photo_is_shrunk_before_the_segmenter_sees_it() = runTest {
        val huge = Bitmap.createBitmap(6000, 4000, Bitmap.Config.RGB_565)
        var received: Pair<Int, Int>? = null
        val result = CutoutProcessor(
            FakeSegmenter {
                received = it.width to it.height
                discMask(it.width, it.height)
            },
        ).process(huge) {}
        huge.recycle()

        val (width, height) = checkNotNull(received) { "The segmenter was never called" }
        assertTrue("The segmenter got $width x $height", max(width, height) <= WorkingSize.REMOVE_BACKGROUND)
        val cutout = (result as? EditResult.Edited)?.bitmap
        assertNotNull("Expected an edited picture, got $result", cutout)
        assertEquals(width, cutout!!.width)
        assertEquals(height, cutout.height)
    }

    /**
     * A [Segmenter] that answers with whatever [answer] returns for the photo it is given, so each
     * test decides the mask, or what is thrown.
     */
    private class FakeSegmenter(private val answer: (Bitmap) -> FloatArray?) : Segmenter {

        /** Returns [answer]'s mask for [photo]. */
        override suspend fun segment(photo: Bitmap): FloatArray? = answer(photo)
    }

    /** Fixtures and values the tests share. */
    private companion object {
        /** The side of the test photo, in pixels. */
        const val SIZE = 200

        /** The radius of the fake mask's disc, in pixels. */
        const val RADIUS = 60.0

        /** Pixels this close to the disc's edge are not checked: the edge may be soft. */
        const val EDGE_MARGIN = 3.0

        /** A mask of [width] x [height]: 1 inside a disc of [RADIUS] around the centre, 0 outside. */
        fun discMask(width: Int, height: Int): FloatArray {
            val cx = width / 2f
            val cy = height / 2f
            val radius = RADIUS * minOf(width, height) / SIZE
            return FloatArray(width * height) { i ->
                val x = i % width + 0.5
                val y = i / width + 0.5
                if (Math.hypot(x - cx, y - cy) <= radius) 1f else 0f
            }
        }

        /** A [width] x [height] opaque photo, red on its left half and blue on its right. */
        fun twoHalves(width: Int, height: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            paint.color = Color.rgb(200, 40, 30)
            canvas.drawRect(0f, 0f, width / 2f, height.toFloat(), paint)
            paint.color = Color.rgb(30, 60, 190)
            canvas.drawRect(width / 2f, 0f, width.toFloat(), height.toFloat(), paint)
            return bitmap
        }
    }
}
