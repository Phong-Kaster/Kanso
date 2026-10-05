package com.photo.editor.ui.screen.gallery

import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.appTextStyle
import com.photo.editor.common.util.logEvent
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.BaseBannerAdContent
import com.photo.editor.ui.activity.setLightStatusBar
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.component.shimmer.SimpleShimmer
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.gallery.model.GalleryBucket
import com.photo.gallery.model.GalleryImage
import com.photo.gallery.model.SortType
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class GalleryFragment : BaseComposeFragment() {
    private val viewModel: GalleryViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setLightStatusBar(true)
    }

    override fun onScreenView() {
        super.onScreenView()
        logEvent("gallery_scr")
    }

    override fun onStart() {
        super.onStart()

        viewModel.fetchMedia()
    }

    @Composable
    override fun ComposeView() {
        GalleryScreen(
            buckets = viewModel.imageBuckets.collectAsStateWithLifecycle().value,
            images = viewModel.allImages.collectAsStateWithLifecycle().value,
            onImageClick = {
                logEvent("gallery_scr_image_click")
                safeNavigate(GalleryFragmentDirections.actionGalleryToEdit(it.uri))
            },
            onBackClick = {
                logEvent("gallery_scr_back_click")
                safePopBackstack()
            }
        )
    }
}

@Composable
fun GalleryScreen(
    buckets: List<GalleryBucket> = listOf(),
    images: List<GalleryImage> = listOf(),
    onImageClick: (GalleryImage) -> Unit = {},
    onBackClick: () -> Unit = {},
    isSubScreen: Boolean = false,
) {
    var selectedBucket: GalleryBucket? by remember { mutableStateOf(null) }
    var selectedSortType: SortType by remember { mutableStateOf(SortType.LastModified) }
    val filteredImages by remember(images, selectedSortType) {
        derivedStateOf {
            val unsorted = if (selectedBucket == null) images else images.filter { it.bucketId == selectedBucket?.id }
            when (selectedSortType) {
                SortType.LastModified -> unsorted.sortedByDescending { it.modifiedTime }
                SortType.Name -> unsorted.sortedBy { it.name }
                SortType.Size -> unsorted.sortedByDescending { it.size }
            }
        }
    }

    BackHandler {
        onBackClick()
    }

    BaseScreen(
        topBar = {
            Column {
                GalleryTopBar(
                    buckets = buckets,
                    selectedBucket = selectedBucket,
                    onBucketSelected = { selectedBucket = it },
                    selectedSortType = selectedSortType,
                    onSortTypeSelected = { selectedSortType = it },
                    onBackClick = {
                        onBackClick()
                    },
                    isSubScreen = isSubScreen,
                )

                BaseBannerAdContent(bannerAdGroup = AdsProvider.bannerLib)
            }
        },
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 3.dp, end = 3.dp, bottom = 16.dp, top = 3.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filteredImages, key = { it.id }) {
                    GalleryImageItem(
                        galleryImage = it,
                        onClick = {
                            onImageClick(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                }
            }
        }

        if (filteredImages.isEmpty()) {
            CenterColumn(modifier = Modifier.fillMaxSize(), itemSpacing = 24.dp) {
                Icon(
                    painter = painterResource(com.photo.gallery.R.drawable.ic_image),
                    contentDescription = stringResource(R.string.image),
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .aspectRatio(1f),
                    tint = Color.Black,
                )

                Text(
                    text = stringResource(R.string.no_images),
                    style = appTextStyle(24, 600),
                    color = Color.Black,
                )
            }
        }
    }
}

@Composable
private fun GalleryImageItem(
    galleryImage: GalleryImage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var success by remember { mutableStateOf(false) }

    SubcomposeAsyncImage(
        model = galleryImage.uri,
        contentDescription = stringResource(R.string.thumbnail),
        modifier = modifier
            .fillMaxSize()
            .rounded(20.dp)
            .border(Dp.Hairline, Color(0xFF333333), RoundedCornerShape(20.dp))
            .clickable(success) { onClick() },
        contentScale = ContentScale.Crop,
        onSuccess = {
            success = true
        },
        error = {
            SimpleShimmer(
                content = {
                    Image(
                        painter = painterResource(com.photo.gallery.R.drawable.ic_image),
                        contentDescription = stringResource(R.string.none),
                        modifier = Modifier.fillMaxSize(),
                        alignment = Alignment.TopCenter,
                        contentScale = ContentScale.Crop,
                    )
                },
                modifier = Modifier.matchParentSize(),
            )
        },
        loading = { SimpleShimmer(modifier = Modifier.matchParentSize()) },
    )
}

@Preview
@Composable
private fun Preview() {
    GalleryScreen(
        images = listOf(
//            GalleryImage(
//                id = 7584,
//                uri = Uri.EMPTY,
//                path = "tacimates",
//                name = "Stacy Frazier",
//                modifiedTime = 2009,
//                width = 7899,
//                height = 2857,
//                size = 3672,
//                bucketId = 9732,
//                bucketName = "Inez Love"
//            )
        )
    )
}