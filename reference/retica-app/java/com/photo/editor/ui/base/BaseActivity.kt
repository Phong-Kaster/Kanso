package com.photo.editor.ui.base

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ironsource.mediationsdk.IronSource
import com.photo.editor.data.repository.SettingsRepository
import org.koin.android.ext.android.inject

abstract class BaseActivity : AppCompatActivity() {
    protected val TAG = this.javaClass.simpleName

    protected val settingsRepository: SettingsRepository by inject()

    // Override activity context with no font scale
    override fun attachBaseContext(newBase: Context?) {
        var context = newBase
        if (newBase != null) context = newBase.createConfigurationContext(
            newBase.resources.configuration.apply {
                fontScale = 1f
            }
        )
        super.attachBaseContext(context)
    }

    override fun onResume() {
        super.onResume()
        IronSource.onResume(this)
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        IronSource.onPause(this)
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }
}