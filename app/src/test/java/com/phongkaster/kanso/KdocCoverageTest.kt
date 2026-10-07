package com.phongkaster.kanso

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Checks that every class, object, interface and function in the app's own code has a KDoc
 * comment (DoD 25), and that the checker itself really catches a missing one.
 *
 * A declaration counts as documented when the line right above it (skipping only annotation
 * lines such as `@Composable`) closes a `/** ... */` comment holding at least one sentence.
 * Functions marked `override` are skipped: the overridden declaration documents them.
 */
class KdocCoverageTest {

    /** Every declaration under app/src/main/java has a KDoc comment. */
    @Test
    fun every_declaration_in_the_app_code_has_kdoc() {
        val root = findAppSourceRoot()
        val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("No Kotlin sources found under $root", files.isNotEmpty())

        val missing = files.sortedBy { it.path }.flatMap { file ->
            findMissingKdoc(file.relativeTo(root).invariantSeparatorsPath, file.readText())
        }
        assertTrue(
            "These declarations have no KDoc comment of at least one sentence:\n" +
                missing.joinToString("\n"),
            missing.isEmpty(),
        )
    }

    /** The checker reports undocumented declarations and accepts documented ones. */
    @Test
    fun the_checker_reports_declarations_without_kdoc() {
        val sample = """
            package sample

            /** A class that is documented properly. */
            class Documented {
                fun undocumented() {}

                /** Says hello to the caller. */
                @Suppress("unused")
                private fun documented() {}

                override fun toString(): String = "sample"

                /* A plain comment is not a KDoc comment. */
                fun plainComment() {}

                /** */
                fun emptyKdoc() {}

                companion object {
                    val unused = object : Runnable {
                        /** Runs nothing at all. */
                        override fun run() {}
                    }
                }
            }

            @Deprecated("old")
            data class Bare(val x: Int)

            interface NoDocs
        """.trimIndent()

        val missing = findMissingKdoc("Sample.kt", sample)

        assertEquals(
            listOf(
                "Sample.kt:5 fun undocumented",
                "Sample.kt:14 fun plainComment",
                "Sample.kt:17 fun emptyKdoc",
                "Sample.kt:19 object companion",
                "Sample.kt:28 class Bare",
                "Sample.kt:30 interface NoDocs",
            ),
            missing,
        )
    }

    /** A fully documented sample produces no report at all. */
    @Test
    fun the_checker_accepts_a_fully_documented_source() {
        val sample = """
            package sample

            /** Holds the app's settings in memory. */
            class Settings {
                /** Turns the dark theme on or off. */
                @JvmOverloads
                fun setDark(dark: Boolean = true) {}
            }
        """.trimIndent()

        assertEquals(emptyList<String>(), findMissingKdoc("Sample.kt", sample))
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
     * Returns one line per undocumented declaration in [source], as `fileName:line kind name`.
     */
    private fun findMissingKdoc(fileName: String, source: String): List<String> {
        val lines = source.lines()
        val missing = mutableListOf<String>()
        var inBlockComment = false

        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trim()

            // Skip the inside of block comments, so a "fun" mentioned in a comment is ignored.
            if (inBlockComment) {
                if (line.contains("*/")) inBlockComment = false
                return@forEachIndexed
            }
            if (line.startsWith("//")) return@forEachIndexed

            var code = line
            var sameLineKdoc: String? = null
            if (line.startsWith("/*")) {
                val end = line.indexOf("*/", startIndex = 2)
                if (end < 0) {
                    inBlockComment = true
                    return@forEachIndexed
                }
                // A one-line comment, possibly followed by code on the same line.
                if (line.startsWith("/**")) sameLineKdoc = line.substring(0, end + 2)
                code = line.substring(end + 2).trim()
            }

            val declaration = declarationOf(code) ?: return@forEachIndexed
            val documented = if (sameLineKdoc != null) {
                hasSentence(sameLineKdoc)
            } else {
                precedingKdoc(lines, index)?.let(::hasSentence) ?: false
            }
            if (!documented) missing += "$fileName:${index + 1} $declaration"
        }
        return missing
    }

    /**
     * Returns the text of the KDoc comment that ends right above line [index] of [lines],
     * skipping annotation lines, or null if there is none.
     */
    private fun precedingKdoc(lines: List<String>, index: Int): String? {
        var end = index - 1
        while (end >= 0 && lines[end].trim().startsWith("@")) end--
        if (end < 0 || !lines[end].trim().endsWith("*/")) return null

        var start = end
        while (start >= 0 && !lines[start].trim().startsWith("/*")) start--
        if (start < 0 || !lines[start].trim().startsWith("/**")) return null
        return lines.subList(start, end + 1).joinToString("\n")
    }

    /**
     * Tells whether the KDoc [comment] holds at least one sentence: two or more words ending in
     * a full stop, question mark or exclamation mark, before any `@param`-style tag.
     */
    private fun hasSentence(comment: String): Boolean {
        val description = comment
            .removePrefix("/**")
            .removeSuffix("*/")
            .lines()
            .map { it.trim().removePrefix("*").trim() }
            .takeWhile { !it.startsWith("@") }
            .joinToString(" ")
        return SENTENCE.containsMatchIn(description)
    }

    /**
     * Returns `kind name` when [code] starts a class, object, interface or non-override function
     * declaration, or null for any other line.
     */
    private fun declarationOf(code: String): String? {
        var rest = code
        // Drop leading annotations such as @Composable or @Serializable on the same line.
        while (true) {
            val match = ANNOTATION.find(rest) ?: break
            rest = rest.substring(match.range.last + 1)
        }

        val words = rest.split(WHITESPACE).filter { it.isNotEmpty() }
        var i = 0
        var isOverride = false
        var isCompanion = false
        while (i < words.size && words[i] in MODIFIERS) {
            if (words[i] == "override") isOverride = true
            if (words[i] == "companion") isCompanion = true
            i++
        }
        val keyword = words.getOrNull(i)?.substringBefore('<') ?: return null
        val next = words.getOrNull(i + 1)

        return when {
            keyword == "fun" && next == "interface" ->
                "interface ${nameOf(words.getOrNull(i + 2))}"
            keyword == "fun" -> if (isOverride) null else "fun ${nameOf(next)}"
            keyword == "class" || keyword == "interface" -> "$keyword ${nameOf(next)}"
            keyword == "object" -> when {
                isCompanion && (next == null || next.startsWith("{")) -> "object companion"
                // `object : Something { ... }` is an expression, not a declaration.
                next == null || next.startsWith(":") || next.startsWith("{") -> null
                else -> "object ${nameOf(next)}"
            }
            else -> null
        }
    }

    /** Cuts a declaration's name out of the word after its keyword, e.g. `Foo(x: Int)` -> `Foo`. */
    private fun nameOf(word: String?): String =
        word.orEmpty().substringAfterLast('.').takeWhile { it.isLetterOrDigit() || it == '_' || it == '`' }

    /** Patterns and keyword sets the checker uses. */
    private companion object {
        /** A leading annotation, with an optional argument list that has no nested brackets. */
        val ANNOTATION = Regex("""^@[\w.:]+(\([^()]*\))?\s*""")

        /** Any run of whitespace. */
        val WHITESPACE = Regex("""\s+""")

        /** Two or more words followed by sentence-ending punctuation. */
        val SENTENCE = Regex("""\p{L}[\p{L}\p{N}'-]*\W+\S.*?[.!?]""")

        /** Modifiers that may come before `class`, `object`, `interface` or `fun`. */
        val MODIFIERS = setOf(
            "public", "private", "internal", "protected", "open", "abstract", "sealed", "data",
            "inline", "enum", "annotation", "inner", "value", "companion", "suspend", "operator",
            "infix", "tailrec", "external", "actual", "expect", "final", "override",
        )
    }
}
