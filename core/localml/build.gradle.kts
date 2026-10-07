import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    // The KSP plugin (for the Koin annotation processor) was dropped: this module has no Kotlin
    // sources of its own, so the processor had nothing to process, and KSP's version is tied to
    // Kotlin's, which the app's newer libraries forced upward.
}

android {
    namespace = "com.photo.editor.localml"
    // 36 because Koin 4.2's AndroidX dependencies are built against API 36.
    compileSdk = 36

    defaultConfig {
        minSdk = 27

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }

    buildFeatures {
        buildConfig = false
        androidResources = false
        mlModelBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_22)
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.annotations)

    // ML Kit and Tensorflow
    implementation(libs.mlkit.faceMeshDetection)
    implementation(libs.mlkit.pose.detection)
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.mlkit.segmentation.selfie)
    implementation(libs.mlkit.subject.segmentation)

    // Tensor flow lite / Litert
    implementation(libs.litert)
    implementation(libs.litert.metadata)
    implementation(libs.litert.support)
    implementation(libs.litert.gpu)
    implementation(libs.litert.gpu.api)
    implementation(group = "local", name = "tensorflow-lite-select-tf-ops", ext = "aar")

    implementation(project(":core:common"))
    implementation(project(":opencv"))
    api(group = "local", name = "localml", ext = "aar")
}
