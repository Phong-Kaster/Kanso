import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    // Generates serializers for the @Serializable navigation keys.
    alias(libs.plugins.serialization)
    // Screenshot tests on the JVM (verifyRoborazziDebug).
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.phongkaster.kanso"
    // 37, the newest SDK: current AndroidX releases (for example Navigation 3 1.2 and Material3 1.5)
    // are built against it, and the build refuses them at a lower compileSdk.
    // targetSdk below stays 36: it decides which Android behaviour changes apply at run time.
    compileSdk = 37
    // The NDK that strips the prebuilt native libraries (OpenCV, MediaPipe, LiteRT) and supplies
    // libc++_shared.so, as before the Android Gradle Plugin 9 move. Its own default is a newer NDK,
    // which would change libc++_shared.so in the APK for all three ABIs, and so what runs on the
    // 32-bit phone.
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.phongkaster.kanso"
        minSdk = 27
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Only the ABIs a person runs the app on: with every ABI the debug APK is ~287 MB and
            // will not install on the emulator. x86_64 is the emulator; arm64-v8a is a 64-bit phone;
            // armeabi-v7a is a phone with a 32-bit Android, such as the Galaxy A13 (SM-A135F), which
            // refused the APK without it (INSTALL_FAILED_NO_MATCHING_ABIS). Release keeps all ABIs.
            ndk {
                abiFilters += listOf("x86_64", "arm64-v8a", "armeabi-v7a")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }

    buildFeatures {
        compose = true
    }

    packaging {
        // Several engine libraries (OpenCV, LiteRT, MediaPipe) ship their own copy of the C++ runtime.
        jniLibs {
            pickFirsts += "**/libc++_shared.so"
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            // Robolectric needs the app's merged resources and manifest to start MainActivity.
            isIncludeAndroidResources = true
            // Many Robolectric screens (camera, gallery, photos) run in one test JVM; the default
            // heap runs out and the JVM exits. Give it room and start a fresh JVM every 40 classes.
            all {
                it.maxHeapSize = "3g"
                it.setForkEvery(40)
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_22)
    }
}

dependencies {
    // The engine: every core module and OpenCV.
    implementation(project(":core:colorpicker"))
    implementation(project(":core:common"))
    implementation(project(":core:editimage"))
    implementation(project(":core:gallery"))
    implementation(project(":core:imagecrop"))
    implementation(project(":core:imageprocessor"))
    implementation(project(":core:localml"))
    implementation(project(":core:overlaybrush"))
    implementation(project(":opencv"))

    // AndroidX
    implementation(libs.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)

    // Compose (versions from the Compose BOM)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    // The Compose BOM above would give Material3 1.4.0; the app asks for the newer one itself, so the
    // Expressive components of 1.5.0-beta01 are available. Only the app does: the engine modules keep
    // the older entry.
    implementation(libs.compose.material3.app)
    // @Preview and @PreviewLightDark: the annotations that make a composable show up in Android
    // Studio's preview pane. Light enough to ship, because the annotations are not used at run time.
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Photo thumbnails in the gallery grid (Coil loads and caches images off the main thread)
    implementation(libs.coil.compose)
    // Reads a photo's EXIF orientation so it is shown upright
    implementation(libs.androidx.exifinterface)

    // Camera: live preview and taking a shot (CameraX)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Seed colour -> full Material 3 colour scheme
    implementation(libs.material.kolor)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.navigation3)

    // Unit tests on the JVM (Robolectric, Compose UI test, Roborazzi)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.espresso.core)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.ui.test.junit4)
    // Finds every @Preview in the app so a test can draw each one (PreviewsRenderTest).
    testImplementation(libs.composable.preview.scanner)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    // Hosts composables in an empty activity for Compose tests (debug builds only).
    debugImplementation(libs.ui.test.manifest)
}
