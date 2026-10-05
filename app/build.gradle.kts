import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    // Generates serializers for the @Serializable navigation keys.
    alias(libs.plugins.serialization)
    // Screenshot tests on the JVM (verifyRoborazziDebug).
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.phongkaster.kanso"
    // 36 because Navigation 3, lifecycle 2.10 and the current Compose libraries are built against it.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.phongkaster.kanso"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

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
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    // Hosts composables in an empty activity for Compose tests (debug builds only).
    debugImplementation(libs.ui.test.manifest)
}
