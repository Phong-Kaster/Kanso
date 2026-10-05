package com.phongkaster.kanso

import android.app.Application

/**
 * The Kanso application: the one object Android creates for the whole app, before any screen.
 *
 * It is declared in AndroidManifest.xml (`android:name=".KansoApplication"`), so Android creates
 * it once per process. App-wide setup belongs here; dependency injection (Koin) will be started
 * here once the app has objects to provide.
 */
class KansoApplication : Application()
