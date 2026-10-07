package com.phongkaster.kanso.previews

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Reads the app's source code and checks that previews stay clean (DoD 4 and 5):
 *
 * - The body of every preview function, and every top-level `sample…` / `Sample…` declaration,
 *   never mentions Koin, a ViewModel, MediaStore, the photo loader, the camera or a permission
 *   launcher: a preview must be drawable with none of the app's machinery running.
 * - Every top-level `sample…` / `Sample…` declaration is `private`, and is used only by preview
 *   functions or by other `sample…` declarations of the same file, so made-up sample data can
 *   never reach the code the app runs.
 *
 * This is a plain text scan (no Robolectric, no compiling), in the style of `KdocCoverageTest`.
 * It is deliberately simple: a declaration's extent is everything from its first line up to the
 * next line that is indented no deeper and is not a closing bracket.
 */
class PreviewHygieneTest {

    /** The app's own code passes every hygiene rule. */
    @Test
    fun previews_and_sample_data_in_the_app_are_clean() {
        val root = findAppSourceRoot()
        val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("No Kotlin sources found under $root", files.isNotEmpty())

        val previewCount = files.sumOf { findPreviewFunctions(it.readText().lines()).size }
        assertTrue("The scan found no preview function at all under $root", previewCount > 0)

        val problems = files.sortedBy { it.path }.flatMap { file ->
            findProblems(file.relativeTo(root).invariantSeparatorsPath, file.readText())
        }
        assertTrue(
            "These previews or sample declarations break the preview rules:\n" +
                problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }

    /** The checker reports a preview whose body mentions something a preview must not touch. */
    @Test
    fun the_checker_reports_a_forbidden_word_in_a_preview() {
        val sample = """
            package sample

            /** A preview that wrongly reaches for the app's wiring. */
            @PreviewLightDark
            @Composable
            private fun BadPreview() {
                val model: HomeViewModel = koinViewModel()
                HomeContent(model.tools)
            }
        """.trimIndent()

        assertEquals(
            listOf(
                "Sample.kt:6 preview BadPreview mentions koin",
                "Sample.kt:6 preview BadPreview mentions ViewModel",
            ),
            findProblems("Sample.kt", sample),
        )
    }

    /** The checker reports a sample declaration that is not private. */
    @Test
    fun the_checker_reports_a_sample_that_is_not_private() {
        val sample = """
            package sample

            /** Tools to show in the preview. */
            val sampleTools = listOf("Edit", "Enhance")

            /** Home with sample tools. */
            @Preview
            @Composable
            private fun HomePreview() {
                HomeContent(sampleTools)
            }
        """.trimIndent()

        assertEquals(
            listOf("Sample.kt:4 sample sampleTools is not private"),
            findProblems("Sample.kt", sample),
        )
    }

    /** The checker reports a sample declaration that code other than previews uses. */
    @Test
    fun the_checker_reports_a_sample_used_outside_previews() {
        val sample = """
            package sample

            /** Tools to show in the preview. */
            private val sampleTools = listOf("Edit", "Enhance")

            /** The real screen, which must never show sample data. */
            @Composable
            fun HomeScreen() {
                HomeContent(sampleTools)
            }

            /** Home with sample tools. */
            @PreviewLightDark
            @Composable
            private fun HomePreview() {
                HomeContent(sampleTools)
            }
        """.trimIndent()

        assertEquals(
            listOf("Sample.kt:4 sample sampleTools is used outside previews and samples at line 9"),
            findProblems("Sample.kt", sample),
        )
    }

    /** The checker reports a sample declaration that itself mentions a forbidden word. */
    @Test
    fun the_checker_reports_a_forbidden_word_in_a_sample() {
        val sample = """
            package sample

            /** A sample that wrongly builds a real loader. */
            private fun samplePhotos() = PhotoLoader(context).load()

            /** A preview using it. */
            @PreviewLightDark
            @Composable
            private fun PhotosPreview() {
                Photos(samplePhotos())
            }
        """.trimIndent()

        assertEquals(
            listOf("Sample.kt:4 sample samplePhotos mentions PhotoLoader"),
            findProblems("Sample.kt", sample),
        )
    }

    /**
     * Well-behaved previews and samples produce no report: a private sample used by a preview
     * and by another sample, and a preview whose KDoc (outside its body) names a ViewModel.
     */
    @Test
    fun the_checker_accepts_clean_previews_and_samples() {
        val sample = """
            package sample

            /** The stateful screen; its preview avoids the ViewModel by showing HomeContent. */
            @Composable
            fun HomeScreen(viewModel: HomeViewModel) {
                HomeContent(viewModel.tools)
            }

            /** Album names that look real. */
            private val sampleAlbums = listOf("Camera", "Screenshots")

            /** A sample gallery built from the sample albums. */
            private val sampleGallery = Gallery(albums = sampleAlbums)

            /** Shows the gallery; unlike HomeScreen it needs no ViewModel. */
            @PreviewLightDark
            @Composable
            private fun GalleryPreview() {
                // sampleGallery is made above, from sampleAlbums.
                GalleryContent(sampleGallery)
            }

            /** A sample stand-in class, used only by the preview below. */
            private class SampleLoader {
                /** Returns nothing at all. */
                fun load() = Unit
            }

            /** Draws with the stand-in. */
            @Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
            @Composable
            private fun LoaderPreview() {
                Loaded(SampleLoader())
            }
        """.trimIndent()

        assertEquals(emptyList<String>(), findProblems("Sample.kt", sample))
    }

    /**
     * Returns one line per broken rule in [source], as `fileName:line message`, line numbers
     * counted from 1.
     */
    private fun findProblems(fileName: String, source: String): List<String> {
        val lines = source.lines()
        // The same lines with comments blanked out, so a name mentioned in a comment is not
        // counted as a use. Blanking keeps every line where it was.
        val codeLines = blankComments(source).lines()
        val previews = findPreviewFunctions(lines)
        val samples = findSampleDeclarations(lines)
            .filterNot { "$fileName:${it.name}" in NOT_PREVIEW_SAMPLES }
        val allowedRanges = previews.map { it.range } + samples.map { it.range }
        val problems = mutableListOf<String>()

        previews.forEach { preview ->
            forbiddenWordsIn(preview.scannedText).forEach { word ->
                problems += "$fileName:${preview.line + 1} preview ${preview.name} mentions $word"
            }
        }

        samples.forEach { sample ->
            val where = "$fileName:${sample.line + 1} sample ${sample.name}"
            if (!sample.isPrivate) problems += "$where is not private"
            forbiddenWordsIn(sample.scannedText).forEach { word -> problems += "$where mentions $word" }

            val use = Regex("""\b${Regex.escape(sample.name)}\b""")
            codeLines.forEachIndexed { index, line ->
                if (use.containsMatchIn(line) && allowedRanges.none { index in it }) {
                    problems += "$where is used outside previews and samples at line ${index + 1}"
                }
            }
        }
        return problems
    }

    /** Returns the forbidden words [text] mentions, `koin` in any case, the rest exactly. */
    private fun forbiddenWordsIn(text: String): List<String> = buildList {
        if (text.contains("koin", ignoreCase = true)) add("koin")
        addAll(FORBIDDEN_EXACT.filter { text.contains(it) })
    }

    /**
     * Finds every function annotated with a preview annotation (`@Preview`, `@PreviewLightDark`
     * and the other `@Preview…` ones). Its scanned text is the body only: everything after the
     * parameter list, so the KDoc and the signature are not part of it.
     */
    private fun findPreviewFunctions(lines: List<String>): List<Declaration> {
        val found = mutableListOf<Declaration>()
        var index = 0
        while (index < lines.size) {
            if (!PREVIEW_ANNOTATION.containsMatchIn(lines[index].trim())) {
                index++
                continue
            }
            // Walk over the remaining annotation lines (and the lines inside an annotation's
            // brackets, such as a multi-line @Preview(...)) to the declaration they belong to.
            var declarationLine = index
            var bracketDepth = 0
            while (declarationLine < lines.size) {
                val trimmed = lines[declarationLine].trim()
                val isSkippable = trimmed.isEmpty() || trimmed.startsWith("@") ||
                    trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")
                if (bracketDepth == 0 && !isSkippable) {
                    // Annotates something other than a function, e.g. an annotation class.
                    if (!FUN_NAME.containsMatchIn(trimmed)) declarationLine = -1
                    break
                }
                // `@Preview @Composable private fun X()` all on one line.
                if (bracketDepth == 0 && trimmed.startsWith("@") &&
                    FUN_NAME.containsMatchIn(trimmed.replace(ANNOTATIONS_PREFIX, ""))
                ) {
                    break
                }
                bracketDepth += trimmed.count { it == '(' } - trimmed.count { it == ')' }
                declarationLine++
            }
            if (declarationLine < 0 || declarationLine >= lines.size) {
                index++
                continue
            }

            val range = declarationLine..extentEnd(lines, declarationLine)
            val text = lines.slice(range).joinToString("\n")
            val name = FUN_NAME.find(text)?.groupValues?.get(1).orEmpty()
            found += Declaration(name, declarationLine, range, bodyOf(text), isPrivate = true)
            index = range.last + 1
        }
        return found
    }

    /** Finds every top-level (unindented) declaration whose name starts with `sample`/`Sample`. */
    private fun findSampleDeclarations(lines: List<String>): List<Declaration> =
        lines.indices.mapNotNull { index ->
            val line = lines[index]
            if (line.isEmpty() || line[0].isWhitespace()) return@mapNotNull null
            val match = SAMPLE_DECLARATION.find(line.replace(ANNOTATIONS_PREFIX, ""))
                ?: return@mapNotNull null
            val modifiers = match.groupValues[1].split(WHITESPACE)
            val range = index..extentEnd(lines, index)
            Declaration(
                name = match.groupValues[2],
                line = index,
                range = range,
                scannedText = lines.slice(range).joinToString("\n"),
                isPrivate = "private" in modifiers,
            )
        }

    /**
     * Returns the last line of the declaration starting on line [start]: the line before the
     * next non-blank line that is indented no deeper and is not a closing bracket.
     */
    private fun extentEnd(lines: List<String>, start: Int): Int {
        val indent = indentOf(lines[start])
        var next = start + 1
        while (next < lines.size) {
            val line = lines[next]
            if (line.isNotBlank() && indentOf(line) <= indent && line.trim()[0] !in CLOSERS) break
            next++
        }
        var end = next - 1
        while (end > start && lines[end].isBlank()) end--
        return end
    }

    /** Counts the spaces and tabs at the start of [line]. */
    private fun indentOf(line: String): Int = line.takeWhile { it == ' ' || it == '\t' }.length

    /** Cuts a function's declaration [text] down to what follows its parameter list. */
    private fun bodyOf(text: String): String {
        val open = text.indexOf('(', startIndex = text.indexOf("fun ").coerceAtLeast(0))
        if (open < 0) return text
        var depth = 0
        for (i in open until text.length) {
            when (text[i]) {
                '(' -> depth++
                ')' -> if (--depth == 0) return text.substring(i + 1)
            }
        }
        return text
    }

    /**
     * Returns [source] with every comment replaced by spaces (line breaks kept), leaving string
     * literals alone, so line numbers stay the same.
     */
    private fun blankComments(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        var inString = false
        var blockDepth = 0
        var inLineComment = false
        while (i < source.length) {
            val c = source[i]
            val next = source.getOrNull(i + 1)
            when {
                inLineComment -> {
                    if (c == '\n') inLineComment = false
                    out.append(if (c == '\n') '\n' else ' ')
                }
                blockDepth > 0 -> {
                    when {
                        c == '*' && next == '/' -> { blockDepth--; out.append("  "); i++ }
                        c == '/' && next == '*' -> { blockDepth++; out.append("  "); i++ }
                        else -> out.append(if (c == '\n') '\n' else ' ')
                    }
                }
                inString -> {
                    out.append(c)
                    if (c == '\\' && next != null) {
                        out.append(next)
                        i++
                    } else if (c == '"') {
                        inString = false
                    }
                }
                c == '"' -> {
                    inString = true
                    out.append(c)
                }
                // A character literal such as '"' or '/': copied whole, so its quote or slash
                // does not start a string or a comment.
                c == '\'' -> {
                    val close = source.indexOf('\'', startIndex = i + (if (next == '\\') 3 else 2))
                    val end = if (close < 0) i else close
                    out.append(source, i, end + 1)
                    i = end
                }
                c == '/' && next == '/' -> { inLineComment = true; out.append("  "); i++ }
                c == '/' && next == '*' -> { blockDepth = 1; out.append("  "); i++ }
                else -> out.append(c)
            }
            i++
        }
        return out.toString()
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
     * One preview function or sample declaration found by the scan: its [name], the 0-based
     * [line] it is declared on, the [range] of lines it covers, the [scannedText] checked for
     * forbidden words, and whether it [isPrivate].
     */
    private data class Declaration(
        val name: String,
        val line: Int,
        val range: IntRange,
        val scannedText: String,
        val isPrivate: Boolean,
    )

    /** Patterns and word lists the checker uses. */
    private companion object {
        /** Words a preview or sample must not mention, matched exactly (case counts). */
        val FORBIDDEN_EXACT = listOf(
            "ViewModel",
            "MediaStore",
            "PhotoLoader",
            "CameraController",
            "CameraX",
            "PreviewView",
            "rememberLauncherForActivityResult",
            "LocalContext.current.contentResolver",
        )

        /**
         * App code whose name happens to start with "sample" but is not preview sample data, as
         * `file:name`. `sampleSizeFor` works out Android's `inSampleSize` (how much to shrink a
         * photo while decoding it); it is real app code, public on purpose, and an existing test
         * imports it, so it cannot be renamed or made private.
         */
        val NOT_PREVIEW_SAMPLES = setOf(
            "com/phongkaster/kanso/data/photo/PhotoLoader.kt:sampleSizeFor",
        )

        /** A line starting with a preview annotation, plain or fully qualified. */
        val PREVIEW_ANNOTATION = Regex("""^@(?:[\w.]+\.)?Preview\w*\b""")

        /** Leading annotations on a line, with argument lists that hold no nested brackets. */
        val ANNOTATIONS_PREFIX = Regex("""^(?:@[\w.:]+(?:\([^()]*\))?\s*)+""")

        /** A function declaration, capturing its name (after any type parameters or receiver). */
        val FUN_NAME = Regex("""(?:^|\s)fun\s+(?:<[^>]*>\s*)?(?:[\w<>?, ]+?\.)?(\w+)\s*\(""")

        /**
         * A top-level declaration named `sample…` or `Sample…`, capturing its modifiers and its
         * name.
         */
        val SAMPLE_DECLARATION = Regex(
            """^((?:(?:private|internal|public|const|inline|data|enum|sealed|value|lateinit|suspend|abstract|open)\s+)*)""" +
                """(?:val|var|fun|class|object|interface)\s+(?:<[^>]*>\s*)?(?:[\w<>?, ]+?\.)?([sS]ample\w*)\b""",
        )

        /** Characters that close a bracket and so belong to the declaration above them. */
        val CLOSERS = setOf('}', ')', ']')

        /** Any run of whitespace. */
        val WHITESPACE = Regex("""\s+""")
    }
}
