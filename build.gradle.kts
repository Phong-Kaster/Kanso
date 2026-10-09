// Top-level build file. It only declares the plugins (and their versions, from
// gradle/libs.versions.toml) that the modules apply; each module applies the ones it needs.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.roborazzi) apply false
}
