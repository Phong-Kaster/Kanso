package com.phongkaster.kanso

import android.app.Application
import com.phongkaster.kanso.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * The Kanso application: the one object Android creates for the whole app, before any screen.
 *
 * It is declared in AndroidManifest.xml (`android:name=".KansoApplication"`), so Android creates
 * it once per process. App-wide setup belongs here; it starts Koin, the dependency-injection
 * library that builds the objects (such as ViewModels) the screens ask for.
 */
class KansoApplication : Application() {

    /** Starts Koin with the app's module, once, before any activity exists. */
    override fun onCreate() {
        super.onCreate()
        // Koin's state is global to the JVM. A real process starts it once; tests that create a
        // fresh Application in the same JVM need a clean slate, and stopKoin does nothing if idle.
        stopKoin()
        startKoin {
            androidContext(this@KansoApplication)
            modules(appModule)
        }
    }
}
