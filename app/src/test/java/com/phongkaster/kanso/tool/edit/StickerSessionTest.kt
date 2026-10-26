package com.phongkaster.kanso.tool.edit

import com.phongkaster.kanso.data.edit.edit.EditSession
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.Sticker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the sticker actions of [EditSession] on the plain JVM (Run 10, DoD 12, 13 and 21):
 * add, delete, duplicate and flip each make exactly one undo entry; a whole drag of fifty moves is
 * one entry and one undo restores the settings from before it; a drag that ends where it began
 * makes none; a 21st sticker is refused without touching settings or history; a mixed sequence
 * undoes in reverse order; and neither [EditSettings] nor [Sticker] holds a selection.
 */
class StickerSessionTest {

    /** A sticker with the given [key], somewhere in the middle of the photo. */
    private fun sticker(key: Long) = Sticker(
        key = key, pictureId = "star", centreX = 0.5f, centreY = 0.5f, width = 0.2f, degrees = 0f, mirrored = false,
    )

    /** A session holding [count] stickers, keys 1..count, built through [EditSession.addSticker]. */
    private fun sessionWith(count: Int): EditSession =
        (1..count).fold(EditSession()) { session, key -> session.addSticker(sticker(key.toLong())) }

    /** Add pushes one undo entry. */
    @Test
    fun add_is_one_undo_entry() {
        val session = EditSession().addSticker(sticker(1))
        assertEquals(1, session.settings.stickers.size)
        assertEquals(1, session.undoStack.size)
        assertEquals(EditSettings(), session.undo().settings)
    }

    /** Delete pushes one undo entry and undo brings the sticker back. */
    @Test
    fun delete_is_one_undo_entry() {
        val before = sessionWith(2)
        val after = before.deleteSticker(1)
        assertEquals(listOf(2L), after.settings.stickers.map { it.key })
        assertEquals(before.undoStack.size + 1, after.undoStack.size)
        assertEquals(before.settings, after.undo().settings)
    }

    /** Duplicate pushes one undo entry; the copy is on top, offset, with the new key. */
    @Test
    fun duplicate_is_one_undo_entry() {
        val before = sessionWith(1)
        val after = before.duplicateSticker(key = 1, newKey = 2, offsetX = 0.05f, offsetY = 0.05f)
        assertEquals(listOf(1L, 2L), after.settings.stickers.map { it.key })
        assertEquals(0.55f, after.settings.stickers.last().centreX, 1e-6f)
        assertEquals(before.undoStack.size + 1, after.undoStack.size)
        assertEquals(before.settings, after.undo().settings)
    }

    /** Flip pushes one undo entry and toggles the mirror. */
    @Test
    fun flip_is_one_undo_entry() {
        val before = sessionWith(1)
        val after = before.flipSticker(1)
        assertTrue(after.settings.stickers.single().mirrored)
        assertEquals(before.undoStack.size + 1, after.undoStack.size)
        assertEquals(before.settings, after.undo().settings)
    }

    /** Fifty moves of one drag, then commit: exactly one entry, and its undo restores the start. */
    @Test
    fun a_drag_of_fifty_moves_is_one_undo_entry() {
        val start = sessionWith(1)
        var session = start
        for (step in 1..50) {
            val moved = session.settings.stickers.single().copy(centreX = 0.5f + step * 0.005f)
            session = session.preview(session.settings.copy(stickers = listOf(moved)))
        }
        session = session.commit()
        assertEquals(start.undoStack.size + 1, session.undoStack.size)
        assertEquals(start.settings, session.undo().settings)
    }

    /** A drag that ends where it began makes no entry. */
    @Test
    fun a_drag_that_ends_where_it_began_adds_nothing() {
        val start = sessionWith(1)
        val original = start.settings.stickers.single()
        var session = start.preview(start.settings.copy(stickers = listOf(original.copy(centreX = 0.9f))))
        session = session.preview(session.settings.copy(stickers = listOf(original)))
        session = session.commit()
        assertEquals(start.undoStack.size, session.undoStack.size)
        assertEquals(start.settings, session.settings)
    }

    /** At 20 stickers an add leaves the whole session as it was. */
    @Test
    fun a_21st_add_changes_nothing() {
        val full = sessionWith(EditSettings.MAX_STICKERS)
        assertFalse(full.settings.canAddSticker)
        val after = full.addSticker(sticker(99))
        assertEquals(full, after)
        assertEquals(EditSettings.MAX_STICKERS, after.settings.stickers.size)
    }

    /** At 20 stickers a duplicate leaves the whole session as it was. */
    @Test
    fun a_21st_duplicate_changes_nothing() {
        val full = sessionWith(EditSettings.MAX_STICKERS)
        val after = full.duplicateSticker(key = 1, newKey = 99, offsetX = 0.05f, offsetY = 0.05f)
        assertEquals(full, after)
    }

    /** Crop, add, filter, drag: four undos walk back through them in reverse order. */
    @Test
    fun a_mixed_sequence_undoes_in_reverse_order() {
        val crop = EditSettings.CropRect(0.1f, 0.1f, 0.9f, 0.9f)
        val s0 = EditSession()
        val s1 = s0.change(s0.settings.copy(crop = crop))
        val s2 = s1.addSticker(sticker(1))
        val s3 = s2.change(s2.settings.copy(filter = FilterPreset.KELVIN))
        var s4 = s3
        for (step in 1..10) {
            val moved = s4.settings.stickers.single().copy(centreX = 0.5f + step * 0.01f)
            s4 = s4.preview(s4.settings.copy(stickers = listOf(moved)))
        }
        s4 = s4.commit()
        assertEquals(4, s4.undoStack.size)

        val u1 = s4.undo()
        assertEquals(s3.settings, u1.settings)
        val u2 = u1.undo()
        assertEquals(s2.settings, u2.settings)
        val u3 = u2.undo()
        assertEquals(s1.settings, u3.settings)
        val u4 = u3.undo()
        assertEquals(s0.settings, u4.settings)
        assertFalse(u4.canUndo)
    }

    /** Selection is screen state: neither the settings nor a sticker has a field for it. */
    @Test
    fun settings_and_stickers_hold_no_selection() {
        val names = (EditSettings::class.java.declaredFields + Sticker::class.java.declaredFields).map { it.name }
        assertTrue("fields: $names", names.none { it.startsWith("selected", ignoreCase = true) })
    }
}
