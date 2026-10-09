# DoD 14: no library was added, versions only moved

Run on 2026-10-09 against the working tree of the Phase 3 checkpoint (the Verifier repeats it at HEAD). `<base>` is `76ecb97`.

## 1. The set of `group:artifact` coordinates in `[libraries]`

`head-catalog-coordinates.txt` is made by the DoD's awk over `gradle/libs.versions.toml`; `base-catalog-coordinates.txt` (68 lines) is the same at `<base>`.

```
diff <(sort -u .harness/run/evidence/base-catalog-coordinates.txt) .harness/run/evidence/head-catalog-coordinates.txt
```

Output: **nothing** (68 coordinates both sides). The catalog gained one alias, `compose-material3-app`, for the **same** coordinate
`androidx.compose.material3:material3` that `compose-material3` already names, with its own version `compose-material3-app = "1.5.0-beta01"`;
that is the alias the DoD allows ("a second Material3 entry for the app").

## 2. `[plugins]` ids

```
diff <(sort -u .harness/run/evidence/base-catalog-plugins.txt) .harness/run/evidence/head-catalog-plugins.txt
```

Output: one line, `< id = "org.jetbrains.kotlin.android"` (removed in Phase 1: AGP 9's built-in Kotlin). Nothing was added.

## 3. Coordinates added to build scripts

```
git diff -U0 76ecb97 -- app/build.gradle.kts 'core/*/build.gradle.kts' opencv/build.gradle build.gradle.kts | grep -E '^\+' | grep -E "[\"'][a-zA-Z0-9.-]+:[a-zA-Z0-9.-]+(:[^\"']*)?[\"']"
```

Output: **nothing** (exit 1). No build script names a `group:artifact` string; the one new dependency line is
`implementation(libs.compose.material3.app)`, a catalog alias.
