package com.phongkaster.kanso.tool.edit

import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditSession
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the Edit session's undo rules on the plain JVM (Run 9, DoD 7 and 25): one undo entry per
 * action (a change, a whole slider drag, a filter chip, a crop, a quarter turn), Undo going back
 * one action at a time in reverse order across sheets, a drag that is not yet released adding
 * nothing, a change to equal settings adding nothing, and a chosen crop ratio setting a centred
 * rectangle of that shape on a landscape and on a portrait photo.
 */
class EditSessionTest {

    /** One change is one undo entry, and undoing it gives the settings from before. */
    @Test
    fun a_change_is_one_undo_entry() {
        val start = EditSession()
        val changed = start.change(EditSettings(brightness = 0.5f))
        assertEquals(1, changed.undoStack.size)
        assertTrue(changed.canUndo)
        assertEquals(EditSettings(brightness = 0.5f), changed.settings)
        assertEquals(EditSettings(), changed.undo().settings)
        assertFalse(changed.undo().canUndo)
    }

    /** A slider drag of many previews, then its release, is one entry holding the pre-drag value. */
    @Test
    fun a_preview_then_commit_is_one_undo_entry() {
        val session = EditSession()
            .preview(EditSettings(contrast = 0.1f))
            .preview(EditSettings(contrast = 0.2f))
            .preview(EditSettings(contrast = 0.3f))
            .commit()
        assertEquals(1, session.undoStack.size)
        assertNull(session.pending)
        assertEquals(EditSettings(contrast = 0.3f), session.settings)
        assertEquals(EditSettings(), session.undo().settings)
    }

    /** A drag that is not released yet moves the settings but adds no undo entry. */
    @Test
    fun a_preview_without_commit_pushes_nothing() {
        val session = EditSession().preview(EditSettings(warmth = 0.4f)).preview(EditSettings(warmth = 0.6f))
        assertEquals(EditSettings(warmth = 0.6f), session.settings)
        assertTrue(session.undoStack.isEmpty())
        assertFalse(session.canUndo)
        assertEquals(EditSettings(), session.pending)
    }

    /** A change to the settings already shown, or a drag released where it began, adds nothing. */
    @Test
    fun a_change_to_equal_settings_pushes_nothing() {
        val session = EditSession().change(EditSettings(filter = FilterPreset.KELVIN))
        assertSame(session, session.change(EditSettings(filter = FilterPreset.KELVIN)))
        val dragBack = session
            .preview(session.settings.copy(saturation = 0.5f))
            .preview(session.settings)
            .commit()
        assertEquals(1, dragBack.undoStack.size)
    }

    /**
     * A quarter turn, a filter chip, a slider drag and a crop ratio, each from a different sheet,
     * are one entry each, and Undo takes them back one at a time, newest first.
     */
    @Test
    fun undo_takes_back_one_action_at_a_time_in_reverse_order() {
        val turned = EditSession().change(EditSettings(quarterTurns = 1))
        val filtered = turned.change(turned.settings.copy(filter = FilterPreset.AMARO))
        val adjusted = filtered
            .preview(filtered.settings.copy(brightness = 0.2f))
            .preview(filtered.settings.copy(brightness = 0.7f))
            .commit()
        val cropped = adjusted.cropRatioChosen(CropRatio.SQUARE, photoAspect = 4f / 3f)
        assertEquals(4, cropped.undoStack.size)

        val once = cropped.undo()
        assertEquals(adjusted.settings, once.settings)
        val twice = once.undo()
        assertEquals(filtered.settings, twice.settings)
        val thrice = twice.undo()
        assertEquals(turned.settings, thrice.settings)
        val fourTimes = thrice.undo()
        assertEquals(EditSettings(), fourTimes.settings)
        assertFalse(fourTimes.canUndo)
        // With nothing left, Undo changes nothing.
        assertEquals(fourTimes, fourTimes.undo())
    }

    /** On a landscape 4:3 photo a square is the full height with the sides trimmed equally. */
    @Test
    fun a_square_on_a_landscape_photo_is_centred() {
        val session = EditSession().cropRatioChosen(CropRatio.SQUARE, photoAspect = 4f / 3f)
        assertEquals(CropRatio.SQUARE, session.settings.cropRatio)
        assertRect(EditSettings.CropRect(0.125f, 0f, 0.875f, 1f), session.settings.crop)
        assertEquals(1, session.undoStack.size)
    }

    /** On a portrait 3:4 photo a square is the full width, and 16:9 a centred band across it. */
    @Test
    fun a_ratio_on_a_portrait_photo_is_centred() {
        val square = EditSession().cropRatioChosen(CropRatio.SQUARE, photoAspect = 3f / 4f)
        assertRect(EditSettings.CropRect(0f, 0.125f, 1f, 0.875f), square.settings.crop)
        // 16:9 on 3:4: the band is (3/4) / (16/9) = 0.421875 of the height.
        val wide = EditSession().cropRatioChosen(CropRatio.SIXTEEN_NINE, photoAspect = 3f / 4f)
        assertRect(EditSettings.CropRect(0f, 0.2890625f, 1f, 0.7109375f), wide.settings.crop)
    }

    /**
     * With the photo turned a quarter, the shape is fitted on its side, because the crop is cut
     * before the turn: 16:9 on a 4:3 landscape original keeps a tall 9:16 band, which turns wide.
     */
    @Test
    fun a_ratio_on_a_turned_photo_is_fitted_on_its_side() {
        val turned = EditSession().change(EditSettings(quarterTurns = 1))
        val wide = turned.cropRatioChosen(CropRatio.SIXTEEN_NINE, photoAspect = 4f / 3f)
        assertRect(EditSettings.CropRect(0.2890625f, 0f, 0.7109375f, 1f), wide.settings.crop)
    }

    /** Free unlocks the handles and keeps the rectangle already chosen, as one action. */
    @Test
    fun free_keeps_the_rectangle_and_unlocks() {
        val square = EditSession().cropRatioChosen(CropRatio.SQUARE, photoAspect = 4f / 3f)
        val free = square.cropRatioChosen(CropRatio.FREE, photoAspect = 4f / 3f)
        assertEquals(CropRatio.FREE, free.settings.cropRatio)
        assertEquals(square.settings.crop, free.settings.crop)
        assertEquals(2, free.undoStack.size)
    }

    /** Asserts that [actual] has [expected]'s four edges, each within a millionth or so. */
    private fun assertRect(expected: EditSettings.CropRect, actual: EditSettings.CropRect) {
        assertEquals("left", expected.left, actual.left, TOLERANCE)
        assertEquals("top", expected.top, actual.top, TOLERANCE)
        assertEquals("right", expected.right, actual.right, TOLERANCE)
        assertEquals("bottom", expected.bottom, actual.bottom, TOLERANCE)
    }

    /**
     * Run 9 review: settings that change nothing count as neutral, so Back asks nothing and Save
     * copies the original: a locked ratio with the whole photo kept, four turns round, a filter
     * at intensity 0 (DoD 12).
     */
    @Test
    fun settings_that_change_nothing_are_neutral() {
        assertTrue(EditSettings().isNeutral)
        assertTrue(EditSettings(cropRatio = CropRatio.FREE).isNeutral)
        assertTrue(EditSettings(cropRatio = CropRatio.FOUR_THREE).isNeutral)
        assertTrue(EditSettings(quarterTurns = 4).isNeutral)
        assertTrue(EditSettings(filter = FilterPreset.KELVIN, filterIntensity = 0f).isNeutral)
        assertFalse(EditSettings(quarterTurns = 1).isNeutral)
        assertFalse(EditSettings(brightness = 0.1f).isNeutral)
    }

    /** The constant the rectangle checks share. */
    private companion object {
        /** How far a rectangle's edge may be from the expected fraction (float rounding). */
        const val TOLERANCE = 0.00001f
    }
}
