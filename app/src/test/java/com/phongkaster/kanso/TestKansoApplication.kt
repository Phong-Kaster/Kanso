package com.phongkaster.kanso

import com.phongkaster.kanso.data.media.SavedPhotoSource
import com.phongkaster.kanso.di.appModule
import com.phongkaster.kanso.myedits.FakeSavedPhotoSource
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The application every Robolectric test starts (named in `robolectric.properties`): the real
 * [KansoApplication] with one dependency replaced, the source of the user's saved photos.
 *
 * Home and My edits read that source as soon as they appear. The real one asks the phone's
 * MediaStore, which on the JVM wakes up Robolectric's native SQLite on a background thread; that
 * thread can still be starting it when the next test's sandbox loads the same native code, and
 * the next test then fails before it begins. So a test that does not care about the saved photos
 * gets an empty fake, and a test that does swaps in its own with `loadKoinModules`, as before.
 */
class TestKansoApplication : KansoApplication() {

    /** The app's module, followed by a module whose saved-photo source is an empty fake. */
    override fun koinModules(): List<Module> = listOf(
        appModule,
        module { single<SavedPhotoSource> { FakeSavedPhotoSource() } },
    )
}
