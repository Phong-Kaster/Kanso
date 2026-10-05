package com.photo.editor.ui.base

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import apero.aperosg.monetization.MonetizationConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase
import com.photo.editor.data.repository.SettingsRepository
import com.photo.editor.ui.compositionlocal.LocalLocale
import com.photo.editor.ui.compositionlocal.LocalNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

abstract class BaseComposeFragment : Fragment() {
    protected val TAG: String = javaClass.simpleName

    protected val settingsRepository: SettingsRepository by inject()

    private var fragmentViewedJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView")
        return ComposeView(requireActivity()).apply {
            setContent {
                CompositionLocalProvider(
                    LocalNavController provides findNavController(),
                    LocalLocale provides requireActivity().resources.configuration.locales[0],
                    *compositionLocalProvider().toTypedArray()
                ) {
                    MaterialTheme {
                        ComposeView()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        MonetizationConfig.interShownCurrentScreen = false
        attachFragmentViewedListener()
    }

    /** Because fragments often get rendered below ads,
     * lifecycle will go to resume state then immediately go to pause state,
     * this is not counted is fragment viewed.
     * Attach a job that will delay a moment before calling onScreenView
     * when resumed to ensure fragment is on top */
    private fun attachFragmentViewedListener() {
        fragmentViewedJob?.cancel()
        fragmentViewedJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                delay(200)
                withContext(Dispatchers.Main) {
                    onScreenView()
                }
                fragmentViewedJob?.cancel()
            }
        }
    }

    /** Function that will be called when screen is actually viewed */
    open fun onScreenView() {
        Log.d(TAG, "onScreenView")
    }

    @Composable
    protected open fun compositionLocalProvider(): List<ProvidedValue<*>> {
        return listOf()
    }

    fun toast(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
    }

    fun toast(textId: Int) {
        Toast.makeText(requireContext(), textId, Toast.LENGTH_LONG).show()
    }

    fun runOnUiThread(block: () -> Unit) {
        activity?.let { it.runOnUiThread { block() } }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")

        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, TAG)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    @Composable
    abstract fun ComposeView()
}