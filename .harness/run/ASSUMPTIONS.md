# ASSUMPTIONS

> Autonomous mode: every decision the engine made alone. To overturn one, answer its id in `.harness/run/DECISIONS.md` and re-run.

---

## A-001 - Cut Firebase out of `core/common` by making its analytics helper log-only

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** f141583 (T-001)
- **Revert:** `git revert f141583`

### Question
The PRD says "no Firebase" and "reuse the engine as it is; adapt build scripts only as far as the build requires", but `core/common` imports Firebase analytics in one source file (`AnalyticsUtil.kt`), so removing the dependency needs a source change.

### Options Considered
1. Keep Firebase analytics in `core/common` - consequences: breaks "no Firebase" and adds a network permission.
2. Make `AnalyticsUtil.kt` log-only with the same function signatures - consequences: one small source edit in the engine; prebuilt AARs that inline the old Firebase calls would fail only when such a call runs, which nothing in this run does.
3. Delete the file - consequences: may break binary compatibility with the AARs.

### Taken, and why
Option 2: it honours the PRD's constraint and keeps the engine's public signatures.

---

## A-002 - Default theme colour on a fresh install

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** def99b1 (T-004)
- **Revert:** `git revert def99b1`

### Question
The PRD offers "wallpaper or a colour I pick" but not which one is on before the person chooses.

### Options Considered
1. Wallpaper on Android 12+, Material 3 baseline purple `#6750A4` below - consequences: nicest on new phones; default-colour screenshots must run at SDK 30.
2. Always the baseline purple - consequences: simpler, but ignores the wallpaper by default.

### Taken, and why
Option 1: it is the Android convention for Material You apps, and the PRD lists wallpaper first.

---

## A-003 - ML Kit's transitive Firebase infrastructure and Play-services model loading are allowed

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** f141583 (T-001)
- **Revert:** `git revert f141583`

### Question
`core/localml` needs ML Kit, which pulls in `firebase-components` and `firebase-encoders` and may merge INTERNET and ACCESS_NETWORK_STATE; some of its models load through Play services. That conflicts with a literal reading of "no Firebase, no network".

### Options Considered
1. Forbid every Firebase coordinate - consequences: the DoD cannot pass without breaking the engine.
2. Allow those infrastructure libraries, remove the network permissions with `tools:node="remove"`, and list the exception in `PROJECT.md` - consequences: ML models that need a Play-services download will not work until a later run decides how to ship them offline.

### Taken, and why
Option 2: the PRD says to remove a merged network permission unless that breaks the engine, and nothing in this run uses ML.

---

## A-004 - Add `com.materialkolor:material-kolor` for seed-colour schemes

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** f141583 (T-001; used by def99b1, T-004)
- **Revert:** `git revert def99b1 f141583`

### Question
Generating a full Material 3 scheme from one colour needs a generator. Compose Material 3 does not ship one, and Material's own `color.utilities` classes are `@RestrictTo`, which fails lint.

### Options Considered
1. `material-kolor` from Maven Central - consequences: one new build-time dependency; no network at run time.
2. Hand-written tonal palette code - consequences: more code to teach and test, less faithful to Material 3.
3. The `@RestrictTo` classes with a lint suppression - consequences: fragile.

### Taken, and why
Option 1; fall back to option 2 as a Tier 1 amendment if it will not resolve with the Kotlin version in use.

---

## A-005 - The emulator's covering window is the environment's, not the app's

- **Tier:** 2 (missing information)
- **Iteration:** 10
- **First dependent checkpoint:** Iteration 10 (T-006 closed)
- **Revert:** `git revert` the Iteration 10 checkpoint, which reopens T-006

### Question
`-Op dump` is refused because the focused window is `Application` on `emulator-5554`. Is that the app?

### Options Considered
1. Treat it as an app start-up defect and spend attempt 3 on it - consequences: the window persists with the app force-stopped, so no app change can affect it.
2. Treat it as emulator state; leave DoD 2 (emulator half), 19, 22 undriven and unsigned for a person - consequences: three criteria rely on a person.

### Taken, and why
Option 2: evidence is that focus stays `Application` after force-stop.
