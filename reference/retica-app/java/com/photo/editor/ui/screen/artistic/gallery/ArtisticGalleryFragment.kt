package com.photo.editor.ui.screen.artistic.gallery

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photo.editor.R
import com.photo.editor.common.util.logEvent
import com.photo.editor.ui.activity.setLightStatusBar
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.ui.screen.artistic.common.ArtisticViewModel
import com.photo.editor.ui.screen.gallery.GalleryScreen
import com.photo.editor.ui.screen.gallery.GalleryViewModel
import org.koin.androidx.navigation.koinNavGraphViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ArtisticGalleryFragment : BaseComposeFragment() {

    private val viewModel by koinNavGraphViewModel<ArtisticViewModel>(R.id.artistic_graph)
    private val galleryViewModel: GalleryViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setLightStatusBar(true)
    }

    override fun onScreenView() {
        super.onScreenView()
        logEvent("artistic_gallery_scr")
    }

    override fun onStart() {
        super.onStart()
        galleryViewModel.fetchMedia()
    }

    @Composable
    override fun ComposeView() {
        GalleryScreen(
            buckets = galleryViewModel.imageBuckets.collectAsStateWithLifecycle().value,
            images = galleryViewModel.allImages.collectAsStateWithLifecycle().value,
            onImageClick = { galleryImage ->
                viewModel.setOriginalImage(galleryImage.uri)
                safeNavigate(ArtisticGalleryFragmentDirections.actionGalleryToEdit())
            },
            onBackClick = {
                safePopBackstack()
            }
        )
    }
}