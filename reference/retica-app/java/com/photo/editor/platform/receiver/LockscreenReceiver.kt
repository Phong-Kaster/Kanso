package com.photo.editor.platform.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.photo.editor.R
import com.photo.editor.common.util.logEvent
import com.photo.editor.platform.alarm.LockscreenAlarmManager
import com.photo.editor.platform.notification.AppNotificationManager
import com.photo.editor.ui.lockscreen.LockscreenActivity
import com.photo.editor.util.ContextWrapper
import java.time.LocalDate

class LockscreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        logEvent("lockscreen_received")

        sendLockscreenNotification(context, intent)
    }

    @Suppress("NAME_SHADOWING")
    private fun sendLockscreenNotification(context: Context, intent: Intent) {
        // Localize context
        val context = ContextWrapper(context).withAppLanguage()
        val lockscreenType = LocalDate.now().toEpochDay().toInt() % 2

        val fullScreenIntent = Intent(context, LockscreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.extras?.let { putExtras(it) }
            putExtra("lockscreenType", lockscreenType)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Determine which notification content
        val notificationContent = when (lockscreenType) {
            0 -> R.string.lockscreen_filter_2
            else -> R.string.lockscreen_brush_1
        }.let { context.getString(it) }

        val notificationBuilder = NotificationCompat.Builder(context, AppNotificationManager.DEFAULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon_no_bg)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(notificationContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)

        // Cancel old notification
        AppNotificationManager.cancelNotification(context, AppNotificationManager.LOCKSCREEN_NOTIFICATION_ID)
        // Send new notification
        val notificationSent = AppNotificationManager.sendNotification(
            context = context,
            notificationId = AppNotificationManager.LOCKSCREEN_NOTIFICATION_ID,
            notification = notificationBuilder.build()
        )

        if (!notificationSent) {
            logEvent("lockscreen_no_permission")
        } else {
            logEvent("lockscreen_sent")
        }

        // Reschedule new alarm
        LockscreenAlarmManager.scheduleLockscreens()
    }
}