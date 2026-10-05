package com.photo.editor.ui.component.bottomsheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import apero.aperosg.monetization.MonetizationConfig
import apero.aperosg.monetization.ui.NativeAdContent
import com.photo.editor.R
import com.photo.editor.common.ui.bottomsheet.BaseBottomSheet
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.Neutral300
import com.photo.editor.common.ui.theme.Neutral800
import com.photo.editor.common.ui.theme.extraBold14
import com.photo.editor.common.ui.theme.regular14
import com.photo.editor.common.util.logEvent
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.monetization.AdsProvider

@Composable
fun PermissionBottomSheet(
    showBottomSheet: Boolean,
    onDismissRequest: () -> Unit,
    notificationPermissionGranted: Boolean,
    storagePermissionGranted: Boolean,
    cameraPermissionGranted: Boolean,
    onGrantNotificationPermission: () -> Unit,
    onGrantStoragePermission: () -> Unit,
    onGrantCameraPermission: () -> Unit,
) {
    BaseBottomSheet(
        showBottomSheet = showBottomSheet,
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        LaunchedEffect(Unit) {
            // If bottom sheet shows, it counts as a new screen
            // Reset inter shown flag to false
            MonetizationConfig.interShownCurrentScreen = false
            logEvent("permission_bottom_sheet")
        }

        PermissionBottomSheetContent(
            notificationPermissionGranted = notificationPermissionGranted,
            storagePermissionGranted = storagePermissionGranted,
            cameraPermissionGranted = cameraPermissionGranted,
            onGrantNotificationPermission = onGrantNotificationPermission,
            onGrantStoragePermission = onGrantStoragePermission,
            onGrantCameraPermission = onGrantCameraPermission,
        )
    }
}

@Composable
private fun PermissionBottomSheetContent(
    notificationPermissionGranted: Boolean,
    storagePermissionGranted: Boolean,
    cameraPermissionGranted: Boolean,
    onGrantNotificationPermission: () -> Unit,
    onGrantStoragePermission: () -> Unit,
    onGrantCameraPermission: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.permission_bottomsheet),
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .size(140.dp)
        )

        CenterRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            itemSpacing = 16.dp
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notification),
                    style = extraBold14,
                    color = Color.Black,
                )

                Text(
                    text = stringResource(R.string.permission_bs_notification),
                    style = regular14,
                    color = Neutral800,
                    lineHeight = 20.sp,
                )
            }

            AppSwitch(
                checked = notificationPermissionGranted,
                onCheckedChange = { onGrantNotificationPermission() },
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = Neutral300, modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

        CenterRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            itemSpacing = 16.dp,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.photo_library),
                    style = extraBold14,
                    color = Color.Black,
                )
                Text(
                    text = stringResource(R.string.permission_bs_library),
                    style = regular14,
                    color = Neutral800,
                    lineHeight = 20.sp,
                )
            }

            AppSwitch(
                checked = storagePermissionGranted,
                onCheckedChange = { onGrantStoragePermission() },
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = Neutral300, modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

        CenterRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            itemSpacing = 16.dp,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.camera_access),
                    style = extraBold14,
                    color = Color.Black,
                )
                Text(
                    text = stringResource(R.string.permission_bs_camera),
                    style = regular14,
                    color = Neutral800,
                    lineHeight = 20.sp,
                )
            }

            AppSwitch(
                checked = cameraPermissionGranted,
                onCheckedChange = { onGrantCameraPermission() },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val baseAdmobLayoutId = if (RemoteConfig.adsLayoutConfig.admob_layout_cta_above_native_bottom_sheet) R.layout.native_ads_medium_above
        else R.layout.native_ads_medium
        val baseMetaLayoutId = if (RemoteConfig.adsLayoutConfig.meta_layout_cta_above_native_bottom_sheet) R.layout.native_ads_medium_meta_above
        else R.layout.native_ads_medium_meta
        val admobLayoutId = if (RemoteConfig.adsLayoutConfig.force_meta_layout_bottom_sheet) baseMetaLayoutId else baseAdmobLayoutId
        val metaLayoutId = if (RemoteConfig.adsLayoutConfig.force_meta_layout_bottom_sheet) baseAdmobLayoutId else baseMetaLayoutId

        NativeAdContent(
            adGroup = AdsProvider.nativeBottomSheet,
            layoutId = admobLayoutId,
            metaLayoutId = metaLayoutId,
            reloadOnLifecycleEvent = Lifecycle.Event.ON_RESUME,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3F3F3), rounded(10.dp))
        )
    }
}

@Composable
private fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier
            .width(48.dp)
            .height(24.dp),
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color.Black,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFC0C0C0),
            uncheckedBorderColor = Color(0xFFC0C0C0),
        )
    )
}

@Preview
@Composable
private fun Preview() {
    PermissionBottomSheetContent(
        notificationPermissionGranted = true,
        storagePermissionGranted = true,
        cameraPermissionGranted = false,
        onGrantNotificationPermission = { },
        onGrantStoragePermission = { },
        onGrantCameraPermission = {}
    )
}