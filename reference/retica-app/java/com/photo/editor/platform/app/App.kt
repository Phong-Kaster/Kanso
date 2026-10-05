package com.photo.editor.platform.app

import android.os.Build
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.ads.control.admob.Admob
import com.ads.control.ads.AperoAd
import com.ads.control.application.AdsMultiDexApplication
import com.ads.control.config.AdjustConfig
import com.ads.control.config.AperoAdConfig
import com.ads.control.config.AppsflyerConfig
import com.apero.firstopen.FirstOpenSDK
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.photo.editor.BuildConfig
import com.photo.editor.common.NetworkManager
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.data.repository.RemoteConfigRepository
import com.photo.editor.data.repository.SettingsRepository
import com.photo.editor.di.localMlModule
import com.photo.editor.platform.alarm.LockscreenAlarmManager
import com.photo.editor.platform.notification.AppNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration
import org.opencv.android.OpenCVLoader
import javax.inject.Inject
import org.koin.ksp.generated.defaultModule
import org.koin.ksp.generated.*

@OptIn(KoinExperimentalAPI::class)
class App : AdsMultiDexApplication(), KoinStartup {
    companion object {
        private const val TAG = "App"
        lateinit var instance: App
    }

    private val remoteConfigRepository: RemoteConfigRepository by inject()

    private val _appInitFinishedFlow = MutableStateFlow(false)
    val appInitFinishedFlow = _appInitFinishedFlow.asStateFlow()

    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@App)
        modules(defaultModule, localMlModule)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Firebase.crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.dev
        Firebase.analytics.setAnalyticsCollectionEnabled(!BuildConfig.dev)
        AppNotificationManager.createNotificationChannels(this)
        LockscreenAlarmManager.scheduleLockscreens()
        AppNotificationManager.sendShortcutNotification(this)

        GlobalScope.launch(Dispatchers.Default) {
            val loadLibDelegate = async { initOpenCV() }

            Coil.setImageLoader(ImageLoader(this@App).newBuilder().components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }.build())

            initAds()
            initResources()

            loadLibDelegate.await()

            _appInitFinishedFlow.value = true
        }

        setupTerasoftAdvertisement()
    }

    private fun setupTerasoftAdvertisement(){
        FirebaseApp.initializeApp(this)
        FirstOpenSDK.init(this)

        //Disables automatic dark mode applied by devices, especially for brands like Xiaomi, Redmi, etc.
        FirstOpenSDK.disableForceDark = true
    }

    private fun initOpenCV() {
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully")
        } else {
            Log.e(TAG, "OpenCV initialization failed!")
        }
    }

    /** Start initialize additional resources that wasn't initialized when launched to reduce app launch time */
    private fun initResources() {
        // Initialize global settings singleton
        NetworkManager.init(this)

        // Fetch remote config
        remoteConfigRepository.fetchRemoteConfig()
        RemoteConfig.init(remoteConfigRepository)
    }

    private fun initAds() {
        val environment = if (BuildConfig.dev) AperoAdConfig.ENVIRONMENT_DEVELOP
        else AperoAdConfig.ENVIRONMENT_PRODUCTION

        aperoAdConfig = AperoAdConfig(this, BuildConfig.ADS_API_KEY, AperoAdConfig.PROVIDER_ADMOB, environment)
        aperoAdConfig.mediationProvider = AperoAdConfig.PROVIDER_ADMOB
//        aperoAdConfig.idAdResume = BuildConfig.open_resume

        aperoAdConfig.listDeviceTest = listOf(
            "0DCCEBC8E5B21E68CE522D35BABFDD9C",
            "EC8ABFEF5903A15E2C9349B9EC46310C",
            "7FCE562E4BEE3404CD60EE47D430B3FC",
        )

        val appsflyerConfig = AppsflyerConfig(true, "2PUNpdyDTkedZTgeKkWCyB")
        aperoAdConfig.appsflyerConfig = appsflyerConfig
        
        val adjustConfig = AdjustConfig("b1m1ws1a9n9c")
        adjustConfig.eventAdImpression = "n4tji7"
        aperoAdConfig.adjustConfig = adjustConfig

        Admob.getInstance().setFan(false)
        Admob.getInstance().setAppLovin(false)
        Admob.getInstance().setColony(false)
        Admob.getInstance().setOpenActivityAfterShowInterAds(true)
        Admob.getInstance().setDisableAdResumeWhenClickAds(true)

        AperoAd.getInstance().init(this, aperoAdConfig, false)
    }
}