package com.phongkaster.kanso.data.edit.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Lays out and draws the words of a [LayerContent.Text], so a text can be drawn exactly like a
 * sticker picture (ADR-005): everything that draws a layer asks for a bitmap of it at some width,
 * and this class makes that bitmap for a text.
 *
 * **StaticLayout.** Android's `StaticLayout` breaks text into lines for a given width and draws
 * them. It knows the font's real letter widths, line breaks, accents and Vietnamese marks, so the
 * same words always break in the same places.
 *
 * **The shape never depends on the size.** The words are always laid out at one reference letter
 * height ([REFERENCE_PX]) with the text's own wrap width ([LayerContent.Text.wrapEms], counted in
 * letter heights), and only then scaled to the width asked for. So a text's block (its width and
 * height in letter heights, [Block]) depends only on its words, font and alignment: a pinch or
 * the size slider scales the block like a picture, and the lines stay where they are.
 *
 * A thin margin ([PADDING_EMS]) surrounds the lines, so the swash of a script letter or an accent
 * above a capital is never cut off at the block's edge.
 *
 * [typefaceOf] gives the Android typeface of a [TextFont]; [forApp] builds one that loads the
 * bundled fonts from the app's resources.
 */
class TextLayerLayout(private val typefaceOf: (TextFont) -> Typeface) {

    /**
     * The size of a text's block counted in letter heights (multiples of the text size), margin
     * included: [widthEms] across and [heightEms] down.
     */
    data class Block(val widthEms: Float, val heightEms: Float) {

        /** The block's height divided by its width, the shape a frame or a bitmap of it has. */
        val aspect: Float
            get() = heightEms / widthEms
    }

    /** Recent measurements, so a frame redrawn every frame does not lay the words out again. */
    private val blocks = object : LinkedHashMap<Pair<LayerContent.Text, String>, Block>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<LayerContent.Text, String>, Block>?) =
            size > CACHE_SIZE
    }

    /**
     * The block [content] takes when it shows [words] (its own words by default; the Text sheet
     * passes the "Your text" hint for a text that is still empty, so the empty text still has a
     * frame). Words with nothing in them give a block one letter wide and one line tall.
     */
    fun measure(content: LayerContent.Text, words: String = content.text): Block {
        val key = content to words
        synchronized(blocks) { blocks[key]?.let { return it } }
        val layout = layout(content, words)
        val block = Block(
            widthEms = (layout.width + 2 * PADDING_PX) / REFERENCE_PX,
            heightEms = (layout.height + 2 * PADDING_PX) / REFERENCE_PX,
        )
        synchronized(blocks) { blocks[key] = block }
        return block
    }

    /**
     * Draws [content] showing [words] (its own words by default) into a new bitmap about
     * [widthPx] wide, its height following the block's shape, on a clear background, or returns
     * null when there is nothing to draw (blank words) or not enough memory. [alpha] (0..1)
     * fades the letters, which the Text sheet uses for the faint "Your text" hint.
     *
     * A very tall block at a large width could need a huge bitmap; it is drawn smaller instead
     * (never more than [MAX_PIXELS] pixels), and the caller, which always scales a layer's picture
     * to the exact size, stretches it back.
     */
    fun render(content: LayerContent.Text, widthPx: Int, words: String = content.text, alpha: Float = 1f): Bitmap? {
        if (words.isBlank() || widthPx <= 0) return null
        val block = measure(content, words)
        var width = widthPx.toFloat()
        var height = width * block.aspect
        if (width * height > MAX_PIXELS) {
            val shrink = sqrt(MAX_PIXELS / (width * height))
            width *= shrink
            height *= shrink
        }
        val w = width.roundToInt().coerceAtLeast(1)
        val h = height.roundToInt().coerceAtLeast(1)
        val layout = layout(content, words)
        return try {
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            // One uniform scale from the reference layout to the bitmap, after the margin.
            canvas.scale(w / (block.widthEms * REFERENCE_PX), h / (block.heightEms * REFERENCE_PX))
            canvas.translate(PADDING_PX, PADDING_PX)
            layout.paint.alpha = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
            layout.draw(canvas)
            bitmap
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    /**
     * The words laid out at the reference size: first at the wrap width, to find the longest
     * line, then again exactly that wide, so the alignment lines the lines up inside the block
     * rather than inside the wrap width.
     */
    private fun layout(content: LayerContent.Text, words: String): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = REFERENCE_PX
            typeface = typefaceOf(content.font)
            color = content.colorArgb
        }
        val wrap = ceil(content.wrapEms * REFERENCE_PX).toInt().coerceAtLeast(1)
        val first = StaticLayout.Builder.obtain(words, 0, words.length, paint, wrap)
            .setIncludePad(true)
            .build()
        val longest = (0 until first.lineCount).maxOfOrNull { first.getLineWidth(it) } ?: 0f
        val width = ceil(longest).toInt().coerceIn(REFERENCE_PX.toInt(), wrap.coerceAtLeast(REFERENCE_PX.toInt()))
        val alignment = when (content.alignment) {
            TextAlignment.CENTRE -> Layout.Alignment.ALIGN_CENTER
            TextAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
            TextAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }
        return StaticLayout.Builder.obtain(words, 0, words.length, paint, width)
            .setAlignment(alignment)
            .setIncludePad(true)
            .build()
    }

    /** The reference size, the app's own layout, and the limits. */
    companion object {
        /** The letter height (text size) the words are always laid out at, in pixels. */
        const val REFERENCE_PX = 100f

        /** The margin around the lines, in letter heights. */
        const val PADDING_EMS = 0.15f

        /** The margin in pixels at the reference size. */
        private const val PADDING_PX = PADDING_EMS * REFERENCE_PX

        /** The most pixels one rendered text may have (about 2000 x 2000). */
        private const val MAX_PIXELS = 4_000_000f

        /** How many measurements are remembered. */
        private const val CACHE_SIZE = 32

        /**
         * The app's layout: [TextFont.NONE] is the phone's default typeface, every other font is
         * loaded once from `res/font` and remembered. A font that cannot be loaded falls back to
         * the default typeface rather than failing the render.
         */
        fun forApp(context: Context): TextLayerLayout {
            val appContext = context.applicationContext
            val loaded = ConcurrentHashMap<TextFont, Typeface>()
            return TextLayerLayout { font ->
                loaded.getOrPut(font) {
                    val res = font.res ?: return@getOrPut Typeface.DEFAULT
                    try {
                        ResourcesCompat.getFont(appContext, res) ?: Typeface.DEFAULT
                    } catch (_: Exception) {
                        Typeface.DEFAULT
                    }
                }
            }
        }
    }
}
