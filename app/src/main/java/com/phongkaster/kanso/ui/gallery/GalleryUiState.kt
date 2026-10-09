package com.phongkaster.kanso.ui.gallery

import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.navigation.Tool

/**
 * Everything the gallery shows at one moment. It is the gallery's UiState: for what a UiState
 * is and how the screen reads it, see HomeUiState.
 *
 * [tool] is the tool the chosen photo will be opened in, whose name titles the top bar;
 * [albums] feed the album switcher; [selectedAlbumId] is the chosen album, or null for
 * "All photos"; [photos] are that album's photos, newest first; [loading] is true while the
 * photo library is being read.
 */
data class GalleryUiState(
    val tool: Tool,
    val albums: List<Album> = emptyList(),
    val selectedAlbumId: Long? = null,
    val photos: List<Photo> = emptyList(),
    val loading: Boolean = true,
)
