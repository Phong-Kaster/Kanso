package com.phongkaster.kanso.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * Checks that the four theme colour labels live in the app's string resources (`strings.xml`,
 * and `strings_theme.xml` for "Kanso blue") with the exact wording the screen shows, and that no
 * Kotlin source under `app/src/main/java` carries them as hard-coded text.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "notnight")
class ThemeColourLabelsTest {

    /** Stops Koin, which the app's Application class starts, so it does not leak into other tests. */
    @After
    fun tearDown() {
        stopKoin()
    }

    /** The four string resources read exactly as the on-screen labels should. */
    @Test
    fun resourceStringsHaveTheExpectedWording() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Theme colour", context.getString(R.string.theme_colour_title))
        assertEquals("From wallpaper", context.getString(R.string.theme_colour_wallpaper))
        assertEquals("Kanso blue", context.getString(R.string.theme_colour_kanso_blue))
        assertEquals("Custom colour", context.getString(R.string.theme_colour_custom))
    }

    /** No Kotlin source in the app holds one of the four labels as a quoted string in code. */
    @Test
    fun noSourceFileHardCodesTheLabels() {
        val literals = listOf(
            "\"Theme colour\"",
            "\"From wallpaper\"",
            "\"Kanso blue\"",
            "\"Custom colour\"",
        )
        val files = findAppSourceRoot().walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        assertTrue("No Kotlin sources found", files.isNotEmpty())

        // Comment lines may quote a label when describing the screen; only code counts.
        val offenders = files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val trimmed = line.trim()
                val isComment = trimmed.startsWith("*") || trimmed.startsWith("//") ||
                    trimmed.startsWith("/*")
                if (!isComment && literals.any { line.contains(it) }) {
                    "${file.name}:${index + 1}"
                } else {
                    null
                }
            }
        }
        assertTrue("Hard-coded theme colour labels found: $offenders", offenders.isEmpty())
    }

    /** Finds app/src/main/java by walking up from the test's working directory. */
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
}
