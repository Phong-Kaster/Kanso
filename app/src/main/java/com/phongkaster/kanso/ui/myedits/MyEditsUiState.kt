package com.phongkaster.kanso.ui.myedits

import com.phongkaster.kanso.data.media.Photo

/**
 * Everything the My edits tab shows at one moment. It is the tab's UiState: for what a UiState
 * is and how the screen reads it, see HomeUiState.
 *
 * [photos] are the photos Kanso has saved, newest first; [loading] is true while they are being
 * read. Together they say which of the screen's three faces to show:
 *
 * - some [photos]: the grid of photos, even while a new read runs ([loading] true), so the grid
 *   never blinks away when the tab is opened again;
 * - no photos and [loading]: the first read is still running, so the screen shows skeleton tiles;
 * - no photos and not [loading]: Kanso has saved nothing yet (or nothing is readable), so the
 *   screen shows its short "no edits yet" message.
 *
 * A new snapshot starts as loading with no photos: nothing has been read yet.
 */
data class MyEditsUiState(
    val photos: List<Photo> = emptyList(),
    val loading: Boolean = true,
)
