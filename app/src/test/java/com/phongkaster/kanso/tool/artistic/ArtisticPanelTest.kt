package com.phongkaster.kanso.tool.artistic

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.artistic.ArtisticStyle
import com.phongkaster.kanso.ui.tool.component.toolpanel.ARTISTIC_PANEL_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.ARTISTIC_RUN_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.ARTISTIC_STRENGTH_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.ArtisticPanel
import com.phongkaster.kanso.ui.tool.component.toolpanel.ArtisticPanelHost
import com.phongkaster.kanso.ui.tool.component.toolpanel.artisticStyleTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 12, the controls: the Artistic panel shows a chip per style, a strength slider and an
 * Apply button; tapping a chip reports that style, tapping Apply reports the tap, and the
 * stateful host hands Apply the style that was tapped.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ArtisticPanelTest {

    /** Starts an empty activity to host the panel. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Every style has its chip, and the panel has its slider and its button. */
    @Test
    fun the_panel_shows_every_style_a_slider_and_apply() {
        compose.setContent {
            MaterialTheme {
                ArtisticPanel(
                    selected = ArtisticStyle.WARM_WASH,
                    strength = 1f,
                    onSelect = {},
                    onStrengthChange = {},
                    onApply = {},
                )
            }
        }
        compose.onNodeWithTag(ARTISTIC_PANEL_TAG).assertExists()
        ArtisticStyle.entries.forEach { style -> compose.onNodeWithTag(artisticStyleTag(style)).assertExists() }
        compose.onNodeWithTag(artisticStyleTag(ArtisticStyle.WARM_WASH)).assertIsSelected()
        compose.onNodeWithTag(ARTISTIC_STRENGTH_TAG).assertExists()
        compose.onNodeWithTag(ARTISTIC_RUN_TAG).assertExists()
    }

    /** Tapping a style's chip calls onSelect with that style. */
    @Test
    fun tapping_a_chip_selects_its_style() {
        val chosen = mutableListOf<ArtisticStyle>()
        compose.setContent {
            MaterialTheme {
                ArtisticPanel(
                    selected = ArtisticStyle.WARM_WASH,
                    strength = 1f,
                    onSelect = { chosen += it },
                    onStrengthChange = {},
                    onApply = {},
                )
            }
        }
        compose.onNodeWithTag(artisticStyleTag(ArtisticStyle.INK)).performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(listOf(ArtisticStyle.INK), chosen)
    }

    /** Tapping Apply calls onApply once. */
    @Test
    fun tapping_apply_calls_on_apply() {
        var applied = 0
        compose.setContent {
            MaterialTheme {
                ArtisticPanel(
                    selected = ArtisticStyle.WARM_WASH,
                    strength = 1f,
                    onSelect = {},
                    onStrengthChange = {},
                    onApply = { applied++ },
                )
            }
        }
        compose.onNodeWithTag(ARTISTIC_RUN_TAG).performClick()
        compose.waitForIdle()

        assertEquals(1, applied)
    }

    /** The stateful host remembers the tapped style and hands it, with full strength, to Apply. */
    @Test
    fun the_host_applies_the_chosen_style() {
        val applied = mutableListOf<Pair<ArtisticStyle, Float>>()
        compose.setContent {
            MaterialTheme {
                ArtisticPanelHost(onApply = { style, strength -> applied += style to strength })
            }
        }
        compose.onNodeWithTag(artisticStyleTag(ArtisticStyle.MOSAIC)).performScrollTo().performClick()
        compose.onNodeWithTag(artisticStyleTag(ArtisticStyle.MOSAIC)).assertIsSelected()
        compose.onNodeWithTag(ARTISTIC_RUN_TAG).performClick()
        compose.waitForIdle()

        assertEquals(listOf(ArtisticStyle.MOSAIC to 1f), applied)
    }
}
