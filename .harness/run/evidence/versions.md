# Versions at Verify time (DoD 13, DoD 8, DoD 9, DoD 12)

Made on 2026-10-09 by `bash` with `curl` against the Maven metadata of Google Maven (`dl.google.com/dl/android/maven2`)
and Maven Central, comparing the catalog's version (`gradle/libs.versions.toml` at the Phase 3 checkpoint) with the
newest **stable** release (digits and dots only; `-alpha`, `-beta`, `-rc` excluded) and, for context, with the newest
release of any kind. "Base" is `76ecb97`, kept in `base-libs.versions.toml`.

| Library | Catalog key | Base (76ecb97) | Now | Newest stable on 2026-10-09 (Maven metadata) | Newest of any kind |
|---|---|---|---|---|---|
| androidx.navigation3:navigation3-runtime | `nav3` | 1.1.7 | 1.2.0 | 1.2.0 | 1.3.0-alpha02 |
| androidx.lifecycle:lifecycle-viewmodel-navigation3 | `lifecycleNav3` | 2.10.0 | 2.11.0 | 2.11.0 | 2.12.0-alpha04 |
| androidx.camera:camera-core | `camerax` | 1.6.2 | 1.6.2 | 1.6.2 | 1.7.0-alpha03 |
| org.robolectric:robolectric | `robolectric` | 4.17 | 4.17 | 4.17 | 4.17-beta-4 |
| io.github.takahirom.roborazzi:roborazzi | `roborazzi` | 1.76.0 | 1.76.0 | 1.76.0 | 1.76.0 |
| io.github.sergio-sastre.ComposablePreviewScanner:android | `composablePreviewScanner` | 0.9.3 | 0.9.3 | 0.9.3 | 0.9.3 |
| androidx.compose.material3:material3 (the app's own entry, above the BOM) | `compose-material3-app` | - | 1.5.0-beta01 | - | 1.5.0-beta01 |
| org.jetbrains.kotlin:kotlin-gradle-plugin | `kotlin` | 2.3.20 | 2.4.21 | 2.4.21 | 2.5.0-Beta1 |
| com.android.tools.build:gradle (AGP 9.x) | `agp` | 8.13.0 | 9.4.1 | 9.4.1 | 9.5.0-alpha09 |
| androidx.compose:compose-bom | `compose-bom` | 2026.05.00 | 2026.09.00 | 2026.09.00 | 2026.09.00 |

Notes:

- **Material3 is the one library deliberately above the stable BOM.** The stable Compose BOM 2026.09.00 maps Material3 to
  1.4.0 (the newest stable Material3 in the metadata is 1.4.0, `curl ... | grep '<version>1\.4\.'`); there is no stable 1.5.x. The
  app's own catalog entry `compose-material3-app` asks for **1.5.0-beta01**, which is the newest Material3 release of any kind
  (no newer 1.5.x exists: DoD 12). The engine modules keep `compose-material3` 1.3.2.
- **Gradle:** the wrapper is 9.6.0 (the floor that AGP 9.4 names; `distributionSha256Sum` is set). The newest stable Gradle on
  `services.gradle.org/versions/current` is 9.8.1; the DoD asks for "9.6.0 or newer", and nothing needed more, so it stays.
- **Not in the DoD's list, left where they were (Q2 in the DoD):** `activity-compose` 1.10.1, `lifecycle-runtime-ktx` 2.9.1,
  `core-ktx` 1.16.0, `datastore` 1.1.7, Coil 2.7.0.
- Every other "newest of any kind" above is a pre-release (alpha, beta); none is stable, so none is a failure.
