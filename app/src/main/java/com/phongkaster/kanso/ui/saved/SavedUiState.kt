package com.phongkaster.kanso.ui.saved

/**
 * Everything the Saved screen shows at one moment, as one snapshot.
 *
 * [photoUri] is the text of the photo's Uri: the screen shares that photo and marks the image with
 * it, so tests can tell which photo is shown. [photo] is where decoding that photo stands:
 * [SavedPhoto.Loading] until [SavedViewModel] has decoded it, then [SavedPhoto.Ready] with the
 * picture or [SavedPhoto.Unreadable].
 *
 * [viewer] tells which of the screen's two uses this is. False (the default) is the screen shown
 * right after a save: titled "Saved", with the success mark, the line saying where the photo went
 * and "Edit another". True is the photo viewer, a photo opened from the My edits tab: titled
 * "My edits", with no mark and no line (nothing was just saved), and "Edit" to open this very photo
 * in the Edit tool.
 *
 * Every screen in the app describes itself this way; `HomeUiState` explains the idea in full.
 */
data class SavedUiState(
    val photoUri: String,
    val photo: SavedPhoto = SavedPhoto.Loading,
    val viewer: Boolean = false,
)
