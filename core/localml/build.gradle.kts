plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.photo.editor.localml"
    compileSdk = 35

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

    kotlinOptions {
        jvmTarget = "22"
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
    ksp(libs.koin.ksp.compiler)

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
    implementation(group = "", name = "tensorflow-lite-select-tf-ops", ext = "aar")

    implementation(project(":core:common"))
    implementation(project(":opencv"))
    api(group = "", name = "localml", ext = "aar")
}