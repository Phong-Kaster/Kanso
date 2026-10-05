package com.photo.editor.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single

@Single
class RemoteConfigDatastore(
    context: Context
) {
    companion object {
        private const val PREF_NAME = "remoteConfig"
    }

    private val Context.datastore: DataStore<Preferences> by preferencesDataStore(name = PREF_NAME)
    private val datastore = context.datastore

    fun saveConfigValue(key: String, rawValue: String) = runBlocking {
        datastore.edit { it[stringPreferencesKey(key)] = rawValue }
    }

    fun getBooleanConfig(key: String, default: Boolean = true): Boolean = runBlocking {
        try {
            val stringValue = datastore.data.first()[stringPreferencesKey(key)]
            stringValue?.toBoolean() ?: default
        } catch (ex: Exception) {
            default
        }
    }

    fun getIntConfig(key: String, default: Int): Int = runBlocking {
        try {
            val stringValue = datastore.data.first()[stringPreferencesKey(key)]
            stringValue?.toInt() ?: default
        } catch (ex: Exception) {
            default
        }
    }

    fun getFloatConfig(key: String, default: Float): Float = runBlocking {
        try {
            val stringValue = datastore.data.first()[stringPreferencesKey(key)]
            stringValue?.toFloat() ?: default
        } catch (ex: Exception) {
            default
        }
    }

    fun getStringConfig(key: String, default: String): String = runBlocking {
        try {
            datastore.data.first()[stringPreferencesKey(key)] ?: default
        } catch (ex: Exception) {
            default
        }
    }

    fun getDoubleConfig(key: String, default: Double): Double = runBlocking {
        try {
            val stringValue = datastore.data.first()[stringPreferencesKey(key)]
            stringValue?.toDouble() ?: default
        } catch (ex: Exception) {
            default
        }
    }
}