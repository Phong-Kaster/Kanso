package com.phongkaster.kanso.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.data.edit.ProcessorRegistry
import com.phongkaster.kanso.data.edit.RenderedPhotoStore
import com.phongkaster.kanso.data.edit.cutout.CutoutProcessor
import com.phongkaster.kanso.data.edit.cutout.EngineSegmenter
import com.phongkaster.kanso.data.edit.enhance.EngineUpscaler
import com.phongkaster.kanso.data.edit.artistic.ArtisticProcessor
import com.phongkaster.kanso.data.edit.artistic.EngineStylizer
import com.phongkaster.kanso.data.edit.enhance.EnhanceProcessor
import com.phongkaster.kanso.data.edit.retouch.EngineFaceFinder
import com.phongkaster.kanso.data.edit.retouch.EngineSkinSmoother
import com.phongkaster.kanso.data.edit.retouch.RetouchProcessor
import com.phongkaster.kanso.data.media.MediaStorePhotoSource
import com.phongkaster.kanso.data.media.MediaStoreSavedPhotoSource
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.media.SavedPhotoSource
import com.phongkaster.kanso.data.photo.MediaStorePhotoSaver
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.data.settings.SETTINGS_SCOPE
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.createSettingsDataStore
import com.phongkaster.kanso.launcher.LauncherIconSwitcher
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.home.HomeViewModel
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.camera.CameraViewModel
import com.phongkaster.kanso.ui.camera.CameraXController
import com.phongkaster.kanso.ui.myedits.MyEditsViewModel
import com.phongkaster.kanso.ui.result.ResultViewModel
import com.phongkaster.kanso.ui.settings.SettingsViewModel
import com.phongkaster.kanso.ui.tool.ToolViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The app's Koin module: the recipe book Koin uses to build objects when a screen asks for one.
 *
 * Koin is a dependency-injection library. Instead of a screen creating its ViewModel itself,
 * it asks Koin (with `koinViewModel()`), and Koin builds it from the recipes declared here,
 * filling in constructor arguments with `get()`. KansoApplication starts Koin with this module.
 *
 * - `single { ... }` builds one shared instance for the whole app.
 * - `viewModel { ... }` builds a ViewModel; Android keeps it in a ViewModelStore so it survives
 *   rotation, and clears it when its owner (here, a navigation entry) goes away for good.
 */
val appModule = module {
    single { ViewModelClearedProbe() }

    viewModel { HomeViewModel(probe = get(), savedPhotos = get()) }

    // The photo library behind one small interface, so tests can swap in a fake list of photos.
    single<PhotoSource> { MediaStorePhotoSource(androidContext()) }

    // The photos Kanso itself saved (the Pictures/Kanso folder), behind one small interface so tests
    // can swap in a fake list. It never asks for a permission.
    single<SavedPhotoSource> { MediaStoreSavedPhotoSource(androidContext()) }

    // The My edits tab's ViewModel reads that list each time the tab appears.
    viewModel { MyEditsViewModel(savedPhotos = get(), probe = get()) }

    // Switches the launcher icon's background to follow the theme colour.
    single { LauncherIconSwitcher(androidContext()) }

    // Decodes one photo for the tool screen: upright and no bigger than the screen needs.
    single { PhotoLoader(androidContext()) }

    // The gallery ViewModel also receives the tool the user came from.
    viewModel { params -> GalleryViewModel(tool = params.get(), probe = get(), photoSource = get()) }

    // The tool screen's ViewModel receives the tool and the photo (as text) from the navigation key.
    viewModel { params ->
        ToolViewModel(
            tool = params.get(),
            photoUri = params.get(),
            loader = get(),
            probe = get(),
            registry = get(),
            store = get(),
        )
    }

    // Which processor edits the photo for each tool; a tool whose engine is not built yet has none.
    single {
        ProcessorRegistry(
            mapOf(
                Tool.REMOVE_BACKGROUND to CutoutProcessor(EngineSegmenter()),
                Tool.ENHANCE to EnhanceProcessor(EngineUpscaler(androidContext())),
                Tool.RETOUCH to RetouchProcessor(EngineFaceFinder(androidContext()), EngineSkinSmoother()),
                Tool.ARTISTIC to ArtisticProcessor(EngineStylizer(androidContext())),
            ),
        )
    }

    // The picture Done renders waits here for the Result screen (A-004).
    single { RenderedPhotoStore() }

    // The Result screen's ViewModel. After Done it gets no parameters and reads the hand-off store;
    // the My edits viewer passes the photo's Uri (as text) and `true`.
    viewModel { params ->
        ResultViewModel(
            photoUri = params.getOrNull<String>(),
            viewer = params.getOrNull<Boolean>() ?: false,
            store = get(),
            loader = get(),
            saver = get(),
            probe = get(),
        )
    }

    // Writes the finished photo into Pictures/Kanso on the phone.
    single<PhotoSaver> { MediaStorePhotoSaver(androidContext()) }

    // The camera behind one small interface, so tests can swap in a fake that needs no hardware.
    // A factory builds a new controller for every camera screen, so each starts clean.
    factory<CameraController> { CameraXController(androidContext()) }

    viewModel { CameraViewModel(controller = get(), probe = get()) }

    // The scope the settings file is read and written in. It is a named singleton so a test can
    // cancel it, which releases the file, and then open the same file again like a restart.
    single(SETTINGS_SCOPE) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    // The settings live in one small file in the app's private storage (Preferences DataStore).
    single<DataStore<Preferences>> {
        createSettingsDataStore(
            file = androidContext().filesDir.resolve("datastore/settings.preferences_pb"),
            scope = get<CoroutineScope>(SETTINGS_SCOPE),
        )
    }

    single { SettingsRepository(dataStore = get()) }

    viewModel { SettingsViewModel(repository = get()) }
}
