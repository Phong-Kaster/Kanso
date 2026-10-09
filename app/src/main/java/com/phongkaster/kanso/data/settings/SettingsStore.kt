package com.phongkaster.kanso.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named

/**
 * The Koin name of the CoroutineScope the settings DataStore runs in.
 *
 * Cancelling that scope shuts the DataStore down and releases its file, so a test can open the
 * same file again with a fresh DataStore, just as the app does after a restart.
 */
val SETTINGS_SCOPE: Qualifier = named("settings-scope")

/**
 * Creates the Preferences DataStore that keeps Kanso's settings in [file].
 *
 * A Preferences DataStore is a small key-value store saved as one file: you read it as a Flow
 * (a stream that emits the stored values again every time they change) and write it with
 * `edit { }`, which runs off the main thread and saves the whole file safely. Only one DataStore
 * may be open on a file at a time; it stays open until [scope], where its background work runs,
 * is cancelled.
 */
fun createSettingsDataStore(file: File, scope: CoroutineScope): DataStore<Preferences> {
    // DataStore writes a temporary file and then moves it over the real one.
    file.absoluteFile.parentFile?.mkdirs()
    return DataStoreFactory.create(
        // The storage does the file work. The Okio-based one is used on purpose: its "move over
        // the old file" step also works on Windows, where the default one (File.renameTo) fails
        // once the file exists. That matters for the JVM tests run on a Windows machine; on
        // Android both behave the same. PreferencesSerializer turns the key-value pairs into
        // bytes and back.
        storage = OkioStorage(
            fileSystem = RetryingMoveFileSystem(FileSystem.SYSTEM),
            serializer = PreferencesSerializer,
            producePath = { file.absolutePath.toPath() },
        ),
        // If the file cannot be read as settings (for example it was cut short by a crash), start
        // over with empty settings, so every setting falls back to its default instead of the
        // app failing to open.
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
    )
}
