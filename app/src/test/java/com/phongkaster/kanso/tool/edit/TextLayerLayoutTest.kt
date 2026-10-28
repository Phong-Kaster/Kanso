package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.AppLayerPictures
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.TextAlignment
import com.phongkaster.kanso.data.edit.edit.TextFont
import com.phongkaster.kanso.data.edit.edit.TextLayerLayout
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Proves how a text layer is laid out and drawn (Run 11, ADR-005, Android 15, real fonts): its
 * block's shape does not depend on the width it is drawn at, long words wrap at the text's wrap
 * width, the alignment never changes the block's size, a blank text draws nothing, every bundled
 * font draws Vietnamese, and Done's processor draws a text into the photo.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class TextLayerLayoutTest {

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** The app's layout, with the bundled fonts. */
    private val layout by lazy { TextLayerLayout.forApp(context) }

    /** A sample text in [font] with [words], wrapping at 6 letter heights. */
    private fun text(words: String, font: TextFont = TextFont.NONE, alignment: TextAlignment = TextAlignment.CENTRE) =
        LayerContent.Text(words, 0xFFE53935.toInt(), font, alignment, wrapEms = 6f)

    /** Drawn at 200 or at 600 pixels wide, a text has the same shape (height over width). */
    @Test
    fun a_texts_shape_does_not_depend_on_its_size() {
        val content = text("Xin chào\nthế giới")
        val small = layout.render(content, 200)!!
        val large = layout.render(content, 600)!!
        assertEquals(small.height / small.width.toFloat(), large.height / large.width.toFloat(), 0.01f)
        assertEquals(layout.measure(content).aspect, large.height / large.width.toFloat(), 0.01f)
    }

    /** Words longer than the wrap width break into more lines, and the block stays within the wrap. */
    @Test
    fun long_words_wrap_at_the_wrap_width() {
        val one = layout.measure(text("Hi"))
        val many = layout.measure(text("The quick brown fox jumps over the lazy dog"))
        assertTrue("the long text did not wrap", many.heightEms > 2 * one.heightEms)
        assertTrue("the block ${many.widthEms} is wider than its wrap", many.widthEms <= 6f + 2 * TextLayerLayout.PADDING_EMS + 0.01f)
    }

    /** Left, centre and right give one and the same block. */
    @Test
    fun the_alignment_never_changes_the_block() {
        val words = "Happy birthday\nMinh Anh!"
        val blocks = TextAlignment.entries.map { layout.measure(text(words, alignment = it)) }
        assertEquals(1, blocks.toSet().size)
    }

    /** A blank text has nothing to draw. */
    @Test
    fun a_blank_text_draws_nothing() {
        assertNull(layout.render(text(""), 300))
        assertNull(layout.render(text("  \n "), 300))
    }

    /** Every font, the phone's own included, draws Vietnamese words: the bitmap holds coloured pixels. */
    @Test
    fun every_font_draws_vietnamese() {
        TextFont.entries.forEach { font ->
            val bitmap = layout.render(text("Tiếng Việt ữ ộ ằ", font), 400)
            assertNotNull("$font drew nothing", bitmap)
            assertTrue("$font left the bitmap clear", inkOf(bitmap!!) > 0)
        }
    }

    /** Done's processor draws a red text into a grey photo: red pixels appear where it sits. */
    @Test
    fun the_processor_draws_a_text_into_the_photo() {
        val photo = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(GREY) }
        val layer = Layer(1L, text("HELLO"), 0.5f, 0.5f, width = 0.6f, degrees = 0f, mirrored = false)
        val processor = EditProcessor(EditSettings(layers = listOf(layer)), FilterLut.Source { null }, AppLayerPictures(context))
        val result = runBlocking { processor.process(photo) {} }
        val edited = (result as EditResult.Edited).bitmap
        val pixels = IntArray(edited.width * edited.height).also { edited.getPixels(it, 0, edited.width, 0, 0, edited.width, edited.height) }
        assertTrue("no red letter was drawn", pixels.any { (it shr 16 and 0xFF) > 180 && (it shr 8 and 0xFF) < 120 })
    }

    /** How many pixels of [bitmap] are not fully clear. */
    private fun inkOf(bitmap: Bitmap): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { it ushr 24 != 0 }
    }

    /** Values shared by the tests. */
    private companion object {
        /** The photo's flat grey. */
        const val GREY = 0xFF646464.toInt()
    }
}
