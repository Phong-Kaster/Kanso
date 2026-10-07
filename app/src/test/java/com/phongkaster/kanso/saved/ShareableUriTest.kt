package com.phongkaster.kanso.saved

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.ui.saved.shareableUri
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * Checks [shareableUri] on Android 9, where Save returns a `file://` Uri that must never be
 * handed to another app: a file MediaStore knows gives its row's `content://` Uri, a content Uri
 * is kept as it is, and anything that cannot be turned into a content Uri gives null.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class ShareableUriTest {

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Stops Koin, which KansoApplication starts for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** A saved file MediaStore knows is shared as its MediaStore row, found by its path. */
    @Suppress("DEPRECATION")
    @Test
    fun a_file_uri_becomes_its_media_store_content_uri() {
        val provider = FakeImagesProvider.install()
        provider.files[SAVED_PATH] = 42L

        val result = runBlocking { shareableUri(app, "file://$SAVED_PATH") }

        assertEquals(Uri.parse("content://media/external/images/media/42"), result)
        val (queried, selection, argument) = provider.queries.single()
        assertEquals(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, queried)
        assertEquals("${MediaStore.Images.Media.DATA} = ?", selection)
        assertEquals(SAVED_PATH, argument)
    }

    /** A content Uri is already shareable and is returned unchanged, without asking MediaStore. */
    @Test
    fun a_content_uri_is_kept_as_it_is() {
        val provider = FakeImagesProvider.install()
        val uri = "content://media/external/images/media/7"

        val result = runBlocking { shareableUri(app, uri) }

        assertEquals(Uri.parse(uri), result)
        assertEquals(0, provider.queries.size)
    }

    /**
     * A file MediaStore does not know, and the scanner does not report in time, gives null: the
     * file Uri itself is never returned.
     */
    @Test
    fun an_unknown_file_gives_null_not_the_file_uri() {
        FakeImagesProvider.install()

        val result = runBlocking { shareableUri(app, "file://$SAVED_PATH", scanTimeoutMillis = 200) }

        assertNull(result)
    }

    /** A Uri of any other kind gives null. */
    @Test
    fun another_kind_of_uri_gives_null() {
        assertNull(runBlocking { shareableUri(app, "android.resource://com.phongkaster.kanso/1") })
    }

    /** Values shared by the tests. */
    private companion object {
        /** Where Android 9's Save puts a photo. */
        const val SAVED_PATH = "/storage/emulated/0/Pictures/Kanso/Kanso_20261007_120000.jpg"
    }
}
