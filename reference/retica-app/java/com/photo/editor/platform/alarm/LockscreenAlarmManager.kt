package com.photo.editor.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.photo.editor.platform.receiver.LockscreenReceiver
import com.photo.editor.platform.app.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Calendar

object LockscreenAlarmManager {
    private const val TAG = "LockscreenAlarmManager"
    private const val LOCKSCREEN_6_RC = 400000

    fun scheduleLockscreens() = GlobalScope.launch(Dispatchers.Default) {
        val context = App.instance

        scheduleIndividualLockscreen(context, LOCKSCREEN_6_RC, 6)
    }

    private fun scheduleIndividualLockscreen(context: Context, requestCode: Int, atHour: Int, atMinute: Int = 0, extras: Bundle? = null) {
        val alarmIntent = Intent(context, LockscreenReceiver::class.java)
            .apply { extras?.let { putExtras(it) } }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, alarmIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Set time to show widget
        val currentTimeMillis = System.currentTimeMillis()
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, atHour)
            set(Calendar.MINUTE, atMinute)
            set(Calendar.SECOND, 0)

            // If current time is after set time, push set time back one day
            if (timeInMillis <= currentTimeMillis) add(Calendar.HOUR_OF_DAY, 24)
        }

        // Schedule alarm
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)

        Log.d(TAG, "scheduleIndividualLockscreen: lockscreen alarm scheduled at ${calendar.time}")
    }

    private fun cancelLockscreen(context: Context, requestCode: Int) {
        // Create a pending intent with the same intent and request code
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, LockscreenReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            // Cancel it with alarm manager
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }
}