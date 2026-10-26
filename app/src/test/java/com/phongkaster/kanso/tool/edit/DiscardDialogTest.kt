package com.phongkaster.kanso.tool.edit

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.ui.tool.component.DISCARD_CONFIRM_TAG
import com.phongkaster.kanso.ui.tool.component.DISCARD_DIALOG_TAG
import com.phongkaster.kanso.ui.tool.component.DISCARD_KEEP_TAG
import com.phongkaster.kanso.ui.tool.component.DiscardChangesDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks on Android 15 the question asked when leaving the Edit screen with changes (DoD 12):
 * the dialog reads "Discard your changes?" and offers "Discard" and "Keep editing"; Discard calls
 * its callback only, Keep editing calls its own only, and the screen hiding the dialog in answer
 * removes it. When the question is asked at all is the ViewModel's `onBackPressed`, checked by the
 * ViewModel's own tests.
 *
 * The test shows the stateless `DiscardChangesDialog` while a `mutableStateOf` it holds says so,
 * as the screen shows it while its UiState's `discardPrompt` is true.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class DiscardDialogTest {

    /** Starts an empty activity to host the dialog. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test, for its strings. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** Whether the dialog is shown, as the screen's UiState says in the app. */
    private var shown by mutableStateOf(true)

    /** How many times "Discard" was answered. */
    private var discards = 0

    /** How many times "Keep editing" was answered. */
    private var keeps = 0

    /** The dialog asks "Discard your changes?" and offers "Discard" and "Keep editing". */
    @Test
    fun the_dialog_asks_discard_your_changes() {
        show()
        compose.onNodeWithTag(DISCARD_DIALOG_TAG).assertExists()
        compose.onNodeWithText("Discard your changes?").assertIsDisplayed()
        assertEquals("Discard your changes?", context.getString(R.string.edit_session_discard_title))
        compose.onNodeWithTag(DISCARD_CONFIRM_TAG).assertIsDisplayed().assertTextEquals("Discard")
        compose.onNodeWithTag(DISCARD_KEEP_TAG).assertIsDisplayed().assertTextEquals("Keep editing")
        assertEquals(0, discards)
        assertEquals(0, keeps)
    }

    /** "Discard" calls the discard callback once and not Keep editing's; the dialog then goes. */
    @Test
    fun discard_calls_on_discard() {
        show()
        compose.onNodeWithTag(DISCARD_CONFIRM_TAG).performClick()
        compose.waitForIdle()
        assertEquals(1, discards)
        assertEquals(0, keeps)
        compose.onNodeWithTag(DISCARD_DIALOG_TAG).assertDoesNotExist()
    }

    /** "Keep editing" calls its callback once and not Discard's; the dialog then goes. */
    @Test
    fun keep_editing_calls_on_keep_editing() {
        show()
        compose.onNodeWithTag(DISCARD_KEEP_TAG).performClick()
        compose.waitForIdle()
        assertEquals(0, discards)
        assertEquals(1, keeps)
        compose.onNodeWithTag(DISCARD_DIALOG_TAG).assertDoesNotExist()
    }

    /**
     * Draws the dialog while [shown] is true; each answer is counted and then hides it, as the
     * ViewModel clears `discardPrompt` in the app.
     */
    private fun show() {
        compose.setContent {
            MaterialTheme {
                if (shown) {
                    DiscardChangesDialog(
                        onDiscard = {
                            discards++
                            shown = false
                        },
                        onKeepEditing = {
                            keeps++
                            shown = false
                        },
                    )
                }
            }
        }
        compose.waitForIdle()
    }
}
