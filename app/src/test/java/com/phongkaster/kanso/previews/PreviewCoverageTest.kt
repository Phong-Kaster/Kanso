package com.phongkaster.kanso.previews

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Reads the app's source code and checks that every composable has a preview (DoD 1).
 *
 * A composable counts as covered when it is one of these:
 *
 * - a preview itself (a function annotated `@Preview…`);
 * - drawn by a preview of the same file, directly or through another composable of that file
 *   that a preview draws (a preview draws `HomeContent`, which draws `HomeRow`);
 * - a thin stateful wrapper: it calls a covered stateless part, named `…Content` or `…Display`
 *   (`HomeScreen` gathers state and calls `HomeContent`), or another wrapper.
 *
 * Every preview must also come in light and dark: `@PreviewLightDark`, or two `@Preview`
 * annotations one of which asks for `UI_MODE_NIGHT_YES`.
 *
 * Three composables cannot be drawn on the computer and are named in [EXEMPT]; each fills a slot
 * of a previewed `…Content`, and the previews put a stand-in in the same slot.
 *
 * This is a plain text scan (no Robolectric, no compiling), in the style of `KdocCoverageTest`.
 */
class PreviewCoverageTest {

    /** The app's own code passes the coverage rule. */
    @Test
    fun every_composable_in_the_app_has_a_preview() {
        val root = findAppSourceRoot()
        val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("No Kotlin sources found under $root", files.isNotEmpty())

        val problems = files.sortedBy { it.path }.flatMap { file ->
            findProblems(file.relativeTo(root).invariantSeparatorsPath, file.readText())
        }
        assertTrue(
            "These composables or previews break the coverage rule:\n" + problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }

    /** The checker reports a composable that no preview draws. */
    @Test
    fun the_checker_reports_a_composable_with_no_preview() {
        val sample = """
            package sample

            /** Home with sample tools. */
            @PreviewLightDark
            @Composable
            private fun HomePreview() {
                HomeContent()
            }

            /** The stateless home. */
            @Composable
            fun HomeContent() {
                HomeRow()
            }

            /** A row of the home. */
            @Composable
            private fun HomeRow() {}

            /** Nothing draws this one. */
            @Composable
            fun Forgotten() {}
        """.trimIndent()

        assertEquals(
            listOf("Sample.kt:22 composable Forgotten has no preview"),
            findProblems("Sample.kt", sample),
        )
    }

    /** The checker accepts a stateful wrapper whose `…Content` part a preview draws. */
    @Test
    fun the_checker_accepts_a_wrapper_around_a_previewed_content() {
        val sample = """
            package sample

            /** The stateful screen. */
            @Composable
            fun HomeScreen(viewModel: HomeViewModel) {
                HomeBody(viewModel)
            }

            /** Reads the state and hands it down. */
            @Composable
            private fun HomeBody(viewModel: HomeViewModel) {
                HomeContent(viewModel.tools)
            }

            /** The stateless part. */
            @Composable
            fun HomeContent(tools: List<String>) {}

            /** Shows the stateless part. */
            @PreviewLightDark
            @Composable
            private fun HomePreview() {
                HomeContent(emptyList())
            }
        """.trimIndent()

        assertEquals(emptyList<String>(), findProblems("Sample.kt", sample))
    }

    /** A comment that mentions a composable does not count as drawing it. */
    @Test
    fun the_checker_ignores_names_in_comments() {
        val sample = """
            package sample

            /** Shows the screen. */
            @PreviewLightDark
            @Composable
            private fun ScreenPreview() {
                // Forgotten is not drawn here.
            }

            /** Nothing draws this one. */
            @Composable
            fun Forgotten() {}
        """.trimIndent()

        assertEquals(
            listOf("Sample.kt:12 composable Forgotten has no preview"),
            findProblems("Sample.kt", sample),
        )
    }

    /** A slash and a star inside a string literal do not hide the composables that follow it. */
    @Test
    fun the_checker_is_not_fooled_by_a_slash_star_in_a_string() {
        val sample = """
            package sample

            /** Picks an image. */
            private const val MIME = "image/*"

            /** Nothing draws this one. */
            @Composable
            fun Forgotten() {}
        """.trimIndent()

        assertEquals(
            listOf("Sample.kt:8 composable Forgotten has no preview"),
            findProblems("Sample.kt", sample),
        )
    }

    /** The checker reports a preview that comes in light only, and accepts the two valid forms. */
    @Test
    fun the_checker_reports_a_preview_without_a_dark_variant() {
        val sample = """
            package sample

            /** Light only. */
            @Preview
            @Composable
            private fun LightOnlyPreview() {}

            /** A pair of previews. */
            @Preview
            @Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
            @Composable
            private fun PairPreview() {}

            /** The shortcut. */
            @PreviewLightDark
            @Composable
            private fun ShortcutPreview() {}
        """.trimIndent()

        assertEquals(
            listOf("Sample.kt:6 preview LightOnlyPreview is not in light and dark"),
            findProblems("Sample.kt", sample),
        )
    }

    /** A composable named in [EXEMPT] is accepted without a preview. */
    @Test
    fun the_checker_accepts_a_named_exemption() {
        val sample = """
            package sample

            /** Shows the camera. */
            @Composable
            private fun LivePreview() {}
        """.trimIndent()

        assertEquals(emptyList<String>(), findProblems("com/phongkaster/kanso/ui/camera/CameraScreen.kt", sample))
    }

    /**
     * Returns one line per broken rule in [source], as `fileName:line message`, line numbers
     * counted from 1.
     */
    private fun findProblems(fileName: String, source: String): List<String> {
        val lines = blankComments(source).lines()
        val functions = findFunctions(lines)
        val composables = functions.filter { it.isComposable }.associateBy { it.name }
        val previews = functions.filter { it.isPreview }
        val problems = mutableListOf<String>()

        previews.forEach { preview ->
            if (!isLightAndDark(preview.annotations)) {
                problems += "$fileName:${preview.line + 1} preview ${preview.name} is not in light and dark"
            }
        }

        // Everything a preview draws, directly or through another composable of the file.
        val drawn = mutableSetOf<String>()
        val queue = ArrayDeque(previews.map { it.name })
        while (queue.isNotEmpty()) {
            val name = queue.removeFirst()
            val current = functions.firstOrNull { it.name == name } ?: continue
            composables.keys.filter { it != current.name && it !in drawn && current.mentions(it) }
                .forEach { drawn += it; queue += it }
        }

        // Thin wrappers: they call a drawn stateless part, or another wrapper. Repeat until
        // nothing new is accepted, so a wrapper of a wrapper is found too.
        val covered = (drawn + previews.map { it.name }).toMutableSet()
        val wrappers = mutableSetOf<String>()
        do {
            val before = covered.size
            composables.values.filter { it.name !in covered }.forEach { candidate ->
                val callsCoveredPart = composables.keys.any { other ->
                    other != candidate.name && candidate.mentions(other) &&
                        ((other in drawn && STATELESS_PART.containsMatchIn(other)) || other in wrappers)
                }
                // Thin means everything else it draws is already covered or named in EXEMPT.
                val drawsOnlyCovered = composables.keys.all { other ->
                    other == candidate.name || !candidate.mentions(other) || other in covered ||
                        "$fileName:$other" in EXEMPT
                }
                if (callsCoveredPart && drawsOnlyCovered) {
                    covered += candidate.name
                    wrappers += candidate.name
                }
            }
        } while (covered.size != before)

        composables.values.filter { it.name !in covered }.forEach { composable ->
            if ("$fileName:${composable.name}" !in EXEMPT) {
                problems += "$fileName:${composable.line + 1} composable ${composable.name} has no preview"
            }
        }
        return problems
    }

    /** Tells whether [annotations] ask for a light and a dark picture. */
    private fun isLightAndDark(annotations: String): Boolean {
        if (annotations.contains("@PreviewLightDark")) return true
        val plain = Regex("""@Preview\b(?!\w)""").findAll(annotations).count()
        return plain >= 2 && annotations.contains("UI_MODE_NIGHT_YES")
    }

    /**
     * Finds every `fun` declaration at any depth, with the annotations just above it and its
     * body. Its body runs to the line before the next non-blank line indented no deeper that is
     * not a closing bracket.
     */
    private fun findFunctions(lines: List<String>): List<Function> {
        val found = mutableListOf<Function>()
        lines.forEachIndexed { index, line ->
            val name = FUN_NAME.find(line)?.groupValues?.get(1) ?: return@forEachIndexed
            val indent = line.takeWhile { it == ' ' || it == '\t' }.length
            var next = index + 1
            while (next < lines.size) {
                val candidate = lines[next]
                val candidateIndent = candidate.takeWhile { it == ' ' || it == '\t' }.length
                if (candidate.isNotBlank() && candidateIndent <= indent && candidate.trim()[0] !in CLOSERS) break
                next++
            }
            // The annotations are the unbroken run of lines right above (a blank line, which
            // is where a comment used to be, ends it).
            var top = index
            while (top > 0 && lines[top - 1].isNotBlank() && lines[top - 1].trim() != "}") top--
            found += Function(
                name = name,
                line = index,
                annotations = lines.slice(top until index).joinToString("\n"),
                body = lines.slice(index until next).joinToString("\n"),
            )
        }
        return found
    }

    /**
     * Returns [source] with every comment replaced by spaces (line breaks kept), so a name in a
     * comment is not counted as a call and line numbers stay the same.
     */
    private fun blankComments(source: String): String =
        source.replace(COMMENT_OR_STRING) { match ->
            // String literals are kept as they are: a slash-star inside one does not start a comment.
            if (match.value.startsWith("\"")) {
                match.value
            } else {
                match.value.map { if (it == '\n') '\n' else ' ' }.joinToString("")
            }
        }

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

    /**
     * One function found by the scan: its [name], the 0-based [line] it is declared on, the
     * [annotations] above it and its [body] (comments blanked).
     */
    private data class Function(val name: String, val line: Int, val annotations: String, val body: String) {
        /** True when the function is annotated `@Composable`. */
        val isComposable: Boolean get() = annotations.contains("@Composable")

        /** True when the function is annotated with a preview annotation. */
        val isPreview: Boolean get() = Regex("""@Preview\w*""").containsMatchIn(annotations)

        /** True when this function's body, after its own signature line, mentions [other]. */
        fun mentions(other: String): Boolean =
            Regex("""\b${Regex.escape(other)}\b""").containsMatchIn(body.substringAfter('\n', ""))
    }

    /** Patterns and the named exemptions the checker uses. */
    private companion object {
        /** A function declaration, capturing its name. */
        val FUN_NAME = Regex("""(?:^|\s)fun\s+(?:<[^>]*>\s*)?(?:[\w<>?, ]+?\.)?(\w+)\s*\(""")

        /** A string literal (plain or raw), a block comment or a line comment, whichever starts first. */
        val COMMENT_OR_STRING = Regex(
            "\"\"\"[\\s\\S]*?\"\"\"" + // raw string
                "|\"(?:\\\\.|[^\"\\\\\\n])*\"" + // plain string
                "|/\\*[\\s\\S]*?\\*/" + // block comment
                "|//[^\\n]*", // line comment
        )

        /** The names of a stateless part a stateful wrapper hands its state to. */
        val STATELESS_PART = Regex("""(?:Content|Display)$""")

        /** Characters that close a bracket and so belong to the declaration above them. */
        val CLOSERS = setOf('}', ')', ']')

        /**
         * Composables allowed to have no preview, as `file:name`. Each fills a slot of a previewed
         * `…Content` and needs something a preview must not touch (DoD 4): `LivePreview` the
         * camera surface, `ShotView` and `PhotoThumbnail` a Coil load of a real photo. The
         * previews draw a stand-in in the same slot.
         */
        val EXEMPT = setOf(
            "com/phongkaster/kanso/ui/camera/CameraScreen.kt:LivePreview",
            "com/phongkaster/kanso/ui/camera/CameraScreen.kt:ShotView",
            "com/phongkaster/kanso/ui/gallery/GalleryScreen.kt:PhotoThumbnail",
        )
    }
}
