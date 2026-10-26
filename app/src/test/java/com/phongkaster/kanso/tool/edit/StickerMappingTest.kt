package com.phongkaster.kanso.tool.edit

import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.StickerMapping
import com.phongkaster.kanso.data.edit.edit.StickerMapping.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Checks, on the plain JVM, that [StickerMapping] is exactly its own inverse (DoD 17): a point of
 * the picture the person sees, taken to the original photo and back, lands where it started, for
 * every combination of the four quarter turns, the four flips (none, horizontal, vertical, both)
 * and two crops (the whole photo, and an off-centre piece). It also checks that angles, mirror
 * flags and widths convert back and forth, and that a new sticker is centred in the shown picture
 * at a third of its width.
 *
 * The photo is 400 x 300 pixels and the off-centre crop is (0.1, 0.2) to (0.7, 0.9), whose edges
 * are whole pixels, so the mapping's rounding of the crop does not blur the checks.
 */
class StickerMappingTest {

    /** The two crops: the whole photo and an off-centre piece of it. */
    private val crops = listOf(
        EditSettings.CropRect.FULL,
        EditSettings.CropRect(0.1f, 0.2f, 0.7f, 0.9f),
    )

    /** Every combination of the quarter turns, the flips and the crops, as settings. */
    private fun allSettings(): List<EditSettings> {
        val all = mutableListOf<EditSettings>()
        for (crop in crops) {
            for (turns in 0..3) {
                for (flipH in listOf(false, true)) {
                    for (flipV in listOf(false, true)) {
                        all += EditSettings(crop = crop, quarterTurns = turns, flipHorizontal = flipH, flipVertical = flipV)
                    }
                }
            }
        }
        return all
    }

    /** A mapping for [settings] on the 400 x 300 photo. */
    private fun mappingFor(settings: EditSettings) = StickerMapping(PHOTO_WIDTH, PHOTO_HEIGHT, settings)

    /** A description of [settings] for a failure message. */
    private fun describe(s: EditSettings) =
        "turns=${s.quarterTurns} flipH=${s.flipHorizontal} flipV=${s.flipVertical} crop=${s.crop}"

    /** A grid of 25 points across a unit square, kept off the very edges. */
    private fun grid(): List<Point> =
        listOf(0.05f, 0.25f, 0.5f, 0.75f, 0.95f).flatMap { x -> listOf(0.05f, 0.25f, 0.5f, 0.75f, 0.95f).map { y -> Point(x, y) } }

    /** Fails unless [a] and [b] are the same point within [TOLERANCE]. */
    private fun assertSamePoint(message: String, a: Point, b: Point) {
        assertTrue("$message: $a is not $b", abs(a.x - b.x) <= TOLERANCE && abs(a.y - b.y) <= TOLERANCE)
    }

    /** The smallest difference between two angles in degrees, 0 to 180. */
    private fun angleGap(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return if (d > 180f) 360f - d else d
    }

    /** A shown point mapped to the original and back is the point it started as (25 points each). */
    @Test
    fun shown_to_original_to_shown_is_the_identity() {
        allSettings().forEach { settings ->
            val mapping = mappingFor(settings)
            grid().forEach { p ->
                assertSamePoint(describe(settings), p, mapping.originalToShown(mapping.shownToOriginal(p)))
            }
        }
    }

    /**
     * An original point inside the crop, mapped to the shown picture and back, is the point it
     * started as. The points are taken inside the kept rectangle, the only part of the photo that
     * is shown.
     */
    @Test
    fun original_to_shown_to_original_is_the_identity() {
        allSettings().forEach { settings ->
            val mapping = mappingFor(settings)
            val crop = settings.crop
            grid().forEach { g ->
                val p = Point(crop.left + g.x * (crop.right - crop.left), crop.top + g.y * (crop.bottom - crop.top))
                assertSamePoint(describe(settings), p, mapping.shownToOriginal(mapping.originalToShown(p)))
            }
        }
    }

    /** Shown points land inside the original's crop, and the corners go to the right corners for one known turn. */
    @Test
    fun a_quarter_turn_sends_the_shown_corners_to_the_expected_original_corners() {
        // One clockwise quarter turn of the whole photo: the shown top-left is the original's
        // bottom-left, because the original's left edge became the shown top edge.
        val mapping = mappingFor(EditSettings(quarterTurns = 1))
        assertSamePoint("shown top-left", Point(0f, 1f), mapping.shownToOriginal(Point(0f, 0f)))
        assertSamePoint("shown top-right", Point(0f, 0f), mapping.shownToOriginal(Point(1f, 0f)))
        // A horizontal flip alone mirrors left and right.
        val flipped = mappingFor(EditSettings(flipHorizontal = true))
        assertSamePoint("flipped left", Point(1f, 0.25f), flipped.shownToOriginal(Point(0f, 0.25f)))
    }

    /** An angle seen on the shown picture, taken to the original and back, is the angle it was. */
    @Test
    fun angles_round_trip() {
        allSettings().forEach { settings ->
            val mapping = mappingFor(settings)
            listOf(0f, 15f, 90f, 135f, 200f, 270f, 359f).forEach { shown ->
                val back = mapping.originalAngleToShown(mapping.shownAngleToOriginal(shown))
                assertTrue("${describe(settings)}: $shown came back as $back", angleGap(shown, back) <= 1e-3f)
                val other = mapping.shownAngleToOriginal(mapping.originalAngleToShown(shown))
                assertTrue("${describe(settings)}: original $shown came back as $other", angleGap(shown, other) <= 1e-3f)
            }
        }
    }

    /** A mirror flag taken both ways comes back as it was. */
    @Test
    fun mirror_flags_round_trip() {
        allSettings().forEach { settings ->
            val mapping = mappingFor(settings)
            listOf(false, true).forEach { flag ->
                assertEquals(describe(settings), flag, mapping.originalMirrorToShown(mapping.shownMirrorToOriginal(flag)))
                assertEquals(describe(settings), flag, mapping.shownMirrorToOriginal(mapping.originalMirrorToShown(flag)))
            }
        }
    }

    /** A width taken from the shown picture to the original and back is the width it was. */
    @Test
    fun widths_round_trip() {
        allSettings().forEach { settings ->
            val mapping = mappingFor(settings)
            listOf(0.05f, 1f / 3f, 0.5f, 1f).forEach { width ->
                assertEquals(describe(settings), width, mapping.originalWidthToShown(mapping.shownWidthToOriginal(width)), TOLERANCE)
                assertEquals(describe(settings), width, mapping.shownWidthToOriginal(mapping.originalWidthToShown(width)), TOLERANCE)
            }
        }
    }

    /**
     * A new sticker sits at the middle of the shown picture and is a third of its width: for an
     * off-centre crop turned a quarter turn (and for every other combination too), mapping its
     * centre back through [StickerMapping.originalToShown] gives (0.5, 0.5), and its width through
     * [StickerMapping.originalWidthToShown] gives one third. It stands upright and not mirrored as
     * seen: its stored angle and mirror flag map back to 0 and false.
     */
    @Test
    fun a_centred_sticker_is_in_the_middle_of_the_shown_picture_at_a_third_of_its_width() {
        val offCentre = EditSettings(crop = crops[1], quarterTurns = 1)
        check(offCentre, "off-centre crop, one quarter turn")
        allSettings().forEach { check(it, describe(it)) }
    }

    /** Checks the centred sticker of [settings]; [label] names the case in a failure. */
    private fun check(settings: EditSettings, label: String) {
        val mapping = mappingFor(settings)
        val sticker = mapping.centredSticker(9L, "img_sticker_apple")
        assertEquals(label, 9L, sticker.key)
        assertEquals(label, "img_sticker_apple", sticker.pictureId)
        assertSamePoint(label, Point(0.5f, 0.5f), mapping.originalToShown(Point(sticker.centreX, sticker.centreY)))
        assertEquals(label, 1f / 3f, mapping.originalWidthToShown(sticker.width), TOLERANCE)
        assertTrue(
            "$label: angle ${mapping.originalAngleToShown(sticker.degrees)} is not upright",
            angleGap(0f, mapping.originalAngleToShown(sticker.degrees)) <= 1e-3f,
        )
        assertEquals(label, false, mapping.originalMirrorToShown(sticker.mirrored))
    }

    /** The photo's size and the tolerance the round trips must meet. */
    private companion object {
        const val PHOTO_WIDTH = 400
        const val PHOTO_HEIGHT = 300
        const val TOLERANCE = 1e-4f
    }
}
