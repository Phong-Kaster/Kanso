package com.photo.editor.data.remoteconfig

import com.photo.editor.data.repository.RemoteConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Singleton class hold current remote config values */
object RemoteConfig {
    private val _commonConfigFlow = MutableStateFlow(CommonConfig())
    val commonConfigFlow = _commonConfigFlow.asStateFlow()
    val commonConfig: CommonConfig
        get() = commonConfigFlow.value

    private val _adsConfigFlow = MutableStateFlow(AdsConfig())
    val adsConfigFlow = _adsConfigFlow.asStateFlow()
    val adsConfig: AdsConfig
        get() = adsConfigFlow.value

    private val _adsLayoutConfig = MutableStateFlow(AdsLayoutConfig())
    val adsLayoutConfigFlow = _adsLayoutConfig.asStateFlow()
    val adsLayoutConfig: AdsLayoutConfig
        get() = adsLayoutConfigFlow.value

    private val _remoteConfigSet = MutableStateFlow(false)
    val remoteConfigSet = _remoteConfigSet.asStateFlow()

    fun init(remoteConfigRepository: RemoteConfigRepository) {
        _remoteConfigSet.value = false

        CoroutineScope(Dispatchers.IO).launch {
            remoteConfigRepository.fetchedTimestampFlow.collectLatest { fetchedTimestamp ->
                if (fetchedTimestamp == 0L) return@collectLatest

                _commonConfigFlow.value = CommonConfig(remoteConfigRepository)

                _adsConfigFlow.value = AdsConfig(remoteConfigRepository)

                _adsLayoutConfig.value = AdsLayoutConfig(remoteConfigRepository)

                _remoteConfigSet.value = true
            }
        }
    }
}