package com.phongkaster.kanso.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.library.colorpicker.HSVColorPicker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * Checks how the engine's HSVColorPicker reads a colour code typed into its hex field (DoD 17).
 *
 * The picker is hosted alone, twice, as `PickerDefaultAlphaTest` does: once with the default
 * `showAlpha = true` and once with `showAlpha = false`. Both start at white, and every colour
 * either one passes to `onColorChanged` is recorded, so a test can say exactly what the field
 * reported and when. Every case below is run on both pickers.
 *
 * The field is the node with editable text under each host's tag, in the unmerged tree (the
 * engine draws the field inside its own decoration, so the merged tree would hide it).
 *
 * The rule the field must follow: only hex digits are kept (either case, shown upper-cased, at
 * most six); a complete six-digit code applies exactly that colour, fully opaque; a shorter code
 * changes nothing and reports nothing.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class PickerHexInputTest {

    /** Hosts the pickers under test in an empty activity. */
    @get:Rule
    val compose = createComposeRule()

    /**
     * Counts how many times fresh pickers were asked for. The pickers are drawn inside a `key` on
     * this number, so raising it throws the old pickers and their remembered state away.
     */
    private val generation = mutableIntStateOf(0)

    /** Every colour each picker reported, keyed by the tag of the box around that picker. */
    private val recorded: Map<String, MutableList<Color>> = mapOf(
        WITH_ALPHA_TAG to mutableListOf(),
        WITHOUT_ALPHA_TAG to mutableListOf(),
    )

    /** Stops Koin, which the app's Application class starts, so it does not leak into other tests. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * (a) Replacing the field's text with `2E7D32` reports exactly opaque `#2E7D32`, the field
     * reads `2E7D32`, and the picker that shows a percentage shows 100%.
     */
    @Test
    fun a_complete_code_gives_exactly_that_opaque_colour() {
        showPickers()

        PICKER_TAGS.forEach { tag ->
            field(tag).performTextReplacement("2E7D32")
            compose.waitForIdle()

            val last = recorded.getValue(tag).lastOrNull()
            assertTrue("$tag: typing 2E7D32 should report a colour", last != null)
            assertEquals("$tag: the colour reported", hex(GREEN_ARGB), hex(last!!.toArgb()))
            assertEquals("$tag: the colour must be fully opaque", 1f, last.alpha)
            assertEquals("$tag: the field's text", "2E7D32", fieldText(tag))
        }

        // The engine draws an invisible "100%" to size its box and the real percentage on top, so
        // every percentage under the picker must read 100% (and there must be some).
        val percentages = compose.onAllNodes(
            hasAnyAncestor(hasTestTag(WITH_ALPHA_TAG)) and hasPercentageText(),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().flatMap { node ->
            node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        }
        assertTrue("The picker with alpha should show a percentage", percentages.isNotEmpty())
        assertTrue("The percentage should read 100%, found $percentages", percentages.all { it == "100%" })
    }

    /**
     * (b) Typing the code one character at a time reports nothing for the first five characters
     * and exactly one colour, opaque `#2E7D32`, after the sixth.
     *
     * The field starts holding white's code `FFFFFF`, so it is emptied first; an empty field is
     * a partial code too, so the emptying must report nothing either.
     */
    @Test
    fun typing_one_character_at_a_time_reports_only_the_complete_code() {
        showPickers()

        PICKER_TAGS.forEach { tag ->
            val colours = recorded.getValue(tag)
            field(tag).performTextClearance()
            compose.waitForIdle()
            assertEquals("$tag: emptying the field should report nothing", emptyList<String>(), colours.map { hex(it.toArgb()) })

            val typed = StringBuilder()
            "2E7D3".forEach { char ->
                field(tag).performTextInput(char.toString())
                compose.waitForIdle()
                typed.append(char)
                assertEquals(
                    "$tag: after typing $typed nothing should be reported",
                    emptyList<String>(),
                    colours.map { hex(it.toArgb()) },
                )
                assertEquals("$tag: the field's text after typing $typed", typed.toString(), fieldText(tag))
            }

            field(tag).performTextInput("2")
            compose.waitForIdle()
            assertEquals(
                "$tag: the sixth character should report exactly one colour",
                listOf(hex(GREEN_ARGB)),
                colours.map { hex(it.toArgb()) },
            )
            assertEquals("$tag: the field's text", "2E7D32", fieldText(tag))
        }
    }

    /**
     * (c) From a fresh picker, a code of each length from one to five changes nothing, and the
     * field shows what was typed.
     */
    @Test
    fun a_partial_code_reports_nothing() {
        showPickers()

        listOf("2", "2E", "2E7", "2E7D", "2E7D3").forEach { partial ->
            freshPickers()
            PICKER_TAGS.forEach { tag ->
                field(tag).performTextReplacement(partial)
                compose.waitForIdle()
                assertEquals(
                    "$tag: typing $partial should report nothing",
                    emptyList<String>(),
                    recorded.getValue(tag).map { hex(it.toArgb()) },
                )
                assertEquals("$tag: the field's text after typing $partial", partial, fieldText(tag))
            }
        }
    }

    /** (d) The letters A to F and the extremes are read as hex: each code gives its own colour. */
    @Test
    fun letters_and_extremes_give_their_own_colours() {
        showPickers()

        val cases = listOf(
            "ABCDEF" to 0xFFABCDEF.toInt(),
            "1A73E8" to 0xFF1A73E8.toInt(),
            "000000" to 0xFF000000.toInt(),
            "FFFFFF" to 0xFFFFFFFF.toInt(),
        )
        cases.forEach { (code, argb) ->
            PICKER_TAGS.forEach { tag -> assertCodeGives(tag, typed = code, argb = argb, shown = code) }
        }
    }

    /** (e) A lowercase code works too, and the field shows it upper-cased. */
    @Test
    fun a_lowercase_code_works_and_is_shown_upper_cased() {
        showPickers()

        PICKER_TAGS.forEach { tag -> assertCodeGives(tag, typed = "2e7d32", argb = GREEN_ARGB, shown = "2E7D32") }
    }

    /** (f) Characters that are not hex digits are dropped: `#2E-7D 32` reads as `2E7D32`. */
    @Test
    fun characters_that_are_not_hex_digits_are_dropped() {
        showPickers()

        PICKER_TAGS.forEach { tag -> assertCodeGives(tag, typed = "#2E-7D 32", argb = GREEN_ARGB, shown = "2E7D32") }
    }

    /**
     * Replaces the field of the picker tagged [tag] with [typed], then fails unless the last
     * colour it reported is exactly [argb] (opaque, no tolerance) and the field reads [shown].
     */
    private fun assertCodeGives(tag: String, typed: String, argb: Int, shown: String) {
        val colours = recorded.getValue(tag)
        colours.clear()
        field(tag).performTextReplacement(typed)
        compose.waitForIdle()
        assertTrue("$tag: typing $typed should report a colour", colours.isNotEmpty())
        assertEquals("$tag: the colour reported for $typed", hex(argb), hex(colours.last().toArgb()))
        assertEquals("$tag: the colour for $typed must be fully opaque", 1f, colours.last().alpha)
        assertEquals("$tag: the field's text after typing $typed", shown, fieldText(tag))
    }

    /**
     * Shows the two pickers, each starting at white and recording what it reports, then empties
     * the records so a test sees only what its own typing caused.
     */
    private fun showPickers() {
        compose.setContent {
            // Reading the generation here makes the pickers start over when it is raised.
            key(generation.intValue) {
                Column {
                    Box(modifier = Modifier.width(PICKER_WIDTH).testTag(WITH_ALPHA_TAG)) {
                        HSVColorPicker(
                            color = Color.White,
                            onColorChanged = { recorded.getValue(WITH_ALPHA_TAG) += it },
                        )
                    }
                    Box(modifier = Modifier.width(PICKER_WIDTH).testTag(WITHOUT_ALPHA_TAG)) {
                        HSVColorPicker(
                            color = Color.White,
                            onColorChanged = { recorded.getValue(WITHOUT_ALPHA_TAG) += it },
                            showAlpha = false,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        recorded.values.forEach { it.clear() }
    }

    /** Throws the current pickers away, draws new ones at white, and empties the records. */
    private fun freshPickers() {
        compose.runOnIdle { generation.intValue++ }
        compose.waitForIdle()
        recorded.values.forEach { it.clear() }
        PICKER_TAGS.forEach { tag ->
            assertEquals("$tag: a fresh picker shows white's code", "FFFFFF", fieldText(tag))
        }
    }

    /** Finds the hex field of the picker tagged [tag]: the node with editable text under it. */
    private fun field(tag: String): SemanticsNodeInteraction = compose.onNode(
        hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText),
        useUnmergedTree = true,
    )

    /** Returns the text the hex field of the picker tagged [tag] holds right now. */
    private fun fieldText(tag: String): String =
        field(tag).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    /** Writes an ARGB colour number as `#AARRGGBB`, so a failure shows a readable colour. */
    private fun hex(argb: Int): String = "#%08X".format(argb)

    /** Tags, sizes and colours the tests share. */
    private companion object {
        /** Tag on the box around the default picker, the one with a percentage. */
        const val WITH_ALPHA_TAG = "hex_picker_with_alpha"

        /** Tag on the box around the picker with `showAlpha = false`. */
        const val WITHOUT_ALPHA_TAG = "hex_picker_without_alpha"

        /** Both pickers' tags, in the order every case visits them. */
        val PICKER_TAGS = listOf(WITH_ALPHA_TAG, WITHOUT_ALPHA_TAG)

        /** Both pickers get this width. */
        val PICKER_WIDTH = 360.dp

        /** The DoD's green, #2E7D32, fully opaque, as an ARGB number. */
        val GREEN_ARGB = 0xFF2E7D32.toInt()
    }
}
