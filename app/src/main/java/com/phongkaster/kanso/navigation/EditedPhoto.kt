package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * One photo from the My edits tab, opened full screen: the photo viewer. It is drawn by the same
 * screen as [Result], in its viewer form (no success mark, the title "My edits", and "Edit" instead
 * of "Go home"), because both show one finished photo with the same ways out.
 *
 * [photoUri] is the text of the photo's content Uri, as My edits listed it. Like every key it is
 * saved with the back stack, so the same photo reopens after rotation or process death. The key
 * sits above [MyEdits] on the back stack: system Back returns to the grid, while the round close
 * button goes Home.
 */
@Serializable
data class EditedPhoto(val photoUri: String) : NavKey
