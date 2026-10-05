package com.photo.editor.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.photo.editor.data.datastore.RemoteConfigDatastore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single
import javax.inject.Inject

@Single
class RemoteConfigRepository(
    private val remoteConfigDatastore: RemoteConfigDatastore,
) {
    companion object {
        private const val TAG = "RemoteConfigRepository"
    }


    private val _fetchedTimestampFlow = MutableStateFlow(0L)
    val fetchedTimestampFlow = _fetchedTimestampFlow.asStateFlow()

    fun fetchRemoteConfig() {
        Log.d(TAG, "fetchRemoteConfig: fetching")

        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
            fetchTimeoutInSeconds = 5
        }

        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                remoteConfig.all.forEach { (key, value) ->
                    if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
                        remoteConfigDatastore.saveConfigValue(key, value.asString())
                    }
                }
            }
            Log.d(TAG, "fetchRemoteConfig: remote config fetched")

            _fetchedTimestampFlow.value = System.currentTimeMillis()
        }.addOnFailureListener {
            Log.d(TAG, "fetchRemoteConfig: failed to fetch remote config")
            _fetchedTimestampFlow.value = System.currentTimeMillis()
        }
    }

    fun getBooleanConfig(key: String, default: Boolean): Boolean {
        return remoteConfigDatastore.getBooleanConfig(key, default)
    }

    fun getStringConfig(key: String, default: String): String {
        return remoteConfigDatastore.getStringConfig(key, default)
    }

    fun getIntConfig(key: String, default: Int): Int {
        return remoteConfigDatastore.getIntConfig(key, default)
    }

    fun getDoubleConfig(key: String, default: Double): Double {
        return remoteConfigDatastore.getDoubleConfig(key, default)
    }
}