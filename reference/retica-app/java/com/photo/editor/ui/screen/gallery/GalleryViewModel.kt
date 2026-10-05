package com.photo.editor.ui.screen.gallery

import android.annotation.SuppressLint
import android.content.Context
import com.photo.gallery.data.repository.MediaRepository
import com.photo.gallery.model.GalleryImage
import com.photo.editor.platform.app.App
import com.photo.editor.ui.base.BaseViewModel
import com.photo.gallery.model.GalleryBucket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.annotation.KoinViewModel
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@KoinViewModel
class GalleryViewModel(
    private val context: Context,
) : BaseViewModel() {
    private val mediaRepository = MediaRepository()

    private val _imageBuckets = MutableStateFlow<List<GalleryBucket>>(listOf())
    val imageBuckets = _imageBuckets.asStateFlow()

    private val _allImages = MutableStateFlow<List<GalleryImage>>(listOf())
    val allImages = _allImages.asStateFlow()

    fun fetchMedia() {
        launchInIOTryCatch {
            _imageBuckets.value = mediaRepository.getImageBuckets(context)
        }

        launchInIOTryCatch {
            _allImages.value = mediaRepository.getAllImages(context)
        }
    }
}
