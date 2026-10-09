package com.phongkaster.kanso

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Checks the "one class per file" rule of CLAUDE.md (DoD 10) on every Kotlin file of the app, its
 * main code and its tests alike: a file holds at most one top-level type, and a file that holds
 * one is named after it (`Foo.kt` for `Foo`). It also checks, on small sample sources written as
 * strings, that the checker itself reads Kotlin correctly.
 *
 * A top-level type is a class, interface or object declared directly in the file, not inside
 * anything else, whatever modifiers come before it: `data class`, `enum class`, `sealed interface`,
 * `fun interface`, `data object`, `private class` and `internal object` all count. A file with no
 * type at all (a screen's composables, `AppModule.kt`) is fine, and top-level functions,
 * properties and constants may sit beside the one type. These are not top-level types: a type
 * nested inside another type, a `companion object`, an `object : Foo { }` expression (an unnamed
 * object made on the spot), a local class declared inside a function body, and `Foo::class`
 * (which names a class without declaring one).
 *
 * The checker reads a file in three steps:
 * 1. It blanks out every comment, string literal and character literal: each of their characters
 *    becomes a space and each line break stays, so the word `class` in a KDoc or a string is never
 *    taken for code, and every line keeps its number.
 * 2. It blanks out everything inside brackets, leaving only the file's top level: of
 *    `class Foo(val x: Int) { class Inner }` only `class Foo`, the brackets and spaces are left.
 * 3. In what is left, each `class`, `interface` or `object` keyword followed by a name declares
 *    one top-level type. Annotations and modifiers in front of it do not change that.
 *
 * Like `KdocCoverageTest`, this is a plain text scan: no Robolectric and no compiling.
 */
class OneTypePerFileTest {

    /**
     * Every Kotlin file under app/src/main/java and app/src/test/java follows the rule, apart from
     * the files on [ALLOWED].
     */
    @Test
    fun every_kotlin_file_holds_at_most_one_type_named_after_it() {
        val problems = unexpectedProblems(scanAppSources(), ALLOWED)
        assertTrue(
            "These files hold a second top-level type, or a type not named after the file. Give each " +
                "type named below a file of its own, named after it (paths are below " +
                "app/src/main/java/com/phongkaster/kanso or app/src/test/java/com/phongkaster/kanso):\n" +
                problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }

    /**
     * Every file on [ALLOWED] still exists and still breaks the rule. The list is empty now, so
     * this passes at once; it stays so that a file added to the list later cannot be forgotten
     * there after it is fixed. (While the list held the fourteen files of Run 4, this test proved
     * on the real code that the checker finds a second type and a misnamed type; now the sample
     * sources in the tests below carry that proof.)
     */
    @Test
    fun every_allowed_file_still_breaks_the_rule() {
        val stale = staleEntries(scanAppSources(), ALLOWED)
        assertTrue(
            "These ALLOWED entries are stale; remove them from the list:\n" + stale.joinToString("\n"),
            stale.isEmpty(),
        )
    }

    /** The checker reports a second class beside a test class, past the test's annotations. */
    @Test
    fun the_checker_reports_two_top_level_classes() {
        val sample = """
            package sample

            /** A test of the gallery. */
            @RunWith(AndroidJUnit4::class)
            @Config(sdk = [35])
            class GalleryTest {
                /** Finds the screen's nodes. */
                @get:Rule
                val compose = createComposeRule()
            }

            /** A fake beside the test: it needs a file of its own. */
            class FakeSource : Source
        """.trimIndent()

        assertEquals(
            listOf(
                "GalleryTest.kt:13 holds 2 top-level types (GalleryTest, FakeSource): a file may hold only one",
                "GalleryTest.kt:13 class FakeSource is not named after its file: it belongs in FakeSource.kt",
            ),
            findProblems("GalleryTest.kt", sample),
        )
    }

    /** The checker reports a private class beside a class: a private type counts all the same. */
    @Test
    fun the_checker_reports_a_private_class_beside_a_class() {
        val sample = """
            package sample

            /** The file's own class. */
            class Store(private val files: FileSystem)

            /** A private helper is a top-level type all the same. */
            private class RetryingFileSystem : FileSystem()
        """.trimIndent()

        assertEquals(
            listOf(
                "Store.kt:7 holds 2 top-level types (Store, RetryingFileSystem): a file may hold only one",
                "Store.kt:7 class RetryingFileSystem is not named after its file: it belongs in RetryingFileSystem.kt",
            ),
            findProblems("Store.kt", sample),
        )
    }

    /**
     * The checker reports a second navigation key in a file, though each key's `@Serializable`
     * sits on its own line above it.
     */
    @Test
    fun the_checker_reports_an_annotated_key_beside_another_type() {
        val sample = """
            package sample

            import kotlinx.serialization.Serializable

            /** The Settings screen's key. */
            @Serializable
            data object Settings : NavKey

            /** The gallery's key, under an annotation line of its own. */
            @Serializable
            data class Gallery(val tool: Tool) : NavKey
        """.trimIndent()

        assertEquals(
            listOf(
                "Settings.kt:11 holds 2 top-level types (Settings, Gallery): a file may hold only one",
                "Settings.kt:11 class Gallery is not named after its file: it belongs in Gallery.kt",
            ),
            findProblems("Settings.kt", sample),
        )
    }

    /**
     * The checker reports a lone type in a file named after something else, whatever the kind of
     * type: an enum class, a fun interface or an internal object. The file name is taken from the
     * last part of the path.
     */
    @Test
    fun the_checker_reports_a_type_in_a_file_named_otherwise() {
        val enumSample = """
            package sample

            /** How much access the phone has granted. */
            enum class PermissionAccess { Full, Partial, None }

            /** A function may sit beside the type, whatever its name. */
            fun readAccess(): PermissionAccess = PermissionAccess.None
        """.trimIndent()
        val funInterfaceSample = """
            package sample

            /** Decodes a whole photo. */
            fun interface FullSizeDecoder {
                /** Decodes [input], or returns null when it is not a picture. */
                fun decode(input: InputStream): Bitmap?
            }
        """.trimIndent()
        val objectSample = """
            package sample

            /** Helpers the tests share. */
            internal object ToolFixtures
        """.trimIndent()

        assertEquals(
            listOf(
                "main/ui/permission/PermissionGate.kt:4 class PermissionAccess is not named after its file: " +
                    "it belongs in PermissionAccess.kt",
            ),
            findProblems("main/ui/permission/PermissionGate.kt", enumSample),
        )
        assertEquals(
            listOf("UprightJpeg.kt:4 interface FullSizeDecoder is not named after its file: it belongs in FullSizeDecoder.kt"),
            findProblems("UprightJpeg.kt", funInterfaceSample),
        )
        assertEquals(
            listOf("Fixtures.kt:4 object ToolFixtures is not named after its file: it belongs in ToolFixtures.kt"),
            findProblems("Fixtures.kt", objectSample),
        )
    }

    /** A file with Windows line ends (a carriage return before each line feed) reads the same. */
    @Test
    fun the_checker_reads_windows_line_ends() {
        val sample = """
            package sample

            /** The first class. */
            class First

            /** The second class. */
            class Second
        """.trimIndent().replace("\n", "\r\n")

        assertEquals(
            listOf(
                "First.kt:7 holds 2 top-level types (First, Second): a file may hold only one",
                "First.kt:7 class Second is not named after its file: it belongs in Second.kt",
            ),
            findProblems("First.kt", sample),
        )
    }

    /**
     * The checker finds only the one real type in a file full of things that are not top-level
     * types: cases nested in a sealed interface, a companion object, `Foo::class`, two object
     * expressions and a local class. An enum whose entries have bodies and methods, and a file
     * with no type at all, are accepted too.
     */
    @Test
    fun the_checker_accepts_nested_types_companions_expressions_and_local_classes() {
        val sealedSample = """
            package sample

            import kotlin.reflect.KClass

            /** The file's one type: a sealed interface whose cases are nested inside it. */
            sealed interface Shape {
                /** A case with no data. */
                data object Empty : Shape

                /** A case with data. */
                data class Circle(val radius: Int) : Shape

                /** Values every shape shares. */
                companion object {
                    /** The smallest radius. */
                    const val MIN_RADIUS = 1
                }
            }

            /** Names a class without declaring one. */
            val shapeClass: KClass<Shape> = Shape::class

            /** An object expression: an unnamed object made on the spot, not a declaration. */
            val task = object : Runnable {
                /** Does nothing. */
                override fun run() = Unit
            }

            /** An object expression with no supertype at all. */
            val lock = object {}

            /** A local class lives only inside the function that declares it. */
            fun describe(shape: Shape): String {
                class Label(val text: String)
                return Label(shape.toString()).text
            }

            /** A constant may sit beside the type. */
            const val MAX_RADIUS = 10
        """.trimIndent()
        val enumSample = """
            package sample

            /** A colour whose entries have bodies, and methods every entry shares. */
            enum class Colour(val hex: String) {
                RED("#FF0000") {
                    override fun label() = "red"
                },
                GREEN("#00FF00") {
                    override fun label() = "green"
                };

                /** Says the colour's name. */
                abstract fun label(): String

                /** Tells whether this is the red entry. */
                fun isRed(): Boolean = this == RED
            }
        """.trimIndent()
        val noTypeSample = """
            package sample

            /** The app's wiring: values and functions, no type at all. */
            val appModule = module {
                single { Repository() }
            }

            /** Starts nothing. */
            fun start() = Unit
        """.trimIndent()

        assertAccepted("Shape.kt", sealedSample, listOf("Shape"))
        assertAccepted("Colour.kt", enumSample, listOf("Colour"))
        assertAccepted("AppModule.kt", noTypeSample, emptyList())
    }

    /**
     * The checker is not fooled by the words `class`, `object` and `interface` in a comment, a
     * nested block comment, a KDoc, a string, a raw string, a template or a backticked name, nor
     * by braces and quotes inside character literals and templates. The file's one type comes
     * after the traps (the last one on the same line as the last trap), so a misreading of them
     * tends to show up as that type hidden or as one more type found.
     */
    @Test
    fun the_checker_ignores_types_named_in_comments_strings_and_characters() {
        val sample = """
            package sample

            // A line comment: class InLineComment
            /* A block comment /* with a nested one */ class StillInComment */

            /** A KDoc that says class InKdoc, object InKdoc and interface InKdoc. */
            val close = '}'

            /** Opens nothing: it is only a character. */
            val open = '{'

            /** Two double quotes and an apostrophe, each a character. */
            val quotes = listOf('"', '\'', '"')

            /** A template holding a string that holds a closing brace. */
            val closing = "${'$'}{a.map { "}" }}"

            /** A template holding strings, followed by the word class. */
            val mode = "${'$'}{if (dark) "dark" else "light"} class InTemplate"

            /** A string that names a type without declaring one. */
            val plain = "class InString"

            /** A raw string that does the same over several lines. */
            val raw = ${TRIPLE_QUOTE}
                class InRawString { val x = "y" }
            ${TRIPLE_QUOTE}

            /** A function whose name holds a whole declaration's worth of words. */
            fun `has a class Inner inside`() = Unit

            /** A template holding a quote character, then the file's one type on the same line. */
            val oddQuote = "${'$'}{'"'}"; class Words
        """.trimIndent()

        assertAccepted("Words.kt", sample, listOf("Words"))
    }

    /**
     * The allow-list logic: a listed file's problems are hidden, an unlisted file's are reported,
     * and a listed file that now follows the rule, or no longer exists, is a stale entry.
     */
    @Test
    fun the_allow_list_hides_listed_files_and_reports_stale_entries() {
        val found: Map<String, List<String>> = mapOf(
            "main/Listed.kt" to listOf("main/Listed.kt:9 class Other is not named after its file: it belongs in Other.kt"),
            "main/Fixed.kt" to emptyList<String>(),
            "test/Broken.kt" to listOf("test/Broken.kt:5 class Helper is not named after its file: it belongs in Helper.kt"),
        )
        val allowed = setOf("main/Listed.kt", "main/Fixed.kt", "main/Gone.kt")

        assertEquals(
            listOf("test/Broken.kt:5 class Helper is not named after its file: it belongs in Helper.kt"),
            unexpectedProblems(found, allowed),
        )
        assertEquals(
            listOf(
                "main/Fixed.kt is on ALLOWED but no longer breaks the rule",
                "main/Gone.kt is on ALLOWED but no such file exists",
            ),
            staleEntries(found, allowed),
        )
    }

    /**
     * Fails unless the checker finds exactly the top-level types [expected] in [source], by name
     * and in order, and reports no problem for it as the file [fileName]. Checking the types found,
     * and not only the empty report, proves the sample was really read rather than skipped.
     */
    private fun assertAccepted(fileName: String, source: String, expected: List<String>) {
        assertEquals(expected, findTopLevelTypes(source).map { it.name })
        assertEquals(emptyList<String>(), findProblems(fileName, source))
    }

    /**
     * Reads every Kotlin file under app/src/main/java and app/src/test/java and returns what
     * [findProblems] reports for each (an empty list for a file that follows the rule). The keys
     * are the files' paths below the app's package folder with a `main/` or `test/` prefix, the
     * keys [ALLOWED] uses, in sorted order. It fails when either folder holds no Kotlin file, so a
     * scan that looks in the wrong place cannot pass. Both folders are inside `app/src`, so the
     * scan never reaches `reference/` or the engine modules in `core/`.
     */
    private fun scanAppSources(): Map<String, List<String>> {
        val mainRoot = findAppSourceRoot()
        // Two levels up from app/src/main/java is app/src, which also holds test/java.
        val testRoot = File(mainRoot, "../../test/java").canonicalFile
        val found = mutableMapOf<String, List<String>>()
        for ((prefix, root) in listOf("main" to mainRoot, "test" to testRoot)) {
            val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
            assertTrue("No Kotlin sources found under $root", files.isNotEmpty())
            files.sortedBy { it.path }.forEach { file ->
                val below = file.relativeTo(root).invariantSeparatorsPath.removePrefix(PACKAGE_FOLDER)
                val path = "$prefix/$below"
                found[path] = findProblems(path, file.readText())
            }
        }
        return found.toSortedMap()
    }

    /** Returns the problems of every file in [found] that is not on [allowed], in path order. */
    private fun unexpectedProblems(found: Map<String, List<String>>, allowed: Set<String>): List<String> =
        found.filterKeys { it !in allowed }.values.flatten()

    /**
     * Returns one line per entry of [allowed] that no longer earns its place: a path with no file
     * in [found], or a file that now follows the rule.
     */
    private fun staleEntries(found: Map<String, List<String>>, allowed: Set<String>): List<String> =
        allowed.sorted().mapNotNull { path ->
            val problems = found[path]
            when {
                problems == null -> "$path is on ALLOWED but no such file exists"
                problems.isEmpty() -> "$path is on ALLOWED but no longer breaks the rule"
                else -> null
            }
        }

    /**
     * Returns one line per broken rule in the file at [path] whose text is [source], as
     * `path:line message` with lines counted from 1: one line when the file holds two or more
     * top-level types (on the second one's line), and one line for each top-level type that is
     * not named after the file.
     */
    private fun findProblems(path: String, source: String): List<String> {
        val types = findTopLevelTypes(source)
        val fileName = path.substringAfterLast('/').removeSuffix(".kt")
        val problems = mutableListOf<String>()
        if (types.size > 1) {
            val names = types.joinToString { it.name }
            problems += "$path:${types[1].line} holds ${types.size} top-level types ($names): " +
                "a file may hold only one"
        }
        types.filter { it.name != fileName }.forEach { type ->
            problems += "$path:${type.line} ${type.kind} ${type.name} is not named after its file: " +
                "it belongs in ${type.name}.kt"
        }
        return problems
    }

    /**
     * Returns the top-level types declared in [source], in the order they appear. Comments and
     * literals are blanked out first ([Blanker]), then everything inside brackets
     * ([topLevelText]), then annotations; each `class`, `interface` or `object` keyword left that
     * is followed by a name declares one type.
     */
    private fun findTopLevelTypes(source: String): List<TopLevelType> {
        // Windows line ends become a plain line feed, so a line break is one character whatever
        // the file uses (six test files use Windows line ends).
        val text = source.replace("\r\n", "\n").replace('\r', '\n')
        val code = Blanker(text).blank()
        // Annotations such as @Serializable or @get:Rule never declare a type, whether they sit on
        // the declaration's own line or on the lines above it, so they are blanked too.
        val topLevel = ANNOTATION.replace(topLevelText(code)) { " ".repeat(it.value.length) }
        return TYPE_KEYWORD.findAll(topLevel).mapNotNull { match ->
            val start = match.range.first
            val before = wordBefore(topLevel, start)
            val name = nameAfter(topLevel, match.range.last + 1)
            when {
                // Foo::class names a class without declaring one.
                before == "::" -> null
                // A companion object belongs to the class around it, never to the file.
                before == "companion" -> null
                // The word is part of a name written between backticks, not the keyword.
                topLevel.getOrNull(start - 1) == '`' -> null
                // `object : Foo { }` or `object { }`: an unnamed object made on the spot.
                name == null -> null
                else -> TopLevelType(kind = match.value, name = name, line = lineOf(topLevel, start))
            }
        }.toList()
    }

    /**
     * Returns [code] (already free of comments and literals) with everything inside brackets of
     * any kind blanked out, so only the file's top level is left. The outermost brackets are kept,
     * so that what follows `object` still shows whether it is a name or the `{` or `:` of an
     * object expression. A type declared in a class body, a function body or an argument list is
     * therefore gone.
     */
    private fun topLevelText(code: String): String {
        val out = StringBuilder(code.length)
        var depth = 0
        for (c in code) {
            when (c) {
                '{', '(', '[' -> {
                    out.append(if (depth == 0) c else ' ')
                    depth++
                }
                '}', ')', ']' -> {
                    // A stray closing bracket (never seen in code that compiles) must not push the
                    // depth below zero and hide the rest of the file.
                    depth = (depth - 1).coerceAtLeast(0)
                    out.append(if (depth == 0) c else ' ')
                }
                '\n' -> out.append(c)
                else -> out.append(if (depth == 0) c else ' ')
            }
        }
        return out.toString()
    }

    /**
     * Returns what comes right before [index] in [text], skipping whitespace: `::` when the two
     * colons of `Foo::class` are there, otherwise the word there (empty when there is none).
     */
    private fun wordBefore(text: String, index: Int): String {
        var end = index
        while (end > 0 && text[end - 1].isWhitespace()) end--
        if (end >= 2 && text[end - 1] == ':' && text[end - 2] == ':') return "::"
        var start = end
        while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '_')) start--
        return text.substring(start, end)
    }

    /**
     * Returns the name that starts at [from] in [text] once whitespace is skipped (without the
     * backticks of a name written between backticks), or null when no name starts there, as after
     * the `object` of `object : Runnable`.
     */
    private fun nameAfter(text: String, from: Int): String? {
        var i = from
        while (i < text.length && text[i].isWhitespace()) i++
        if (i < text.length && text[i] == '`') {
            val close = text.indexOf('`', startIndex = i + 1)
            return if (close < 0) null else text.substring(i + 1, close)
        }
        val start = i
        while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) i++
        return if (i == start || text[start].isDigit()) null else text.substring(start, i)
    }

    /** Returns the line, counted from 1, that the character at [index] of [text] is on. */
    private fun lineOf(text: String, index: Int): Int = text.substring(0, index).count { it == '\n' } + 1

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
     * One top-level type found in a file: its [kind] (the keyword `class`, `interface` or
     * `object`), its [name], and the [line], counted from 1, that its keyword is on.
     */
    private data class TopLevelType(val kind: String, val name: String, val line: Int)

    /**
     * Blanks out the comments, string literals and character literals of one Kotlin source,
     * [text]: each of their characters becomes a space and each line break stays, so only code is
     * left and every line keeps its number. Code itself is copied as it is.
     *
     * It follows Kotlin's own reading rules where they matter here:
     * - Block comments nest: a block comment (a KDoc included) may hold another one, and only the
     *   close that matches the first opening ends it.
     * - A string template, a dollar sign and a brace, holds code that may itself hold strings and
     *   braces, as in `"${if (dark) "dark" else "light"}"`; the string goes on after it.
     * - A raw string runs from three double quotes to the next three and has no escapes.
     * - A character literal may hide a quote, a brace or an apostrophe: `'"'`, `'}'`, `'\''`.
     *
     * It keeps a reading position, [at], and writes one character of [out] per character read.
     */
    private class Blanker(private val text: String) {

        /** The result: one character for each character of [text]. */
        private val out = StringBuilder(text.length)

        /** The index in [text] of the next character to read. */
        private var at = 0

        /** Reads the whole of [text] and returns it with its comments and literals blanked out. */
        fun blank(): String {
            code(keep = true, insideTemplate = false)
            return out.toString()
        }

        /**
         * Reads code, up to the end of [text] or, when [insideTemplate], up to the brace that
         * closes the template (left for the caller to read). Code is copied when [keep] and
         * blanked otherwise, since the code of a template is part of its string. Braces are
         * counted, so that a lambda's closing brace inside a template does not end the template.
         */
        private fun code(keep: Boolean, insideTemplate: Boolean) {
            var braces = 0
            while (at < text.length) {
                val c = text[at]
                when {
                    text.startsWith("//", at) -> lineComment()
                    text.startsWith("/*", at) -> blockComment()
                    text.startsWith(TRIPLE_QUOTE, at) -> rawString()
                    c == '"' -> string()
                    c == '\'' -> character()
                    c == '`' -> backtickName(keep)
                    c == '}' && insideTemplate && braces == 0 -> return
                    else -> {
                        if (c == '{') braces++
                        if (c == '}') braces--
                        take(keep)
                    }
                }
            }
        }

        /** Blanks a line comment up to the end of its line; the line break itself is code. */
        private fun lineComment() {
            while (at < text.length && text[at] != '\n') skip()
        }

        /**
         * Blanks a block comment or a KDoc, starting at its opening. Each opening met inside goes
         * one level deeper and each close comes back one level, because Kotlin lets block
         * comments nest; the comment ends when the first level is closed.
         */
        private fun blockComment() {
            var depth = 0
            while (at < text.length) {
                when {
                    text.startsWith("/*", at) -> {
                        depth++
                        skip()
                        skip()
                    }
                    text.startsWith("*/", at) -> {
                        depth--
                        skip()
                        skip()
                        if (depth == 0) return
                    }
                    else -> skip()
                }
            }
        }

        /**
         * Blanks a string literal in double quotes, starting at its opening quote. A backslash
         * escapes the character after it, so an escaped quote does not end the string, and a
         * dollar sign followed by a brace opens a template. A plain string never spans lines, so
         * a line break ends it too, which keeps a stray quote from blanking the rest of the file.
         */
        private fun string() {
            skip() // the opening quote
            while (at < text.length) {
                when {
                    text[at] == '\\' -> {
                        skip() // the backslash
                        if (at < text.length) skip() // the character it escapes
                    }
                    text[at] == '"' -> {
                        skip() // the closing quote
                        return
                    }
                    text[at] == '\n' -> return
                    text.startsWith("\${", at) -> template()
                    else -> skip()
                }
            }
        }

        /**
         * Blanks a raw string, starting at its three opening quotes. It has no escapes and ends at
         * the next three quotes, but a dollar sign followed by a brace still opens a template. A
         * raw string may end in more than three quotes in a row: the extra ones belong to its
         * text, so every one of them is read.
         */
        private fun rawString() {
            repeat(TRIPLE_QUOTE.length) { skip() }
            while (at < text.length) {
                when {
                    text.startsWith(TRIPLE_QUOTE, at) -> {
                        while (at < text.length && text[at] == '"') skip()
                        return
                    }
                    text.startsWith("\${", at) -> template()
                    else -> skip()
                }
            }
        }

        /** Blanks a string template: its dollar sign and brace, the code inside, its closing brace. */
        private fun template() {
            skip() // the dollar sign
            skip() // the opening brace
            code(keep = false, insideTemplate = true)
            if (at < text.length) skip() // the closing brace
        }

        /**
         * Blanks a character literal such as `'a'`, `'}'`, `'"'` or `'\''`, starting at its opening
         * apostrophe. After a backslash the next character belongs to the literal, so the escaped
         * apostrophe of `'\''` does not close it.
         */
        private fun character() {
            skip() // the opening apostrophe
            if (at < text.length && text[at] == '\\') skip() // the backslash of an escape
            if (at < text.length && text[at] != '\n') skip() // the character, or the escaped one
            // Whatever is left before the closing apostrophe, such as the hex digits of a Unicode escape.
            while (at < text.length && text[at] != '\'' && text[at] != '\n') skip()
            if (at < text.length && text[at] == '\'') skip() // the closing apostrophe
        }

        /**
         * Copies a name written between backticks (such as a test named with spaces), turning
         * every character that is not a letter or a digit into an underscore, so that a word
         * inside the name, like class, is never read as a keyword. Inside a template ([keep]
         * false) it is blanked like the rest of the template's code.
         */
        private fun backtickName(keep: Boolean) {
            take(keep) // the opening backtick
            while (at < text.length && text[at] != '`' && text[at] != '\n') {
                val c = text[at]
                out.append(if (!keep) ' ' else if (c.isLetterOrDigit()) c else '_')
                at++
            }
            if (at < text.length && text[at] == '`') take(keep) // the closing backtick
        }

        /** Copies the next character when [keep], or blanks it, and moves on; a line break always stays. */
        private fun take(keep: Boolean) {
            val c = text[at]
            out.append(if (keep || c == '\n') c else ' ')
            at++
        }

        /** Blanks the next character (a line break stays) and moves on. */
        private fun skip() = take(keep = false)
    }

    /** The allow-list, and the text and patterns the checker uses. */
    private companion object {
        /**
         * The files that are allowed to break the rule, as paths below the app's package folder
         * with a `main/` or `test/` prefix; their problems are not reported. It is empty: when
         * the rule arrived (Run 4) fourteen files broke it, and each has been split into one file
         * per type. A file added here would hide a real problem, so it needs the owner's approval;
         * [every_allowed_file_still_breaks_the_rule] fails while a fixed file is still on the list.
         */
        private val ALLOWED: Set<String> = emptySet()

        /** The app's package folder: every scanned path starts with it below its source root. */
        const val PACKAGE_FOLDER = "com/phongkaster/kanso/"

        /** Three double quotes: what opens and closes a raw string. */
        const val TRIPLE_QUOTE = "\"\"\""

        /** A keyword that declares a type, as a whole word. */
        val TYPE_KEYWORD = Regex("""\b(class|interface|object)\b""")

        /** An annotation's name, with its use-site target if any, such as `@Serializable` or `@get:Rule`. */
        val ANNOTATION = Regex("""@[\p{L}_][\w.:]*""")
    }
}
