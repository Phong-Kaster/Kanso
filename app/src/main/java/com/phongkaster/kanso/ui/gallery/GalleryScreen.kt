package com.phongkaster.kanso.ui.gallery

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.ui.common.BackArrowIcon
import com.phongkaster.kanso.ui.permission.AccessRefusedPanel
import com.phongkaster.kanso.ui.permission.PermissionGate
import com.phongkaster.kanso.ui.permission.fullPhotoPermissionFor
import com.phongkaster.kanso.ui.permission.openAppDetailsSettings
import com.phongkaster.kanso.ui.permission.partialPhotoPermissionsFor
import com.phongkaster.kanso.ui.permission.photoPermissionsFor

/** The test tag on the root of the gallery screen, present in every permission state. */
const val GALLERY_SCREEN_TAG = "gallery-screen"

/** The test tag on the "photo access is off" explanation. */
const val GALLERY_REFUSED_TAG = "gallery-refused"

/** The test tag on the notice that only some photos are shared (Android 14+). */
const val GALLERY_PARTIAL_NOTICE_TAG = "gallery-partial-notice"

/** The test tag on the "share more photos" button of the partial-access notice. */
const val GALLERY_SHARE_MORE_TAG = "gallery-share-more"

/** The test tag on the button that opens the app's page in the system settings. */
const val GALLERY_SETTINGS_BUTTON_TAG = "gallery-settings-button"

/** The test tag on the area that shows the photos once access is granted. */
const val GALLERY_BODY_TAG = "gallery-body"

/**
 * The photo gallery of a tool: a top bar titled "<tool>: choose a photo" with a back arrow, and
 * below it the photos, once the person has let Kanso read them.
 *
 * The first time it is shown it asks for the photo permission that fits the phone's Android
 * version (see [photoPermissionsFor]) through [PermissionGate]; the app never asks at launch.
 * With full access it shows the photos; with partial access (Android 14+ "Select photos") it
 * shows them under a notice with a "share more" button; after a refusal it explains why access
 * is needed and offers a button to the app's system settings.
 *
 * [onBack] is called when the user taps the back arrow; the caller pops the back stack.
 * The [viewModel] is passed in by the navigation entry, which owns it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toolName = stringResource(viewModel.tool.labelRes)
    val context = LocalContext.current
    val sdkInt = Build.VERSION.SDK_INT

    Scaffold(
        modifier = modifier.testTag(GALLERY_SCREEN_TAG),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gallery_title, toolName)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = BackArrowIcon,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar (and system bars) take; the content avoids it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PermissionGate(
                permissions = photoPermissionsFor(sdkInt),
                fullAccess = listOf(fullPhotoPermissionFor(sdkInt)),
                partialAccess = partialPhotoPermissionsFor(sdkInt),
                granted = { GalleryBody(Modifier.fillMaxSize()) },
                partial = { shareMore ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        PartialAccessNotice(onShareMore = shareMore)
                        GalleryBody(Modifier.weight(1f).fillMaxWidth())
                    }
                },
                refused = {
                    AccessRefusedPanel(
                        title = stringResource(R.string.gallery_refused_title),
                        message = stringResource(R.string.gallery_refused_message),
                        buttonLabel = stringResource(R.string.gallery_open_settings),
                        onOpenSettings = { context.openAppDetailsSettings() },
                        modifier = Modifier.testTag(GALLERY_REFUSED_TAG),
                        buttonModifier = Modifier.testTag(GALLERY_SETTINGS_BUTTON_TAG),
                    )
                },
            )
        }
    }
}

/**
 * The area that shows the photos. For now it holds a short note; the photo grid replaces it.
 */
@Composable
private fun GalleryBody(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag(GALLERY_BODY_TAG)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.gallery_body_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The banner shown when only some photos are shared (Android 14+): it says so and offers a
 * button that calls [onShareMore], which asks for photo access again so the person can share
 * more photos or all of them.
 */
@Composable
private fun PartialAccessNotice(onShareMore: () -> Unit) {
    // secondaryContainer with onSecondaryContainer text is a Material 3 colour pair, so the
    // notice stays readable in light, dark and custom colours.
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(GALLERY_PARTIAL_NOTICE_TAG),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.gallery_partial_notice),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = onShareMore,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag(GALLERY_SHARE_MORE_TAG),
            ) {
                Text(stringResource(R.string.gallery_share_more))
            }
        }
    }
}
