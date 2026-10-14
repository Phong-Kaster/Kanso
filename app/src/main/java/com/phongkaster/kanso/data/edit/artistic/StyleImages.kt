package com.phongkaster.kanso.data.edit.artistic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.random.Random

/**
 * Draws the style image of each [ArtisticStyle] in code, with Android's `Canvas`.
 *
 * The style-transfer model needs a picture to copy a style from. Kanso ships no painting by
 * anyone else: its style images are drawn here, from shapes and gradients. The same pictures are
 * exported once into `app/src/main/assets/styles/<id>.webp`; [EngineStylizer] reads that file
 * and falls back to drawing it here when the file is missing.
 *
 * Every drawing is deterministic: the "random" washes, tiles and strokes come from
 * `Random(style.ordinal)`, a random-number generator started from a fixed seed, which gives the
 * same sequence of numbers on every run. So the exported file and the fallback are the same
 * picture.
 */
object StyleImages {

    /** Width and height, in pixels, of every style image. */
    const val SIZE = 256

    /**
     * Returns a new [SIZE] x [SIZE] opaque picture of [style]: warm or cool colour washes, a tile
     * mosaic, or ink strokes on paper.
     */
    fun draw(style: ArtisticStyle): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val random = Random(style.ordinal)
        when (style) {
            ArtisticStyle.WARM_WASH -> drawWash(
                canvas,
                random,
                Color.rgb(255, 196, 120),
                Color.rgb(176, 42, 30),
                listOf(Color.rgb(255, 140, 0), Color.rgb(220, 60, 50), Color.rgb(250, 210, 80), Color.rgb(150, 30, 70)),
            )
            ArtisticStyle.COOL_WASH -> drawWash(
                canvas,
                random,
                Color.rgb(170, 220, 240),
                Color.rgb(30, 50, 140),
                listOf(Color.rgb(0, 150, 170), Color.rgb(60, 90, 220), Color.rgb(140, 100, 210), Color.rgb(120, 220, 200)),
            )
            ArtisticStyle.MOSAIC -> drawMosaic(canvas, random)
            ArtisticStyle.INK -> drawInk(canvas, random)
        }
        bitmap.setHasAlpha(false)
        return bitmap
    }

    /**
     * Paints a diagonal gradient from [light] to [dark], then layers soft, see-through round
     * blots of the [blots] colours on it, as wet watercolour washes would.
     */
    private fun drawWash(canvas: Canvas, random: Random, light: Int, dark: Int, blots: List<Int>) {
        val size = SIZE.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, size, size, light, dark, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, size, size, paint)
        repeat(WASH_BLOTS) {
            val colour = blots[random.nextInt(blots.size)]
            val x = random.nextFloat() * size
            val y = random.nextFloat() * size
            val radius = 30f + random.nextFloat() * 70f
            // Each blot fades from half see-through at its centre to fully see-through at its rim.
            paint.shader = RadialGradient(
                x,
                y,
                radius,
                Color.argb(140, Color.red(colour), Color.green(colour), Color.blue(colour)),
                Color.argb(0, Color.red(colour), Color.green(colour), Color.blue(colour)),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
        }
    }

    /** Paints a grid of small tiles in bright, randomly picked colours, with dark grout between them. */
    private fun drawMosaic(canvas: Canvas, random: Random) {
        val palette = listOf(
            Color.rgb(230, 57, 70),
            Color.rgb(241, 196, 15),
            Color.rgb(46, 134, 193),
            Color.rgb(39, 174, 96),
            Color.rgb(243, 156, 18),
            Color.rgb(142, 68, 173),
            Color.rgb(236, 240, 241),
        )
        canvas.drawColor(Color.rgb(40, 36, 32))
        val paint = Paint()
        val tile = SIZE / MOSAIC_TILES
        for (row in 0 until MOSAIC_TILES) {
            for (column in 0 until MOSAIC_TILES) {
                paint.color = palette[random.nextInt(palette.size)]
                // A gap of 2 pixels on each side leaves the dark grout showing between tiles.
                val left = column * tile + 2f
                val top = row * tile + 2f
                canvas.drawRect(left, top, left + tile - 4f, top + tile - 4f, paint)
            }
        }
    }

    /** Paints pale paper with dark, curved brush strokes of varying width across it. */
    private fun drawInk(canvas: Canvas, random: Random) {
        canvas.drawColor(Color.rgb(244, 236, 220))
        val size = SIZE.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        repeat(INK_STROKES) {
            val darkness = 10 + random.nextInt(40)
            paint.color = Color.argb(200 + random.nextInt(56), darkness, darkness, darkness + 10)
            paint.strokeWidth = 2f + random.nextFloat() * 12f
            // A stroke is a curve from one random point to another, bent toward a third one.
            val path = Path().apply {
                moveTo(random.nextFloat() * size, random.nextFloat() * size)
                quadTo(
                    random.nextFloat() * size,
                    random.nextFloat() * size,
                    random.nextFloat() * size,
                    random.nextFloat() * size,
                )
            }
            canvas.drawPath(path, paint)
        }
    }

    /** How many colour blots a wash gets. */
    private const val WASH_BLOTS = 24

    /** How many tiles the mosaic has along each side. */
    private const val MOSAIC_TILES = 16

    /** How many brush strokes the ink drawing gets. */
    private const val INK_STROKES = 40
}
