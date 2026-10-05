package com.photo.editor.platform.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import com.photo.editor.BuildConfig
import com.photo.editor.R
import com.photo.editor.ui.navigation.NavigationConstant
import com.photo.editor.ui.screen.splash.SplashFragmentArgs
import com.photo.editor.util.ContextWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppNotificationManager {
    private const val TAG = "AppNotificationManager"

    const val DEFAULT_CHANNEL_ID = "default"
    private const val DEFAULT_CHANNEL_NAME = "Default"
    private const val DEFAULT_CHANNEL_DESC = "Default Notification Channel"

    const val SHORTCUT_CHANNEL_ID = "shortcut"
    private const val SHORTCUT_CHANNEL_NAME = "Shortcut"
    private const val SHORTCUT_CHANNEL_DESC = "Shortcut Notification Channel"

    const val SHORTCUT_NOTIFICATION_ID = 100000
    const val DAILY_NOTIFICATION_ID = 100001
    const val LOCKSCREEN_NOTIFICATION_ID = 100002

    const val CAMERA_PENDING_IC = 100000
    const val EDIT_PENDING_IC = 100001
    const val HOME_PENDING_IC = 100002
    const val DAILY_NOTIFICATION_PENDING_IC = 100003

    fun createNotificationChannels(context: Context) {
        // Default notification channel
        val defaultChannel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            DEFAULT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            description = DEFAULT_CHANNEL_DESC
            enableLights(true)
            lightColor = 0x93D5E1
            vibrationPattern = longArrayOf(1000, 500, 1000, 500)
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttr)
        }

        // Shortcut channel
        val shortcutChannel = NotificationChannel(
            SHORTCUT_CHANNEL_ID,
            SHORTCUT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = SHORTCUT_CHANNEL_DESC
            setShowBadge(false)
        }

        with(NotificationManagerCompat.from(context)) {
            createNotificationChannel(defaultChannel)
            createNotificationChannel(shortcutChannel)
        }
    }

    fun sendShortcutNotification(base: Context) = CoroutineScope(Dispatchers.Default).launch {
        val context = ContextWrapper(base).withAppLanguage()

        val notificationLayout = RemoteViews(BuildConfig.APPLICATION_ID, R.layout.notification_ongoing)

        val cameraIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.app_graph)
            .setDestination(R.id.splashFragment)
            .setArguments(
                SplashFragmentArgs(
                    destination = NavigationConstant.DESTINATION_CAMERA,
                    origin = NavigationConstant.ORIGIN_ONGOING_NOTIFICATION
                ).toBundle()
            )
            .createPendingIntent()

        val editIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.app_graph)
            .setDestination(R.id.splashFragment)
            .setArguments(
                SplashFragmentArgs(
                    destination = NavigationConstant.DESTINATION_EDIT,
                    origin = NavigationConstant.ORIGIN_ONGOING_NOTIFICATION
                ).toBundle()
            )
            .createPendingIntent()

        val homeIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.app_graph)
            .setDestination(R.id.splashFragment)
            .setArguments(
                SplashFragmentArgs(
                    origin = NavigationConstant.ORIGIN_ONGOING_NOTIFICATION
                ).toBundle()
            )
            .createPendingIntent()

        notificationLayout.setOnClickPendingIntent(R.id.cameraButton, cameraIntent)
        notificationLayout.setOnClickPendingIntent(R.id.editButton, editIntent)

        notificationLayout.setTextViewText(R.id.captureTv, context.getString(R.string.capture))
        notificationLayout.setTextViewText(R.id.editTv, context.getString(R.string.edit))

        val notification = NotificationCompat.Builder(context, SHORTCUT_CHANNEL_ID)
            .setContentTitle(context.getText(R.string.app_name))
            .setSmallIcon(R.drawable.app_icon_no_bg)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(homeIntent)
            .setColorized(true)
            .setColor(0xFFFD8258.toInt())
            .setOngoing(true)
            .setSilent(true)
            .setGroup("ongoing")
            .build()

        sendNotification(context, SHORTCUT_NOTIFICATION_ID, notification)
    }

    fun sendNotification(
        context: Context,
        notificationId: Int,
        notification: Notification
    ): Boolean {
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }

            notify(notificationId, notification)
            return true
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        with(NotificationManagerCompat.from(context)) {
            cancel(notificationId)
        }
    }
}