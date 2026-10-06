package com.phongkaster.kanso.tool

import android.content.Context
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.photo.LoadedPhoto
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.data.photo.displayMaxEdgePx
import com.phongkaster.kanso.data.photo.sampleSizeFor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks the tool screen's photo decoder (DoD 8 and 9): photos come out upright whatever their
 * EXIF orientation says, and a huge photo is shrunk while it is decoded, never read at full size.
 *
 * The JPEG fixtures are made at test time (see [ToolFixtures]) and decoded by Android's real
 * decoder, which Robolectric runs natively in GraphicsMode NATIVE.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PhotoLoaderTest {

    /** A fresh folder for the fixture files, deleted after each test. */
    @get:Rule
    val temp = TemporaryFolder()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    // --- Upright (DoD 8) ---------------------------------------------------------------------

    /** A 400 x 200 photo tagged "turn 90 degrees clockwise" comes out 200 x 400, left half on top. */
    @Test
    fun orientation_6_turns_a_400x200_photo_into_200x400() {
        val photo = loadFixture(orientation = ExifInterface.ORIENTATION_ROTATE_90)

        assertEquals(200, photo.bitmap.width)
        assertEquals(400, photo.bitmap.height)
        // Turned clockwise, the red left half ends up at the top and the blue right half below.
        assertRed(photo.bitmap.getPixel(100, 100))
        assertBlue(photo.bitmap.getPixel(100, 300))
    }

    /** A photo tagged "normal" is left as it is. */
    @Test
    fun orientation_1_leaves_the_photo_unchanged() {
        val photo = loadFixture(orientation = ExifInterface.ORIENTATION_NORMAL)

        assertEquals(400, photo.bitmap.width)
        assertEquals(200, photo.bitmap.height)
        assertRed(photo.bitmap.getPixel(100, 100))
        assertBlue(photo.bitmap.getPixel(300, 100))
    }

    /** A photo with no EXIF orientation at all is left as it is. */
    @Test
    fun no_orientation_tag_leaves_the_photo_unchanged() {
        val photo = loadFixture(orientation = null)

        assertEquals(400, photo.bitmap.width)
        assertEquals(200, photo.bitmap.height)
        assertRed(photo.bitmap.getPixel(100, 100))
    }

    /** A photo tagged "turn 270 degrees clockwise" comes out 200 x 400, left half at the bottom. */
    @Test
    fun orientation_8_turns_the_other_way() {
        val photo = loadFixture(orientation = ExifInterface.ORIENTATION_ROTATE_270)

        assertEquals(200, photo.bitmap.width)
        assertEquals(400, photo.bitmap.height)
        assertBlue(photo.bitmap.getPixel(100, 100))
        assertRed(photo.bitmap.getPixel(100, 300))
    }

    /** A photo tagged "turn 180 degrees" keeps its size, with the halves swapped. */
    @Test
    fun orientation_3_turns_the_photo_half_way() {
        val photo = loadFixture(orientation = ExifInterface.ORIENTATION_ROTATE_180)

        assertEquals(400, photo.bitmap.width)
        assertEquals(200, photo.bitmap.height)
        assertBlue(photo.bitmap.getPixel(100, 100))
        assertRed(photo.bitmap.getPixel(300, 100))
    }

    /** A photo tagged "mirror left to right" keeps its size, with the halves swapped. */
    @Test
    fun orientation_2_mirrors_the_photo() {
        val photo = loadFixture(orientation = ExifInterface.ORIENTATION_FLIP_HORIZONTAL)

        assertEquals(400, photo.bitmap.width)
        assertEquals(200, photo.bitmap.height)
        assertBlue(photo.bitmap.getPixel(100, 100))
        assertRed(photo.bitmap.getPixel(300, 100))
    }

    // --- Never at full size (DoD 9) -----------------------------------------------------------

    /**
     * An 8000 x 6000 photo on a 411 x 891 dp xxhdpi phone (1233 x 2673 pixels) is decoded with
     * its longest edge at most twice the screen's longest edge, and smaller than the original.
     */
    @Test
    fun an_8000x6000_photo_is_shrunk_for_a_phone_screen() {
        val screenWidthPx = 411 * 3
        val screenHeightPx = 891 * 3
        val maxEdge = displayMaxEdgePx(screenWidthPx, screenHeightPx)
        assertEquals(2 * 2673, maxEdge)

        val sample = sampleSizeFor(8000, 6000, maxEdge)
        val decodedLongest = ceilDiv(8000, sample)

        assertTrue(
            "sample size $sample is not a power of two",
            sample > 1 && (sample and (sample - 1)) == 0,
        )
        assertTrue("decoded edge $decodedLongest exceeds $maxEdge", decodedLongest <= maxEdge)
        assertTrue("decoded edge $decodedLongest is the full size", decodedLongest < 8000)
        // The smallest such shrink is used, so the photo is not blurrier than needed.
        assertTrue(ceilDiv(8000, sample / 2) > maxEdge)
    }

    /** Small photos and odd inputs are decoded at full size. */
    @Test
    fun small_photos_and_odd_sizes_are_not_shrunk() {
        assertEquals(1, sampleSizeFor(400, 200, 5346))
        assertEquals(1, sampleSizeFor(5346, 10, 5346))
        assertEquals(2, sampleSizeFor(5347, 10, 5346))
        assertEquals(1, sampleSizeFor(0, 0, 5346))
        assertEquals(1, sampleSizeFor(400, 200, 0))
    }

    /** A real decode of a small generated JPEG honours the sample size the calculation gives. */
    @Test
    fun a_real_decode_honours_the_sample_size() {
        val maxEdge = 300
        val sample = sampleSizeFor(800, 600, maxEdge)
        assertEquals(4, sample)

        val file = ToolFixtures.writeJpeg(temp.newFile("big.jpg"), ToolFixtures.twoHalves(800, 600))
        val uri = ToolFixtures.serve(context, file, "big.jpg")
        val photo = load(uri, maxEdge)

        assertNotNull("The generated JPEG could not be decoded", photo)
        assertEquals(800 / sample, photo!!.bitmap.width)
        assertEquals(600 / sample, photo.bitmap.height)
    }

    // --- Unreadable --------------------------------------------------------------------------

    /** A Uri with nothing behind it gives null, not a crash. */
    @Test
    fun a_missing_photo_gives_null() {
        assertNull(load(ToolFixtures.MISSING_URI, 5346))
    }

    /** A file that is not an image gives null, not a crash. */
    @Test
    fun a_file_that_is_not_an_image_gives_null() {
        val file = temp.newFile("notes.jpg").apply { writeText("not a photo at all") }
        val uri = ToolFixtures.serve(context, file, "notes.jpg")
        assertNull(load(uri, 5346))
    }

    /** Text that is not a Uri at all gives null, not a crash. */
    @Test
    fun a_malformed_uri_gives_null() {
        assertNull(load("", 5346))
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Writes a 400 x 200 two-halves JPEG with the EXIF [orientation] (none when null), loads it
     * with the real loader and returns the result, failing if it could not be decoded.
     */
    private fun loadFixture(orientation: Int?): LoadedPhoto {
        val name = "fixture-${orientation ?: "none"}.jpg"
        val file = ToolFixtures.writeJpeg(
            temp.newFile(name),
            ToolFixtures.twoHalves(400, 200),
            orientation,
        )
        val uri = ToolFixtures.serve(context, file, name)
        val photo = load(uri, 10_000)
        assertNotNull("The fixture $name could not be decoded", photo)
        return photo!!
    }

    /** Loads [uri] with the real loader, waiting for the background decode to finish. */
    private fun load(uri: String, maxEdgePx: Int): LoadedPhoto? = runBlocking {
        PhotoLoader(context).load(uri, maxEdgePx)
    }

    /** Divides and rounds up, as the decoder does for a shrunk edge. */
    private fun ceilDiv(value: Int, divisor: Int): Int = (value + divisor - 1) / divisor

    /** Checks that [pixel] is clearly red (JPEG blurs colours a little). */
    private fun assertRed(pixel: Int) {
        assertTrue(
            "Expected red, got #${Integer.toHexString(pixel)}",
            Color.red(pixel) > 180 && Color.blue(pixel) < 90,
        )
    }

    /** Checks that [pixel] is clearly blue (JPEG blurs colours a little). */
    private fun assertBlue(pixel: Int) {
        assertTrue(
            "Expected blue, got #${Integer.toHexString(pixel)}",
            Color.blue(pixel) > 180 && Color.red(pixel) < 90,
        )
    }
}
