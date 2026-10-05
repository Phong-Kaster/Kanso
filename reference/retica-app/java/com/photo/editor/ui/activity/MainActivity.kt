package com.photo.editor.ui.activity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.photo.editor.R
import com.photo.editor.ui.base.BaseActivity
import com.photo.editor.ui.component.bottomsheet.PermissionBottomSheet
import com.photo.editor.common.util.checkCameraPermission
import com.photo.editor.common.util.checkNotificationPermission
import com.photo.editor.common.util.checkStoragePermissions
import com.photo.editor.common.util.getStoragePermissions
import com.photo.editor.common.util.logEvent
import com.photo.editor.common.util.tryWithoutCatch
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.platform.notification.AppNotificationManager

class MainActivity : BaseActivity() {
    private lateinit var navController: NavController

    companion object {
        var showPermissionBottomSheet by mutableStateOf(false)
        var notificationPermissionGranted by mutableStateOf(true)
        var storagePermissionGranted by mutableStateOf(true)
        var cameraPermissionGranted by mutableStateOf(true)
    }

    private val requestStoragePermissionsLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { grantedMaps ->
            val isPermissionGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                grantedMaps.all { it.value }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    grantedMaps.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false) ||
                            grantedMaps.getOrDefault(
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                                false
                            )
                } else {
                    grantedMaps.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false)
                }
            }

            if (isPermissionGranted) logEvent("storage_permission_accept")
            else logEvent("storage_permission_deny")
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(RequestPermission()) { granted ->
            if (granted) {
                logEvent("noti_permission_accept")
                AppNotificationManager.sendShortcutNotification(this)
            } else {
                logEvent("noti_permission_deny")
            }
        }

    private val cameraPermissionRequest =
        registerForActivityResult(RequestPermission()) { granted ->
            if (granted) logEvent("camera_permission_accept")
            else logEvent("camera_permission_deny")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setLightStatusBar(true)
        setContentView(R.layout.activity_main)

        initView()
    }

    private fun initView() {
        // Get nav fragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        configureSystemBars()

        findViewById<ComposeView>(R.id.composeView).setContent {
            MaterialTheme {
                ComposeView()
            }
        }
    }

    private fun configureSystemBars() = tryWithoutCatch {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // From Android API 35, there is a bug causing status bar to have space when recreating activity
        // This code is a hack to make content draw behind status bar again
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }

        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        configureSystemBars()
    }

    override fun onResume() {
        super.onResume()
        configureSystemBars()

        cameraPermissionGranted = checkCameraPermission(this)
        storagePermissionGranted = checkStoragePermissions(this)
        notificationPermissionGranted = checkNotificationPermission(this)
    }

    @Composable
    private fun ComposeView() {
        PermissionBottomSheet(
            showBottomSheet = showPermissionBottomSheet,
            onDismissRequest = { showPermissionBottomSheet = false },
            notificationPermissionGranted = notificationPermissionGranted,
            storagePermissionGranted = storagePermissionGranted,
            cameraPermissionGranted = cameraPermissionGranted,
            onGrantNotificationPermission = {
                logEvent("permission_bottom_sheet_noti_click")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        tryWithoutCatch {
                            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            })
                        }
                    } else {
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    AdsProvider.nativeBottomSheet.loadAds(this)
                }
            },
            onGrantStoragePermission = {
                logEvent("permission_bottom_sheet_library_click")
                val permissions = getStoragePermissions()
                if (permissions.any { shouldShowRequestPermissionRationale(it) }) {
                    tryWithoutCatch {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        })
                    }
                } else {
                    requestStoragePermissionsLauncher.launch(permissions.toTypedArray())
                }

                AdsProvider.nativeBottomSheet.loadAds(this)
            },
            onGrantCameraPermission = {
                logEvent("permission_bottom_sheet_camera_click")
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    tryWithoutCatch {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        })
                    }
                } else {
                    cameraPermissionRequest.launch(Manifest.permission.CAMERA)
                }

                AdsProvider.nativeBottomSheet.loadAds(this)
            }
        )
    }
}