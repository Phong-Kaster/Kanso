package com.phongkaster.kanso.ui.saved

/**
 * Everything the Saved screen shows at one moment, as one snapshot.
 *
 * [photoUri] is the text of the Uri the save returned: the screen shares that photo and marks the
 * image with it, so tests can tell which photo is shown. [photo] is where decoding that photo
 * stands: [SavedPhoto.Loading] until [SavedViewModel] has decoded it, then [SavedPhoto.Ready] with
 * the picture or [SavedPhoto.Unreadable].
 *
 * Every screen in the app describes itself this way; `HomeUiState` explains the idea in full.
 */
data class SavedUiState(
    val photoUri: String,
    val photo: SavedPhoto = SavedPhoto.Loading,
)
