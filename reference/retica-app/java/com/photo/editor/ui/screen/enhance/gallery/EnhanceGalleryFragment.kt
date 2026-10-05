package com.photo.editor.ui.screen.enhance.gallery

import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photo.editor.R
import com.photo.editor.common.util.logEvent
import com.photo.editor.ui.activity.setLightStatusBar
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.ui.screen.enhance.common.EnhanceViewModel
import com.photo.editor.ui.screen.gallery.GalleryScreen
import com.photo.editor.ui.screen.gallery.GalleryViewModel
import org.koin.androidx.navigation.koinNavGraphViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class EnhanceGalleryFragment : BaseComposeFragment() {
    private val viewModel by koinNavGraphViewModel<EnhanceViewModel>(R.id.enhance_graph)
    private val galleryViewModel: GalleryViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setLightStatusBar(true)
    }

    override fun onScreenView() {
        super.onScreenView()
//        logEvent("enhance_gallery_scr")
    }

    override fun onStart() {
        super.onStart()
        galleryViewModel.fetchMedia()
    }

    @Composable
    override fun ComposeView() {
        BackHandler {
            logEvent("ai_enhance_scr_back_click")
            safePopBackstack()
        }

        GalleryScreen(
            buckets = galleryViewModel.imageBuckets.collectAsStateWithLifecycle().value,
            images = galleryViewModel.allImages.collectAsStateWithLifecycle().value,
            onImageClick = { galleryImage ->
                viewModel.setImage(galleryImage.uri)
                safeNavigate(EnhanceGalleryFragmentDirections.actionGalleryToEdit())
            },
            onBackClick = {
                logEvent("ai_enhance_scr_back_click")
                safePopBackstack()
            }
        )
    }
}