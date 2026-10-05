// Where Gradle finds its plugins (Android, Kotlin, Compose compiler, Roborazzi): public
// repositories only.
pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// Where every module finds its libraries. Modules may not declare repositories of their own.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // The engine's prebuilt AARs live in libs/. The core modules ask for them as
        // `group = "", name = "<file>", ext = "aar"`, which a flat directory repository resolves to
        // libs/<file>.aar.
        flatDir {
            dirs("$settingsDir/libs")
        }
    }
}

rootProject.name = "Kanso"

include(":app")
include(":opencv")
include(":core:colorpicker")
include(":core:common")
include(":core:editimage")
include(":core:gallery")
include(":core:imagecrop")
include(":core:imageprocessor")
include(":core:localml")
include(":core:overlaybrush")
