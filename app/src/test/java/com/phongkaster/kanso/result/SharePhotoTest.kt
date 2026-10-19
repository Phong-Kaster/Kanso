package com.phongkaster.kanso.result

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.ui.result.SHARE_CACHE_FOLDER
import com.phongkaster.kanso.ui.result.bitmapShareMimeType
import com.phongkaster.kanso.ui.result.shareMimeTypeFor
import com.phongkaster.kanso.ui.result.shareableBitmapUri
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks how Kanso hands a picture to another app.
 *
 * First [shareMimeTypeFor] (Run 8): a photo is shared as `image/png` when its provider reports
 * `image/png`, or reports nothing and its name ends in `.png` (a saved cut-out), and as
 * `image/jpeg`, Kanso's usual format, otherwise.
 *
 * Then, new in Run 9, [shareableBitmapUri] and [bitmapShareMimeType]: Share on the Result screen
 * sends the picture Done rendered, which is not in the gallery yet, as a file in the app's cache
 * folder `shared/` reached through FileProvider. The file is a PNG when the picture has see-through
 * pixels and a JPEG otherwise, the Uri names the app's own FileProvider, and every share replaces
 * the copy left by the one before, so the cache never grows.
 *
 * Run 9: moved from the `saved` test package to `result`, and now run under Robolectric, because
 * the Result screen replaced the Saved screen and its share writes a real file through Android's
 * Bitmap and FileProvider. Robolectric draws with the real graphics library here
 * ([GraphicsMode.Mode.NATIVE]), so the written bytes are a real PNG or JPEG.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SharePhotoTest {

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Stops Koin, which KansoApplication starts for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    // --- The type of a saved photo ------------------------------------------------------------

    /** A provider's own `image/png` wins, even over a `.jpg` name. */
    @Test
    fun a_reported_png_is_shared_as_png() {
        assertEquals("image/png", shareMimeTypeFor(reportedType = "image/png", fileName = "Kanso_1.jpg"))
        assertEquals("image/png", shareMimeTypeFor(reportedType = "image/png", fileName = null))
    }

    /** With no type reported, a `.png` name (any case) is shared as PNG. */
    @Test
    fun a_png_name_is_shared_as_png_when_nothing_is_reported() {
        assertEquals("image/png", shareMimeTypeFor(reportedType = null, fileName = "Kanso_20261014_120000.png"))
        assertEquals("image/png", shareMimeTypeFor(reportedType = null, fileName = "/storage/Pictures/Kanso/CUT.PNG"))
    }

    /**
     * Anything else is shared as JPEG: a reported JPEG, a `.jpg` name, no name at all, or a
     * report that is not an image type with a name that is not `.png`.
     */
    @Test
    fun everything_else_is_shared_as_jpeg() {
        assertEquals("image/jpeg", shareMimeTypeFor(reportedType = "image/jpeg", fileName = "Kanso_1.jpg"))
        assertEquals("image/jpeg", shareMimeTypeFor(reportedType = null, fileName = "Kanso_1.jpg"))
        assertEquals("image/jpeg", shareMimeTypeFor(reportedType = null, fileName = null))
        assertEquals("image/jpeg", shareMimeTypeFor(reportedType = "application/octet-stream", fileName = "42"))
    }

    /** A report that is not an image type is ignored, so a `.png` name still decides. */
    @Test
    fun a_non_image_report_falls_back_to_the_name() {
        assertEquals("image/png", shareMimeTypeFor(reportedType = "application/octet-stream", fileName = "Kanso_1.png"))
    }

    // --- The type of a rendered picture -------------------------------------------------------

    /** A rendered picture with an alpha channel (a cut-out) is shared as PNG, an opaque one as JPEG. */
    @Test
    fun a_rendered_picture_is_shared_as_png_with_alpha_and_as_jpeg_without() {
        assertEquals("image/png", bitmapShareMimeType(picture(hasAlpha = true)))
        assertEquals("image/jpeg", bitmapShareMimeType(picture(hasAlpha = false)))
    }

    // --- The cache copy Share sends -----------------------------------------------------------

    /**
     * An opaque picture is written as a JPEG into the cache's `shared/` folder, and the Uri handed
     * back is a `content://` Uri of the app's own FileProvider naming that very file.
     */
    @Test
    fun an_opaque_picture_is_written_as_a_jpeg_in_the_shared_cache_folder() {
        val uri = share(picture(hasAlpha = false))

        assertFileProviderUri(uri)
        val file = onlySharedFile()
        assertEquals(uri.lastPathSegment, file.name)
        assertTrue("The file ${file.name} is not a .jpg", file.name.endsWith(".jpg"))
        // Every JPEG file starts with the two bytes FF D8.
        assertArrayEquals(JPEG_MAGIC, file.readBytes().copyOf(JPEG_MAGIC.size))
    }

    /**
     * A picture with see-through pixels is written as a PNG, so the cut-out keeps its transparent
     * background in the app that receives it.
     */
    @Test
    fun a_picture_with_alpha_is_written_as_a_png_in_the_shared_cache_folder() {
        val uri = share(picture(hasAlpha = true))

        assertFileProviderUri(uri)
        val file = onlySharedFile()
        assertEquals(uri.lastPathSegment, file.name)
        assertTrue("The file ${file.name} is not a .png", file.name.endsWith(".png"))
        // Every PNG file starts with the eight bytes 89 'P' 'N' 'G' CR LF 1A LF.
        assertArrayEquals(PNG_MAGIC, file.readBytes().copyOf(PNG_MAGIC.size))
    }

    /**
     * A second share replaces the first copy: the folder then holds exactly one file, the one the
     * second Uri names, so sharing again and again never fills the cache.
     */
    @Test
    fun a_second_share_leaves_only_one_file_in_the_shared_cache_folder() {
        share(picture(hasAlpha = true))
        val second = share(picture(hasAlpha = false))

        val file = onlySharedFile()
        assertEquals(second.lastPathSegment, file.name)
    }

    /**
     * Run 9: the manifest declares the provider the share Uri names, so a real device can resolve it.
     * (The host tests cannot call FileProvider itself on Windows, see [HOST_URI].)
     */
    @Test
    fun the_manifest_declares_the_file_provider_the_share_uri_names() {
        val info = app.packageManager.resolveContentProvider(app.packageName + ".fileprovider", 0)
        assertNotNull("No provider with the authority ${app.packageName}.fileprovider", info)
        assertEquals("androidx.core.content.FileProvider", info!!.name)
        assertEquals(false, info.exported)
    }

    // --- Helpers -------------------------------------------------------------------------------

    /** Writes [bitmap] through [shareableBitmapUri] and returns its Uri, failing if there is none. */
    private fun share(bitmap: Bitmap): Uri {
        val uri = runBlocking { shareableBitmapUri(app, bitmap, HOST_URI) }
        assertNotNull("shareableBitmapUri wrote nothing", uri)
        return uri!!
    }

    /**
     * Checks that [uri] is a `content://` Uri of the app's FileProvider (the package name followed
     * by `.fileprovider`) under the `shared` path that `res/xml/file_paths.xml` declares.
     */
    private fun assertFileProviderUri(uri: Uri) {
        assertEquals("content", uri.scheme)
        assertEquals(app.packageName + ".fileprovider", uri.authority)
        assertEquals(SHARE_CACHE_FOLDER, uri.pathSegments.first())
    }

    /** Returns the one file in the cache's `shared/` folder, failing if there is not exactly one. */
    private fun onlySharedFile(): File {
        val files = File(app.cacheDir, SHARE_CACHE_FOLDER).listFiles().orEmpty().toList()
        assertEquals("The shared folder holds ${files.map { it.name }}", 1, files.size)
        return files.single()
    }

    /**
     * A small 40 x 30 picture painted one colour. [hasAlpha] sets whether Android treats it as
     * having see-through pixels: a new ARGB bitmap says yes by default, so an opaque one is told no.
     */
    private fun picture(hasAlpha: Boolean): Bitmap =
        Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).apply {
            eraseColor(if (hasAlpha) Color.argb(128, 30, 120, 200) else Color.rgb(30, 120, 200))
            setHasAlpha(hasAlpha)
        }

    /** Values shared by the tests. */
    private companion object {
        /** The first bytes of every PNG file. */
        val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

        /** The first bytes of every JPEG file. */
        val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
    }
}
