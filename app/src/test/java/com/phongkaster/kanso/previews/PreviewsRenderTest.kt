package com.phongkaster.kanso.previews

import android.app.Application
import android.content.res.Configuration
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

/**
 * Draws every preview in the app on the computer, in light and in dark, and fails if any of
 * them throws (DoD 2), with the app's wiring (Koin) not running (DoD 4).
 *
 * ComposablePreviewScanner is a test-only library that reads the app's compiled code and returns
 * one entry for every `@Preview` it finds, private ones included. A `@PreviewLightDark` function
 * comes back twice, once marked as night mode and once not, so both pictures are drawn.
 *
 * Each preview is drawn in a fresh, empty activity inside Robolectric (Android running on the
 * plain Java machine). The test runs with a plain [Application] instead of the app's own one,
 * so Koin is never started: a preview that reached for Koin would throw here, and the test also
 * checks Koin is still stopped before and after every drawing.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = PHONE, application = Application::class)
class PreviewsRenderTest {

    /**
     * Stops Koin before each test. Koin's state lives in the Java machine, not in the test's
     * Application, so an earlier test class in the same run may have left it started.
     */
    @Before
    fun stopLeftoverKoin() {
        stopKoin()
    }

    /** Leaves the day/night setting as other tests expect it. */
    @After
    fun resetNightMode() {
        RuntimeEnvironment.setQualifiers("+notnight")
    }

    /** Every preview the scanner finds draws without an error, in its light or dark variant. */
    @Test
    fun every_preview_draws_without_an_error() {
        val previews = scanPreviews()
        assertTrue("The scanner found no previews at all", previews.isNotEmpty())

        previews.forEach { preview ->
            drawPreview(nameOf(preview), dark = isDark(preview)) { preview() }
        }
    }

    /** The previews the Definition of Done names exist, each with a light and a dark variant. */
    @Test
    fun the_required_previews_exist_in_light_and_dark() {
        val previews = scanPreviews()
        requiredPreviewNames.forEach { name ->
            val variants = previews.filter { it.methodName == name }
            assertTrue(
                "No preview named $name was found; found: ${previews.map { it.methodName }.toSet()}",
                variants.isNotEmpty(),
            )
            assertTrue("$name has no light variant", variants.any { !isDark(it) })
            assertTrue("$name has no dark variant", variants.any { isDark(it) })
        }
    }

    /** Every source file that holds a composable has at least one preview the scanner found. */
    @Test
    fun every_file_with_a_composable_has_a_preview() {
        val root = findAppSourceRoot()
        val previews = scanPreviews()
        val filesWithComposables = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.readText().contains("@Composable") }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .toList()
        assertTrue("The scan found no file holding a composable", filesWithComposables.isNotEmpty())

        val missing = filesWithComposables.filter { path ->
            val fileClass = fileClassOf(path)
            previews.none { belongsTo(it.declaringClass, fileClass) }
        }
        assertEquals(
            "These files hold a composable but no preview the scanner can find",
            emptyList<String>(),
            missing,
        )
    }

    /** The drawing helper reports a preview that throws, naming it in the failure. */
    @Test
    fun a_preview_that_throws_is_reported() {
        val failure = assertThrows(AssertionError::class.java) {
            drawPreview("ThrowingSelfCheckPreview", dark = false) { ThrowingComposable() }
        }
        assertTrue(
            "The failure should name the preview, was: ${failure.message}",
            failure.message.orEmpty().contains("ThrowingSelfCheckPreview"),
        )
        assertTrue(
            "The failure should carry the original error",
            generateSequence(failure.cause) { it.cause }.any { it.message == THROWING_MESSAGE },
        )
    }

    /** The drawing helper accepts a composable that draws without trouble. */
    @Test
    fun a_preview_that_draws_is_accepted() {
        drawPreview("QuietSelfCheckPreview", dark = true) { QuietComposable() }
    }

    /**
     * Draws [content] in a fresh empty activity, with the phone in night mode when [dark] is
     * true, waits for it to settle, and turns any error into a failure naming [name]. Koin must
     * be stopped before and after.
     */
    private fun drawPreview(name: String, dark: Boolean, content: @Composable () -> Unit) {
        assertNull("Koin is running before drawing $name", GlobalContext.getOrNull())

        // Robolectric's resource qualifiers play the part of the phone's dark-theme setting; the
        // "+" keeps the other qualifiers (the phone size) as they are.
        RuntimeEnvironment.setQualifiers(if (dark) "+night" else "+notnight")
        val controller = Robolectric.buildActivity(ComponentActivity::class.java)
        // A preview that keeps invalidating its own composition never lets the test clock reach
        // the end of the half second, and nothing else would stop it. The watchdog names the
        // preview and ends the test process, so a hang fails fast instead of running for hours.
        val watchdog = startWatchdog("$name (${if (dark) "dark" else "light"})")
        try {
            val activity = controller.setup().get()
            activity.setContent { content() }
            // Runs the main thread's queued work (composition, layout and drawing frames) for
            // half a second of the test's clock.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        } catch (error: Throwable) {
            throw AssertionError("Preview $name (${if (dark) "dark" else "light"}) failed to draw: $error", error)
        } finally {
            watchdog.interrupt()
            runCatching { controller.pause().stop().destroy() }
        }

        assertNull("Koin is running after drawing $name", GlobalContext.getOrNull())
    }

    /**
     * Starts a background timer that, if it is not interrupted within [WATCHDOG_SECONDS], prints
     * which preview is stuck and halts the test process. The returned thread is interrupted when
     * the drawing finishes in time.
     */
    private fun startWatchdog(label: String): Thread =
        Thread {
            try {
                Thread.sleep(WATCHDOG_SECONDS * 1000)
                System.err.println("PREVIEW HANG: $label did not finish drawing in $WATCHDOG_SECONDS s")
                System.err.flush()
                Runtime.getRuntime().halt(1)
            } catch (_: InterruptedException) {
                // Drawn in time: nothing to report.
            }
        }.apply {
            isDaemon = true
            start()
        }

    /** Scans the app's compiled code for every preview, private ones included. */
    private fun scanPreviews(): List<ComposablePreview<AndroidPreviewInfo>> =
        AndroidComposablePreviewScanner()
            .scanPackageTrees("com.phongkaster.kanso")
            .includePrivatePreviews()
            .getPreviews()

    /** Tells whether [preview] is the night-mode variant (its uiMode asks for night). */
    private fun isDark(preview: ComposablePreview<AndroidPreviewInfo>): Boolean =
        (preview.previewInfo.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    /** A readable name for [preview]: its file class and function, e.g. `HomeScreenKt.HomePreview`. */
    private fun nameOf(preview: ComposablePreview<AndroidPreviewInfo>): String =
        "${preview.declaringClass.substringAfterLast('.')}.${preview.methodName}"

    /**
     * Turns a source path such as `com/x/HomeScreen.kt` into the class Kotlin compiles its
     * top-level functions into: `com.x.HomeScreenKt`.
     */
    private fun fileClassOf(path: String): String =
        path.removeSuffix(".kt").replace('/', '.') + "Kt"

    /**
     * Tells whether [declaringClass], as the scanner reports it (with or without its package),
     * is [fileClass].
     */
    private fun belongsTo(declaringClass: String, fileClass: String): Boolean =
        declaringClass == fileClass || fileClass.endsWith(".$declaringClass")

    /**
     * Finds `app/src/main/java`, whatever directory the test runs from (Gradle runs unit tests
     * from the module directory; an IDE may use the repository root).
     */
    private fun findAppSourceRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidates = listOf(File(dir, "src/main/java"), File(dir, "app/src/main/java"))
            for (candidate in candidates) {
                if (File(candidate, "com/phongkaster/kanso").isDirectory) return candidate
            }
            dir = dir.parentFile
        }
        fail("Could not find app/src/main/java from ${System.getProperty("user.dir")}")
        error("unreachable")
    }

    /** The preview names this test expects. */
    private companion object {
        /** Previews that must exist by name, each in light and dark (DoD 3). */
        val requiredPreviewNames = setOf(
            "GalleryPhotosPreview",
            "GalleryPartialAccessPreview",
            "GalleryRefusedPreview",
            "GalleryEmptyPreview",
            "CameraLivePreview",
            "CameraAfterShotPreview",
            "CameraRefusedPreview",
            "ToolPhotoPreview",
            "ToolEnhancePreview",
            "ToolUnreadablePreview",
            "ResultPreview",
            "SettingsMenuPreview",
            "SettingsMenuCustomPreview",
            "SettingsMenuRowPreview",
            "SettingsDetailScreenPreview",
            "ThemeModeContentPreview",
            "ThemeColourContentPreview",
            "ThemeColourChoicePreview",
            "CustomColourPickerPreview",
            "TextSizeContentPreview",
            "SettingsGroupPreview",
            "ColourRowPreview",
            "ResultMarkPreview",
            "ResultSavedPreview",
            "ResultNothingPreview",
            "HomePreview",
            "AccessRefusedPanelPreview",
            "KansoNavHostPreview",
            "KansoThemePreview",
            "KansoTabBarPreview",
            "KansoTabShellPreview",
            "MyEditsPreview",
            "MyEditsEmptyPreview",
            "MyEditsLoadingPreview",
            "ToolNarrowPhotoPreview",
            "PhotoViewerPreview",
            "HomeEmptyPreview",
            "HomeLoadingPreview",
        )
    }
}

/** How long one preview may take to draw before the watchdog ends the test process. */
private const val WATCHDOG_SECONDS = 60L

/** The message [ThrowingComposable] fails with, so the self-check can find it again. */
private const val THROWING_MESSAGE = "This composable throws on purpose"

/** A test-only composable that always throws while drawing, for the self-check. */
@Composable
private fun ThrowingComposable() {
    throw IllegalStateException(THROWING_MESSAGE)
}

/** A test-only composable that draws nothing and never fails, for the self-check. */
@Composable
private fun QuietComposable() {
    Box {}
}

/**
 * The Robolectric screen every preview is drawn on: 411 dp wide, 891 dp tall, at xxhdpi, the
 * same typical phone the screenshot tests use.
 */
private const val PHONE = "w411dp-h891dp-xxhdpi"
