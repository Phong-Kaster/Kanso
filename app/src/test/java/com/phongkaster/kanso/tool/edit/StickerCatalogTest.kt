package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks on Android 15 the list of ready-made layers, [StickerCatalog] (DoD 3):
 *
 * - four categories in the order Food, Mood, Quote, Emoji holding 17, 17, 6 and 20 layers, 60 in
 *   all;
 * - every sticker's picture id is unique and equals the name of its drawable resource;
 * - every drawable id exists (is not zero) and decodes to a real picture;
 * - [StickerCatalog.byId] finds each sticker, and finds nothing for an unknown id.
 *
 * It runs under Robolectric with native graphics, because decoding a webp drawable needs the real
 * `BitmapFactory`, and resource names need the app's resource table.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class StickerCatalogTest {

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

    /** The categories are Food, Mood, Quote and Emoji, in that order. */
    @Test
    fun the_four_categories_come_in_order() {
        assertEquals(
            listOf(StickerCategory.FOOD, StickerCategory.MOOD, StickerCategory.QUOTE, StickerCategory.EMOJI),
            StickerCategory.entries,
        )
    }

    /** Food holds 17, Mood 17, Quote 6 and Emoji 20 layers: 60 in all. */
    @Test
    fun the_categories_hold_17_17_6_and_20_stickers_60_in_all() {
        assertEquals(17, StickerCatalog.inCategory(StickerCategory.FOOD).size)
        assertEquals(17, StickerCatalog.inCategory(StickerCategory.MOOD).size)
        assertEquals(6, StickerCatalog.inCategory(StickerCategory.QUOTE).size)
        assertEquals(20, StickerCatalog.inCategory(StickerCategory.EMOJI).size)
        assertEquals(60, StickerCatalog.all.size)
        // Every sticker is listed under its own category, and the list shows the categories in order.
        StickerCategory.entries.forEach { category ->
            StickerCatalog.inCategory(category).forEach { assertEquals(category, it.category) }
        }
        assertEquals(
            "the list is not grouped food, mood, quote, emoji",
            StickerCatalog.all.sortedBy { it.category.ordinal },
            StickerCatalog.all,
        )
    }

    /** No two layers share a picture id. */
    @Test
    fun every_picture_id_is_unique() {
        val ids = StickerCatalog.all.map { it.pictureId }
        assertEquals("a picture id is used twice", ids.size, ids.toSet().size)
    }

    /** No two layers share a drawable, and every drawable id exists. */
    @Test
    fun every_drawable_id_exists_and_is_unique() {
        StickerCatalog.all.forEach { sticker ->
            assertTrue("${sticker.pictureId} has no drawable (id 0)", sticker.res != 0)
        }
        val resources = StickerCatalog.all.map { it.res }
        assertEquals("a drawable is used twice", resources.size, resources.toSet().size)
    }

    /** Each picture id is the drawable's own resource entry name, in the `drawable` type. */
    @Test
    fun every_picture_id_is_the_drawables_resource_name() {
        val resources = context.resources
        StickerCatalog.all.forEach { sticker ->
            assertEquals("drawable", resources.getResourceTypeName(sticker.res))
            assertEquals(sticker.pictureId, resources.getResourceEntryName(sticker.res))
        }
    }

    /** Every drawable decodes to a picture with a width and a height. */
    @Test
    fun every_picture_decodes() {
        val resources = context.resources
        StickerCatalog.all.forEach { sticker ->
            val bitmap = BitmapFactory.decodeResource(resources, sticker.res)
            assertNotNull("${sticker.pictureId} did not decode", bitmap)
            assertTrue("${sticker.pictureId} decoded empty", bitmap.width > 0 && bitmap.height > 0)
            bitmap.recycle()
        }
    }

    /** [StickerCatalog.byId] finds every sticker by its id, and nothing for an id the catalog lacks. */
    @Test
    fun by_id_finds_each_sticker() {
        StickerCatalog.all.forEach { sticker ->
            assertEquals(sticker, StickerCatalog.byId(sticker.pictureId))
        }
        assertNull(StickerCatalog.byId("img_sticker_does_not_exist"))
    }
}
