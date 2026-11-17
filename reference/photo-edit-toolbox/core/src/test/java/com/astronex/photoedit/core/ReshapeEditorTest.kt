package com.astronex.photoedit.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.astronex.photoedit.core.reshape.FaceLocator
import com.astronex.photoedit.core.reshape.FakePoseLocator
import com.astronex.photoedit.core.reshape.FakeFaceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks Face and Body in [PhotoEditor] with fake finders: finding faces and people once per
 * photo, what the state reports, the reshape sliders as drags, and that Done applies the reshapes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ReshapeEditorTest {

    /**
     * A 200 x 160 photo of steep diagonal grey ramps (8 levels a pixel, repeating every 32 pixels), so a
     * move in any direction shows, even a fraction of a pixel: Double chin moves almost straight up,
     * and Smooth moves the outline of this small face by about a third of a pixel.
     */
    private val photo: Bitmap = Bitmap.createBitmap(200, 160, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val grey = (x + y) * 8 % 256
                setPixel(x, y, Color.rgb(grey, grey, grey))
            }
        }
    }

    private val filters = FilterTable.fromAssets { path ->
        ApplicationProvider.getApplicationContext<Context>().assets.open(path)
    }

    /** Runs the editor's work at once on the calling thread, so the state is final after each call. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun tearDown() {
        scope.cancel()
    }

    /** An editor with [faces] and [bodies] as its finders (null for none), its photo not yet loaded. */
    private fun editor(faces: FaceLocator? = FakeFaceLocator(), bodies: FakePoseLocator? = FakePoseLocator()) =
        PhotoEditor("content://sample/1", { _, _ -> photo }, filters, scope, Dispatchers.Unconfined, faces, bodies)

    /** Without finders (no Context given), Face and Body say they are unavailable. */
    @Test
    fun without_finders_both_are_unavailable() {
        val editor = editor(faces = null, bodies = null).also { it.load(1000) }
        editor.findFaces()
        editor.findBodies()
        assertEquals(Detection.Unavailable, editor.state.value.faces)
        assertEquals(Detection.Unavailable, editor.state.value.bodies)
    }

    /** A face is found once, with the box around it; asking again does not look again. */
    @Test
    fun faces_are_found_once() {
        val finder = FakeFaceLocator()
        val editor = editor(faces = finder).also { it.load(1000) }
        assertEquals(Detection.NotStarted, editor.state.value.faces)

        editor.findFaces()
        editor.findFaces()

        val found = editor.state.value.faces as Detection.Found
        assertEquals(1, found.count)
        // The fake face fills the middle half of the photo.
        assertEquals(0.25f, found.largest.left, 0.02f)
        assertEquals(0.75f, found.largest.right, 0.02f)
        assertEquals(1, finder.received.size)
    }

    /** No face and no person are reported as none found, not as a failure. */
    @Test
    fun nothing_found_is_reported() {
        val editor = editor(faces = FakeFaceLocator.none(), bodies = FakePoseLocator.none()).also { it.load(1000) }
        editor.findFaces()
        editor.findBodies()
        assertEquals(Detection.NoneFound, editor.state.value.faces)
        assertEquals(Detection.NoneFound, editor.state.value.bodies)
    }

    /** A finder that throws (a model that cannot load) makes the tool unavailable, never a crash. */
    @Test
    fun a_broken_model_is_unavailable() {
        val broken = FakeFaceLocator(facesFor = { throw IllegalStateException("no model") })
        val editor = editor(faces = broken).also { it.load(1000) }
        editor.findFaces()
        assertEquals(Detection.Unavailable, editor.state.value.faces)
    }

    /** Asked before the photo is decoded, the editor looks as soon as it is. */
    @Test
    fun asking_early_looks_once_the_photo_is_there() {
        val editor = editor()
        editor.findBodies()
        assertEquals(Detection.NotStarted, editor.state.value.bodies)
        editor.load(1000)
        assertTrue(editor.state.value.bodies is Detection.Found)
    }

    /** A reshape slider is a drag: the whole drag is one undo entry, and it marks its tool. */
    @Test
    fun a_reshape_is_a_drag() {
        val editor = editor().also { it.load(1000) }
        editor.findFaces()
        editor.setFaceReshape(FaceReshape.WIDTH, 0.3f)
        editor.setFaceReshape(FaceReshape.WIDTH, 0.6f)
        editor.commit()

        val settings = editor.state.value.settings
        assertEquals(0.6f, settings.amountOf(FaceReshape.WIDTH), 0f)
        assertFalse(settings.isNeutral(PhotoTool.FACE))
        assertTrue(settings.isNeutral(PhotoTool.BODY))
        assertTrue(editor.state.value.hasChanges)

        editor.undo()
        assertTrue(editor.state.value.settings.isNeutral)
        assertFalse(editor.state.value.canUndo)
    }

    /**
     * Each of the ten reshapes added in 0.3.0 works through the editor like Width: a drag sets it,
     * the reshape-only copy keeps it, undo clears it, redo brings it back, and Done changes the
     * picture.
     */
    @Test
    fun every_new_face_reshape_sets_undoes_redoes_and_renders() = runBlocking {
        val added = listOf(
            FaceReshape.EYE_SIZE,
            FaceReshape.NOSE_SIZE,
            FaceReshape.LIP_SIZE,
            FaceReshape.HEAD_SIZE,
            FaceReshape.JAWLINE,
            FaceReshape.CHIN_SHAPE,
            FaceReshape.SMOOTH,
            FaceReshape.CHIN_TIP,
            FaceReshape.JAW_ANGLE,
            FaceReshape.DOUBLE_CHIN,
        )
        added.forEach(
            action = { reshape ->
                val editor = editor().also { it.load(1000) }
                editor.findFaces()
                editor.setFaceReshape(reshape, 1f)
                editor.commit()

                val settings = editor.state.value.settings
                assertEquals("$reshape", 1f, settings.amountOf(reshape), 0f)
                assertEquals("$reshape", 1f, settings.reshapeOnly.amountOf(reshape), 0f)
                assertTrue("$reshape", settings.hasFaceReshape)
                assertFalse("$reshape", settings.isNeutral(PhotoTool.FACE))

                editor.undo()
                assertTrue("$reshape", editor.state.value.settings.isNeutral)
                editor.redo()
                assertEquals("$reshape", 1f, editor.state.value.settings.amountOf(reshape), 0f)

                val result = editor.done() as RenderResult.Edited
                val changed = (0 until photo.height).any(
                    predicate = { y -> (0 until photo.width).any(predicate = { x -> photo.getPixel(x, y) != result.bitmap.getPixel(x, y) }) },
                )
                assertTrue("$reshape changed no pixel", changed)
            },
        )
    }

    /** Values outside the slider's range are held to it. */
    @Test
    fun reshapes_are_clamped() {
        val editor = editor().also { it.load(1000) }
        editor.setBodyReshape(BodyReshape.WAIST, 3f)
        assertEquals(PhotoEdit.MAX, editor.state.value.settings.bodyWaist, 0f)
    }

    /** Done moves the pixels by the face reshape: beside the cheek the grey is no longer the same. */
    @Test
    fun done_applies_the_face_reshape() = runBlocking {
        val editor = editor().also { it.load(1000) }
        editor.findFaces()
        editor.setFaceReshape(FaceReshape.WIDTH, 1f)
        editor.commit()

        val result = editor.done() as RenderResult.Edited

        assertEquals(photo.width, result.bitmap.width)
        // A point just inside the left cheek (the outline is at x = 50) now shows grey from further out.
        assertNotEquals(photo.getPixel(55, 80), result.bitmap.getPixel(55, 80))
        // Far from the face nothing moved.
        assertEquals(photo.getPixel(5, 5), result.bitmap.getPixel(5, 5))
    }

    /** Done looks for the faces itself when a reshape is set but Face was never opened. */
    @Test
    fun done_finds_the_faces_when_needed() = runBlocking {
        val finder = FakeFaceLocator()
        val editor = editor(faces = finder).also { it.load(1000) }
        editor.change(PhotoEdit(faceWidth = 1f))

        val result = editor.done() as RenderResult.Edited

        assertEquals(1, finder.received.size)
        assertNotEquals(photo.getPixel(55, 80), result.bitmap.getPixel(55, 80))
    }

    /** With no face in the photo, a face reshape changes nothing and Done still gives a picture. */
    @Test
    fun a_reshape_with_no_face_changes_nothing() = runBlocking {
        val editor = editor(faces = FakeFaceLocator.none()).also { it.load(1000) }
        editor.findFaces()
        editor.change(PhotoEdit(faceWidth = 1f))

        val result = editor.done() as RenderResult.Edited

        assertEquals(photo.getPixel(55, 80), result.bitmap.getPixel(55, 80))
    }
}
