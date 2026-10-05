package com.photo.editor.ui.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.bundleOf
import com.photo.editor.common.util.logEvent
import com.photo.editor.platform.notification.AppNotificationManager
import com.photo.editor.ui.activity.MainActivity
import java.time.LocalDate

class LockscreenActivity : AppCompatActivity() {
    private var lockscreenType: Int by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logEvent("lockscreen_show")

        lockscreenType = LocalDate.now().toEpochDay().toInt() % 2

        showOnLockscreen()
        // Cancel notification right after activity shows up
        AppNotificationManager.cancelNotification(
            this,
            AppNotificationManager.LOCKSCREEN_NOTIFICATION_ID
        )

        setContent {
            MaterialTheme {
                LockscreenBaseScreen {
                    ComposeView()
                }
            }
        }
    }

    private fun showOnLockscreen() {
        setShowWhenLocked(true)
    }

    // Function to prompt user to unlock the screen
    private fun dismissKeyguard() {
        with(getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager) {
            requestDismissKeyguard(this@LockscreenActivity, null)
        }
    }

    private fun startMainActivityAndFinish() {
        val intent = Intent(this, MainActivity::class.java)
        // Set flags to clear the activity if active and start a new one
        // Feel free to change the flags to suit your needs
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        // Put any arguments here
        intent.putExtras(bundleOf())
        finish()
        dismissKeyguard()
        startActivity(intent)
    }

    @Composable
    private fun ComposeView() {
        when (lockscreenType) {
            0 -> {
                LockscreenContentFilter(
                    onContentClick = {
                        logEvent("lockscreen_content_click") {
                            param("lockscreen_type", "Filter")
                        }
                        startMainActivityAndFinish()
                    },
                    onDismissClick = {
                        logEvent("lockscreen_dismiss_click") {
                            param("lockscreen_type", "Filter")
                        }
                        finish()
                    },
                    onConfirmClick = {
                        logEvent("lockscreen_confirm_click") {
                            param("lockscreen_type", "Filter")
                        }
                        startMainActivityAndFinish()
                    },
                )
            }

            1 -> {
                LockscreenContentBrush(
                    onContentClick = {
                        logEvent("lockscreen_content_click") {
                            param("lockscreen_type", "Brush")
                        }
                        startMainActivityAndFinish()
                    },
                    onDismissClick = {
                        logEvent("lockscreen_dismiss_click") {
                            param("lockscreen_type", "Brush")
                        }
                        finish()
                    },
                    onConfirmClick = {
                        logEvent("lockscreen_confirm_click") {
                            param("lockscreen_type", "Brush")
                        }
                        startMainActivityAndFinish()
                    },
                )
            }
        }
    }
}