package com.phongkaster.kanso

import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Checks that every screen is built the one way CLAUDE.md asks for (DoD 12), so a later change
 * cannot quietly give a screen a second source of state:
 *
 * - The screen packages under `ui/` hold exactly the six screen ViewModels.
 * - Each ViewModel gives out exactly one StateFlow, and it is a StateFlow of the screen's own
 *   UiState (`HomeViewModel` gives out a `StateFlow<HomeUiState>`). A one-shot Flow of events
 *   that is not a StateFlow, such as ToolViewModel's `savedEvents`, and plain read-only values
 *   are allowed beside it.
 * - Each screen's stateless `…Content` function takes that UiState as a parameter.
 * - No UiState declares a `var` or a `Mutable…` type, so a snapshot can never change in place.
 *
 * The ViewModel check uses Java reflection: reading a compiled class's methods, and their types,
 * while the test runs. Kotlin's own reflection library (kotlin-reflect) is not on the test
 * classpath, so the check reads what the Kotlin compiler wrote into the class file. There, a
 * property `val state: StateFlow<HomeUiState>` becomes a public method `getState()` (a "getter")
 * whose return type is `StateFlow` and whose generic return type also records the type argument
 * `HomeUiState`. The classes are only loaded, never initialised or constructed, so no Koin,
 * Android or Robolectric is needed: this is a plain JUnit test.
 *
 * The other two checks read the source as text, in the style of `KdocCoverageTest`, after
 * blanking out comments and the inside of string literals, so prose is never taken for code.
 * Every checker is also run on small samples written inside this class, to prove that it goes
 * red on a broken screen and stays green on a good one.
 */
class ScreenStateTest {

    /**
     * The `ui` package holds exactly the six screen ViewModels, one per screen. A seventh, or one
     * gone missing, means a screen was added or removed without this test being brought along.
     */
    @Test
    fun the_screen_packages_hold_exactly_the_six_view_models() {
        assertEquals(
            "The screen packages under ui/ must hold exactly one ViewModel per screen",
            EXPECTED_VIEW_MODELS,
            findViewModelPaths(uiFolder()).toSet(),
        )
    }

    /**
     * Every screen's ViewModel gives out exactly one StateFlow, a StateFlow of its own UiState:
     * `HomeViewModel` a `StateFlow<HomeUiState>`, `ToolViewModel` a `StateFlow<ToolUiState>`.
     */
    @Test
    fun every_view_model_gives_out_exactly_one_state_flow_of_its_ui_state() {
        val viewModels = findViewModelPaths(uiFolder())
        assertTrue("No ViewModel found under ${uiFolder()}", viewModels.isNotEmpty())

        val problems = viewModels.flatMap { path ->
            // home/HomeViewModel is the class com.phongkaster.kanso.ui.home.HomeViewModel, and
            // its UiState is com.phongkaster.kanso.ui.home.HomeUiState.
            val className = UI_PACKAGE + "." + path.replace('/', '.')
            val viewModel = loadWithoutInitialising(className)
                ?: return@flatMap listOf("$path: the class $className is not on the test classpath")
            findStateProblems(viewModel, className.removeSuffix("ViewModel") + "UiState")
        }
        assertTrue(
            "Each ViewModel must give out exactly one StateFlow, of its own UiState:\n" +
                problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }

    /**
     * Every screen's stateless `…Content` function (`HomeContent` in `home/HomeScreen.kt`) takes
     * the screen's UiState as one of its parameters, so it draws from the one snapshot the
     * ViewModel gives out.
     */
    @Test
    fun every_content_function_takes_its_screen_ui_state() {
        val ui = uiFolder()
        val viewModels = findViewModelPaths(ui)
        assertTrue("No ViewModel found under $ui", viewModels.isNotEmpty())

        val problems = viewModels.flatMap { path ->
            // home/HomeViewModel belongs to the screen Home, whose functions are in home/HomeScreen.kt.
            val screen = path.substringAfterLast('/').removeSuffix("ViewModel")
            val screenFile = path.removeSuffix("ViewModel") + "Screen.kt"
            val file = File(ui, screenFile)
            if (!file.isFile) return@flatMap listOf("$screenFile is missing")
            findContentProblems(screenFile, file.readText(), screen)
        }
        assertTrue(
            "Each screen's Content function must take the screen's UiState:\n" +
                problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }

    /**
     * No UiState under `ui/` declares a `var` or uses a `Mutable…` type outside its comments, so a
     * snapshot, once made, can never be changed in place: a change is always a new snapshot.
     */
    @Test
    fun no_ui_state_declares_a_var_or_a_mutable_type() {
        val ui = uiFolder()
        val files = ui.walkTopDown()
            .filter { it.isFile && it.name.endsWith("UiState.kt") }
            .sortedBy { it.path }
            .toList()
        assertTrue("No UiState file found under $ui", files.isNotEmpty())

        val problems = files.flatMap { file ->
            findMutableDeclarations(file.relativeTo(ui).invariantSeparatorsPath, file.readText())
        }
        assertTrue(
            "A UiState must be read-only, but these lines could change one in place:\n" +
                problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }

    /** The state checker reports a ViewModel that gives out two StateFlows. */
    @Test
    fun the_state_checker_reports_two_state_flows() {
        assertEquals(
            listOf("TwoStatesViewModel declares 2 public StateFlows, not exactly one: [getBusy, getState]"),
            findStateProblems(TwoStatesViewModel::class.java, SampleUiState::class.java.name),
        )
    }

    /** The state checker reports a ViewModel whose one StateFlow holds a String, not its UiState. */
    @Test
    fun the_state_checker_reports_a_state_flow_of_the_wrong_type() {
        assertEquals(
            listOf(
                "WrongTypeViewModel.getState() is a StateFlow of java.lang.String, " +
                    "not of ${SampleUiState::class.java.name}",
            ),
            findStateProblems(WrongTypeViewModel::class.java, SampleUiState::class.java.name),
        )
    }

    /**
     * The state checker reports a ViewModel that gives out its writable MutableStateFlow, through
     * which the screen could put in a snapshot of its own.
     */
    @Test
    fun the_state_checker_reports_a_writable_state_flow() {
        assertEquals(
            listOf("WritableStateViewModel.getState() gives out a MutableStateFlow, not a read-only StateFlow"),
            findStateProblems(WritableStateViewModel::class.java, SampleUiState::class.java.name),
        )
    }

    /**
     * The state checker reports a ViewModel whose second StateFlow comes from the class it
     * extends, which a look at the methods the class itself declares would never see.
     */
    @Test
    fun the_state_checker_reports_a_state_flow_inherited_from_a_base_class() {
        assertEquals(
            listOf("InheritingViewModel declares 2 public StateFlows, not exactly one: [getBusy, getState]"),
            findStateProblems(InheritingViewModel::class.java, SampleUiState::class.java.name),
        )
    }

    /**
     * The state checker accepts a ViewModel with one StateFlow of its UiState, beside a one-shot
     * Flow, plain read-only values and the hidden accessor Kotlin adds for its private state.
     */
    @Test
    fun the_state_checker_accepts_one_state_flow_beside_events_and_plain_values() {
        assertEquals(
            emptyList<String>(),
            findStateProblems(GoodViewModel::class.java, SampleUiState::class.java.name),
        )
    }

    /**
     * The content checker reports a Content function that takes no UiState, even though the
     * UiState's name appears in its comment, in a lambda parameter's type, in a default text and
     * in another function's parameters.
     */
    @Test
    fun the_content_checker_reports_a_content_without_its_ui_state() {
        val sample = """
            package sample

            /** Draws Home from loose values; HomeUiState is named here, in prose, only. */
            @Composable
            fun HomeContent(
                tools: List<Tool>,
                // state: HomeUiState was taken out
                onToolClick: (state: HomeUiState) -> Unit,
                title: String = "state: HomeUiState",
            ) {
                Text(title)
            }

            /** The stateful Home, whose own parameter does not count for HomeContent. */
            @Composable
            fun HomeScreen(state: HomeUiState) {
                HomeContent(state.tools, {}, "Kanso")
            }
        """.trimIndent()

        assertEquals(
            listOf("Sample.kt:5 fun HomeContent takes no HomeUiState parameter"),
            findContentProblems("Sample.kt", sample, "Home"),
        )
    }

    /** The content checker reports a screen file that has no Content function at all. */
    @Test
    fun the_content_checker_reports_a_missing_content_function() {
        val sample = """
            package sample

            /** A screen whose stateless part was renamed, so nothing is called HomeContent. */
            @Composable
            fun HomeBody(state: HomeUiState) {
                Text(state.title)
            }
        """.trimIndent()

        assertEquals(
            listOf("Sample.kt declares no HomeContent function"),
            findContentProblems("Sample.kt", sample, "Home"),
        )
    }

    /**
     * The content checker accepts a Content function that takes its UiState (here by its full
     * name) after a multi-line lambda parameter with a default value, with stray brackets in a
     * comment and in a text that must not end the parameter list early.
     */
    @Test
    fun the_content_checker_accepts_a_content_that_takes_its_ui_state() {
        val sample = """
            package sample

            /** The stateless gallery: draws [state] and reports taps. */
            @Composable
            internal fun GalleryContent(
                onBack: () -> Unit, /* a ) here and a ( here */
                thumbnail: @Composable (photo: Photo, sizePx: Int) -> Unit =
                    { photo, sizePx -> Thumbnail(photo, sizePx, label = ") (") },
                state: com.example.gallery.GalleryUiState,
                modifier: Modifier = Modifier,
            ) {
                Text(state.title)
            }
        """.trimIndent()

        assertEquals(emptyList<String>(), findContentProblems("Sample.kt", sample, "Gallery"))
    }

    /** The UiState checker reports a var, a Mutable type and a mutable builder, each by line. */
    @Test
    fun the_ui_state_checker_reports_a_var_and_mutable_types() {
        val sample = """
            package sample

            /** A UiState done wrong: the screen could change it in place. */
            data class BadUiState(
                var count: Int,
                val photos: MutableList<String>,
                val note: String = "Mutable in a text is only a word",
            ) {
                // a var in a comment is only a word
                val selection = mutableListOf<String>()
            }
        """.trimIndent()

        assertEquals(
            listOf(
                "Sample.kt:5 declares a var",
                "Sample.kt:6 uses MutableList",
                "Sample.kt:10 uses mutableListOf",
            ),
            findMutableDeclarations("Sample.kt", sample),
        )
    }

    /**
     * The UiState checker accepts a read-only UiState whose KDoc and texts mention var and
     * Mutable, and whose field name merely starts with the letters v, a, r.
     */
    @Test
    fun the_ui_state_checker_accepts_a_read_only_ui_state() {
        val sample = """
            package sample

            /**
             * A read-only snapshot. No var and no MutableList here: a Mutable type would let the
             * screen edit the snapshot in place.
             */
            data class SettingsUiState(
                val themeMode: String,
                val note: String = "a var in a text is only a word",
                val variants: List<String> = emptyList(),
            )
        """.trimIndent()

        assertEquals(emptyList<String>(), findMutableDeclarations("Sample.kt", sample))
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

    /** Returns the folder of the `ui` package, which holds one sub-folder per screen. */
    private fun uiFolder(): File = File(findAppSourceRoot(), UI_PACKAGE.replace('.', '/'))

    /**
     * Lists every `…ViewModel.kt` file under [ui] as `folder/ClassName` (for example
     * `home/HomeViewModel`), sorted, so a failure message always lists them in the same order.
     */
    private fun findViewModelPaths(ui: File): List<String> =
        ui.walkTopDown()
            .filter { it.isFile && it.name.endsWith("ViewModel.kt") }
            .map { it.relativeTo(ui).invariantSeparatorsPath.removeSuffix(".kt") }
            .sorted()
            .toList()

    /**
     * Loads the class named [className] from the test classpath without running any of its code,
     * or returns null when there is no such class.
     *
     * `Class.forName(name, false, loader)` only reads the class file: the `false` keeps the
     * class's static set-up from running, and nothing is constructed, so no ViewModel, Koin or
     * Android service is needed. That is all reflection needs to list the class's methods.
     */
    private fun loadWithoutInitialising(className: String): Class<*>? =
        try {
            Class.forName(className, false, ScreenStateTest::class.java.classLoader)
        } catch (missing: ClassNotFoundException) {
            null
        }

    /**
     * Returns one line per way [viewModel] breaks the one-state rule, or nothing when it keeps it.
     *
     * It must give out exactly one StateFlow: one public method (a property's getter counts,
     * since `val state` compiles to `getState()`) whose return type is StateFlow or a kind of
     * StateFlow. Methods the class inherits are looked at too, so a StateFlow added in a base
     * class counts as a second one; what `ViewModel` itself gives out holds no StateFlow. A Flow
     * that is not a StateFlow (a one-shot stream of events) or a plain value is allowed beside
     * it. That one method's declared type must be StateFlow itself, not a MutableStateFlow, so
     * the screen is not handed a way to write to it, and its type argument must be the class
     * named [uiStateName]. Only declared types can be read this way: how the value behind them
     * was made is not looked at.
     */
    private fun findStateProblems(viewModel: Class<*>, uiStateName: String): List<String> {
        val name = viewModel.simpleName
        val stateFlows = viewModel.methods
            .filter { isGivenOut(it) && StateFlow::class.java.isAssignableFrom(it.returnType) }
            // The JVM lists methods in no fixed order; sorting keeps the messages stable.
            .sortedBy { it.name }
        if (stateFlows.size != 1) {
            val names = stateFlows.map { it.name }
            return listOf("$name declares ${stateFlows.size} public StateFlows, not exactly one: $names")
        }

        val getter = stateFlows.single()
        val problems = mutableListOf<String>()
        if (getter.returnType != StateFlow::class.java) {
            problems += "$name.${getter.name}() gives out a ${getter.returnType.simpleName}, " +
                "not a read-only StateFlow"
        }
        val valueType = valueTypeName(getter.genericReturnType)
        if (valueType != uiStateName) {
            problems += "$name.${getter.name}() is a StateFlow of $valueType, not of $uiStateName"
        }
        return problems
    }

    /**
     * Tells whether [method] is part of what its class gives out: public, and written by the
     * programmer rather than made up by the compiler.
     *
     * Kotlin adds hidden methods of its own: `access…` accessors, through which a lambda or an
     * object of another class reaches a private property such as `mutableState` (and which return
     * that MutableStateFlow), helpers for default arguments ending in `default`, and bridges. The
     * class file marks them synthetic; they are also skipped by name, in case one is not marked.
     */
    private fun isGivenOut(method: Method): Boolean =
        Modifier.isPublic(method.modifiers) &&
            !method.isSynthetic &&
            !method.isBridge &&
            !method.name.startsWith(ACCESSOR_PREFIX) &&
            !method.name.endsWith(DEFAULT_SUFFIX)

    /**
     * Names the type of value a StateFlow holds, from the generic [returnType] the class file
     * records for its getter: for `StateFlow<HomeUiState>`, the full name of HomeUiState.
     *
     * Kotlin writes return types without wildcards, so the type argument is normally a plain
     * class; [typeName] still copes with the other kinds of type, so none of them can pass for
     * the UiState by accident.
     */
    private fun valueTypeName(returnType: Type): String {
        if (returnType !is ParameterizedType) return "no type argument"
        return typeName(returnType.actualTypeArguments.first())
    }

    /**
     * Names the class behind [type]: a class's own name; for a generic type such as
     * `List<HomeUiState>`, its outer class (`java.util.List`); for a wildcard such as
     * `? extends HomeUiState`, its upper bound.
     */
    private fun typeName(type: Type): String = when (type) {
        is Class<*> -> type.name
        is ParameterizedType -> typeName(type.rawType)
        is WildcardType -> typeName(type.upperBounds.first())
        else -> type.typeName
    }

    /**
     * Returns one line per `fun <screen>Content(` in [source] that takes no `<screen>UiState`
     * parameter, as `fileName:line ...`, or a single line when there is no such function at all.
     *
     * The parameter list is read with its brackets balanced, so it may span lines and hold
     * lambdas and default values, and a parameter counts only when its own type is the UiState,
     * by its short or its full name. A lambda parameter that merely takes the UiState
     * (`(HomeUiState) -> Unit`) does not count. Comments and texts are blanked out first.
     */
    private fun findContentProblems(fileName: String, source: String, screen: String): List<String> {
        val function = screen + "Content"
        val uiState = screen + "UiState"
        // The UiState written with its package in front (`com.example.home.HomeUiState`): lower-case
        // package names, then the name, and nothing else, so a lambda type does not pass for it.
        val qualified = Regex("""([a-z_]\w*\.)+${Regex.escape(uiState)}""")
        val code = codeOnly(source)
        val declarations = Regex("""\bfun\s+(?:<[^>]*>\s*)?${Regex.escape(function)}\s*\(""")
            .findAll(code)
            .toList()
        if (declarations.isEmpty()) return listOf("$fileName declares no $function function")

        return declarations.mapNotNull { declaration ->
            val where = "$fileName:${lineOf(code, declaration.range.first)} fun $function"
            // The match ends on the bracket that opens the parameter list.
            val parameters = parameterListAt(code, declaration.range.last)
                ?: return@mapNotNull "$where never closes its parameter list"
            val types = topLevelParts(parameters).mapNotNull { parameterTypeOf(it) }
            if (types.any { it == uiState || qualified.matches(it) }) {
                null
            } else {
                "$where takes no $uiState parameter"
            }
        }
    }

    /**
     * Returns the text between the bracket at [open] in [code] and the bracket that closes it,
     * counting every kind of bracket on the way, or null when it is never closed.
     */
    private fun parameterListAt(code: String, open: Int): String? {
        var depth = 0
        for (i in open until code.length) {
            depth += depthChange(code[i])
            if (depth == 0) return code.substring(open + 1, i)
        }
        return null
    }

    /**
     * Splits [text] at the commas that sit outside every bracket, so a parameter list becomes
     * one piece per parameter even when a lambda type or value holds commas of its own. Blank
     * pieces (left by a trailing comma) are dropped.
     */
    private fun topLevelParts(text: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        text.forEachIndexed { i, c ->
            depth += depthChange(c)
            if (c == ',' && depth == 0) {
                parts += text.substring(start, i)
                start = i + 1
            }
        }
        parts += text.substring(start)
        return parts.map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * Returns the type written in one [parameter], such as `HomeUiState` for `state: HomeUiState`
     * or `Modifier` for `modifier: Modifier = Modifier`, with its spaces removed; null when it has
     * no type. The type runs from the first colon outside brackets to the first `=` outside
     * brackets, where a default value starts, so the colons inside a lambda's type are skipped.
     */
    private fun parameterTypeOf(parameter: String): String? {
        var depth = 0
        var colon = -1
        parameter.forEachIndexed { i, c ->
            depth += depthChange(c)
            if (depth == 0 && colon < 0 && c == ':') colon = i
            if (depth == 0 && colon >= 0 && c == '=') {
                return parameter.substring(colon + 1, i).filterNot { it.isWhitespace() }
            }
        }
        return if (colon < 0) null else parameter.substring(colon + 1).filterNot { it.isWhitespace() }
    }

    /**
     * Returns one line per `var` and per `Mutable…` or `mutable…` word in the code of [source]
     * (comments and texts left out), as `fileName:line ...`. The lower-case form catches a
     * builder such as `mutableListOf()`, which makes a mutable value without naming its type.
     */
    private fun findMutableDeclarations(fileName: String, source: String): List<String> =
        codeOnly(source).lines().flatMapIndexed { index, line ->
            val where = "$fileName:${index + 1}"
            VAR.findAll(line).map { "$where declares a var" }.toList() +
                MUTABLE.findAll(line).map { "$where uses ${it.value}" }.toList()
        }

    /**
     * Returns [source] with every comment, and every string and character literal, replaced by
     * spaces, keeping the line breaks where they were. What is left is the code alone, so a word
     * or a bracket in a KDoc comment or in a text is never taken for code, and line numbers still
     * match the file.
     */
    private fun codeOnly(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        while (i < source.length) {
            // Where the comment or literal starting at i ends, or -1 when i is plain code.
            val end = when {
                source.startsWith("//", i) -> source.indexOf('\n', i).let { if (it < 0) source.length else it }
                source.startsWith("/*", i) -> blockCommentEnd(source, i)
                source[i] == QUOTE -> stringEnd(source, i)
                source[i] == '\'' -> charLiteralEnd(source, i)
                else -> -1
            }
            if (end < 0) {
                out.append(source[i])
                i++
            } else {
                for (j in i until end) {
                    out.append(if (source[j] == '\n' || source[j] == '\r') source[j] else ' ')
                }
                i = end
            }
        }
        return out.toString()
    }

    /**
     * Returns the index just past the block comment that starts at [start]. Kotlin lets block
     * comments nest, so each inner opening needs its own closing.
     */
    private fun blockCommentEnd(source: String, start: Int): Int {
        var depth = 0
        var i = start
        while (i < source.length) {
            when {
                source.startsWith("/*", i) -> {
                    depth++
                    i += 2
                }
                source.startsWith("*/", i) -> {
                    depth--
                    i += 2
                    if (depth == 0) return i
                }
                else -> i++
            }
        }
        return source.length
    }

    /**
     * Returns the index just past the string literal whose opening quote is at [start]: a normal
     * string, or a raw string between three quotes. It steps over escapes in a normal string and
     * over the code part of a template, which may hold strings of its own.
     */
    private fun stringEnd(source: String, start: Int): Int {
        val raw = source.startsWith(RAW_QUOTES, start)
        var i = start + (if (raw) 3 else 1)
        while (i < source.length) {
            val c = source[i]
            when {
                // An escape such as \n: the character after the backslash belongs to it.
                !raw && c == '\\' -> i += 2
                c == '$' && source.getOrNull(i + 1) == CURLY_OPEN -> i = templateEnd(source, i + 2)
                raw && source.startsWith(RAW_QUOTES, i) -> {
                    i += 3
                    // Quotes right before the closing three are part of the text.
                    while (i < source.length && source[i] == QUOTE) i++
                    return i
                }
                !raw && c == QUOTE -> return i + 1
                // A normal string cannot span lines; stop rather than swallow the file.
                !raw && c == '\n' -> return i
                else -> i++
            }
        }
        return source.length
    }

    /**
     * Returns the index just past the curly bracket that closes the code part of a template,
     * whose first character is at [start]. Brackets inside it are counted, and strings and
     * characters inside it are stepped over whole.
     */
    private fun templateEnd(source: String, start: Int): Int {
        var depth = 1
        var i = start
        while (i < source.length) {
            when (source[i]) {
                QUOTE -> i = stringEnd(source, i)
                '\'' -> i = charLiteralEnd(source, i)
                CURLY_OPEN -> {
                    depth++
                    i++
                }
                CURLY_CLOSE -> {
                    depth--
                    i++
                    if (depth == 0) return i
                }
                else -> i++
            }
        }
        return source.length
    }

    /**
     * Returns the index just past the character literal that starts at [start], such as `'a'`,
     * or one with a backslash escape inside it. A character literal never spans lines, so when
     * no closing apostrophe comes before the line ends, only the apostrophe itself is skipped.
     */
    private fun charLiteralEnd(source: String, start: Int): Int {
        val escaped = source.getOrNull(start + 1) == '\\'
        val close = source.indexOf('\'', startIndex = start + (if (escaped) 3 else 2))
        val lineEnd = source.indexOf('\n', startIndex = start)
        val closesOnThisLine = close >= 0 && (lineEnd < 0 || close < lineEnd)
        return if (closesOnThisLine) close + 1 else start + 1
    }

    /**
     * Tells how [c] changes the bracket depth: 1 for an opening round, square or curly bracket,
     * -1 for a closing one, 0 for any other character.
     */
    private fun depthChange(c: Char): Int {
        val at = BRACKET_PAIRS.indexOf(c)
        return when {
            at < 0 -> 0
            at % 2 == 0 -> 1
            else -> -1
        }
    }

    /** Returns the 1-based line of [text] that the character at [index] sits on. */
    private fun lineOf(text: String, index: Int): Int = text.take(index).count { it == '\n' } + 1

    /** The UiState the sample ViewModels below give out: one read-only value, as a UiState is. */
    private data class SampleUiState(val label: String)

    /**
     * A sample ViewModel that keeps the rule, the way ToolViewModel does: one StateFlow of its
     * UiState, beside a one-shot Flow of events and plain read-only values. It is never built;
     * the checker only reads its compiled methods.
     */
    private class GoodViewModel {

        /** The writable state behind [state]; private, so only this class can change it. */
        private val mutableState = MutableStateFlow(SampleUiState("good"))

        /** The one StateFlow a screen reads. */
        val state: StateFlow<SampleUiState> = mutableState.asStateFlow()

        /** Events delivered once each: a Flow that is not a StateFlow, which the rule allows. */
        val events: Flow<String> = emptyFlow()

        /** A plain read-only value, which the rule allows as well. */
        val title: String = "good"

        /**
         * Puts the first snapshot back. It is an object of another class reaching the private
         * [mutableState], so Kotlin adds a hidden accessor for it to this class, which the
         * checker must not count as a second StateFlow. (A lambda would not need one.)
         */
        val reset: Runnable = object : Runnable {
            /** Replaces the current snapshot with the first one. */
            override fun run() {
                mutableState.value = SampleUiState("good")
            }
        }
    }

    /** A sample ViewModel that breaks the rule by giving out a second StateFlow beside its state. */
    private class TwoStatesViewModel {

        /** The screen's state, as the rule wants it. */
        val state: StateFlow<SampleUiState> = MutableStateFlow(SampleUiState("two"))

        /** A second StateFlow, which belongs in the UiState instead. */
        val busy: StateFlow<Boolean> = MutableStateFlow(false)
    }

    /** A sample ViewModel that breaks the rule: its one StateFlow holds a String, not its UiState. */
    private class WrongTypeViewModel {

        /** A StateFlow of the wrong type. */
        val state: StateFlow<String> = MutableStateFlow("wrong")
    }

    /** A sample ViewModel that breaks the rule by giving out its writable MutableStateFlow. */
    private class WritableStateViewModel {

        /** The state, which anyone who reads it could also overwrite. */
        val state = MutableStateFlow(SampleUiState("writable"))
    }

    /** A sample base class that already gives out a StateFlow, which every class extending it inherits. */
    private open class BusyBaseViewModel {

        /** A StateFlow that sits in the base class, out of sight of a class that extends it. */
        val busy: StateFlow<Boolean> = MutableStateFlow(false)
    }

    /** A sample ViewModel that breaks the rule: its own StateFlow and the one it inherits make two. */
    private class InheritingViewModel : BusyBaseViewModel() {

        /** The screen's state, as the rule wants it. */
        val state: StateFlow<SampleUiState> = MutableStateFlow(SampleUiState("inheriting"))
    }

    /** Names, patterns and characters the checks use. */
    private companion object {
        /** The package that holds one sub-package per screen. */
        const val UI_PACKAGE = "com.phongkaster.kanso.ui"

        /** The six screens' ViewModels, as `folder/ClassName` under the `ui` package. */
        val EXPECTED_VIEW_MODELS = setOf(
            "home/HomeViewModel",
            "gallery/GalleryViewModel",
            "camera/CameraViewModel",
            "tool/ToolViewModel",
            "saved/SavedViewModel",
            "settings/SettingsViewModel",
        )

        /** How the names of Kotlin's hidden accessor methods begin: `access` and a dollar sign. */
        val ACCESSOR_PREFIX = "access${'$'}"

        /** How the names of Kotlin's default-argument helpers end: a dollar sign and `default`. */
        val DEFAULT_SUFFIX = "${'$'}default"

        /** Brackets in pairs, each opening one right before its closing one: round, square, curly. */
        const val BRACKET_PAIRS = "()[]{}"

        /**
         * A double quote. The text scanner above compares characters with these three names, so
         * its steps read as words (a quote, a curly bracket opening) and not as a row of quotes
         * and brackets.
         */
        const val QUOTE = '"'

        /** An opening curly bracket; see [QUOTE] for why it has a name. */
        const val CURLY_OPEN = '{'

        /** A closing curly bracket; see [QUOTE] for why it has a name. */
        const val CURLY_CLOSE = '}'

        /** Three double quotes, which open and close a raw string. */
        val RAW_QUOTES = QUOTE.toString().repeat(3)

        /** The keyword `var`, as a whole word (so `variants` does not match). */
        val VAR = Regex("""\bvar\b""")

        /** A word starting with Mutable or mutable: a Mutable type, or a builder that makes one. */
        val MUTABLE = Regex("""\b[Mm]utable\w*""")
    }
}
