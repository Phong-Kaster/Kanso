package com.phongkaster.kanso.navigation

import android.Manifest
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.camera.FakeCameraController
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.camera.CAMERA_KEEP_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SCREEN_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.home.HOME_OPEN_PHOTO_TAG
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.home.HomeViewModel
import com.phongkaster.kanso.ui.home.homeToolTag
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Drives the real app (MainActivity, started on the JVM by Robolectric) through its navigation:
 * Home opens with the floating tab bar, Home selected, and lists "Open a photo" and the five tool
 * cards in their two groups; each card opens its tool's gallery and Back returns to Home; the
 * Settings tab opens Settings (no Back button there) and Back returns to Home; tabs never pile up
 * on the back stack; the round Camera button opens the camera from Home and from Settings, a kept
 * shot opens the Edit tool, and Back returns to the tab it started on; Back on Home closes the
 * app; rotation keeps the current screen; each screen's ViewModel lives exactly as long as its
 * back stack entry; and no permission is asked for before the screen that needs it.
 *
 * Settings and the camera are reached through the bar's tags (`TabTaps.openTab`,
 * `TabTaps.tapCameraButton`), never by their text: "Settings" is on screen twice on the Settings
 * tab (the title and the tab). Home is recognised by its tag, `home-screen`. Home's cards are
 * found by their tag (`homeToolTag`), never by their text alone (the "Edit" header and the Edit
 * card read the same word), and tapped with `tapAboveBar`, because on this 320 x 470 dp screen a
 * card scrolled into view can rest under the floating bar. The My edits tab's own walks are with
 * that screen's tests.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class NavigationTest {

    /** Lets the test find and check Compose nodes in whatever activity the test launches. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** The app context, used to read the same string resources the screens show. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** The application under test, used to grant permissions before a launch. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test. KansoApplication starts Koin for every test's fresh
     * Application, but Koin's global state outlives it inside the same JVM, and starting it
     * twice fails.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * DoD 1 (the whole-app part): the app opens on Home with the floating bar once, holding
     * exactly three tabs, Home, My edits and Settings, left to right with those names; Home is
     * selected and the other two are not; the round Camera button lies outside the bar, named
     * "Camera" and showing no text.
     */
    @Test
    fun home_opens_with_the_floating_bar_and_home_selected() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome()
            waitForBar()
            compose.onAllNodesWithTag(KANSO_TAB_BAR_TAG).assertCountEquals(1)

            val tabs = compose.onAllNodes(isTab and hasAnyAncestor(hasTestTag(KANSO_TAB_BAR_TAG)))
                .fetchSemanticsNodes()
                .sortedBy { it.positionInRoot.x }
            assertEquals(
                "The bar must hold the three tabs, left to right",
                KansoTab.entries.map { kansoTabTag(it) },
                tabs.map { it.config.getOrNull(SemanticsProperties.TestTag) },
            )
            KansoTab.entries.forEach { tab ->
                compose.onNodeWithTag(kansoTabTag(tab)).assert(hasText(context.getString(tab.labelRes)))
            }
            assertSelected(KansoTab.HOME)

            compose.onNodeWithTag(KANSO_CAMERA_BUTTON_TAG)
                .assert(hasContentDescription(context.getString(R.string.tool_camera)))
                .assert(!hasAnyAncestor(hasTestTag(KANSO_TAB_BAR_TAG)))
                .assertHasClickAction()
            val cameraText = compose.onNodeWithTag(KANSO_CAMERA_BUTTON_TAG).fetchSemanticsNode()
                .config.getOrNull(SemanticsProperties.Text)
            assertTrue("The Camera button must show no text, found $cameraText", cameraText.isNullOrEmpty())
        }
    }

    /**
     * DoD 5 of Run 6 as Home still stands, with R3 and the Home half of R4: inside Home's one
     * scrolling column there are the title "Kanso" exactly once, a clickable "Open a photo", the
     * headers "AI tools" and "Edit" and five clickable tool cards each showing its label, the four
     * AI tool cards between the two headers and the Edit card below the "Edit" header. Home has no
     * Camera shortcut of its own (no text "Camera" anywhere; the only node named "Camera" is the
     * bar's round button, outside the column) and no Settings row (the only clickable node reading
     * "Settings" is the Settings tab, inside the bar).
     */
    @Test
    fun home_lists_open_a_photo_and_the_tool_cards_while_settings_and_the_camera_are_on_the_bar() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome()
            waitForBar()
            compose.onAllNodesWithText("Kanso").assertCountEquals(1)
            compose.onNode(hasText("Kanso")).assert(insideTheColumn)

            val openPhoto = context.getString(R.string.home_open_photo)
            compose.onAllNodesWithText(openPhoto).assertCountEquals(1)
            compose.onNodeWithTag(HOME_OPEN_PHOTO_TAG)
                .assert(hasText(openPhoto))
                .assert(insideTheColumn)
                .assertHasClickAction()

            val camera = context.getString(R.string.tool_camera)
            compose.onAllNodesWithText(camera).assertCountEquals(0)
            compose.onAllNodes(hasContentDescription(camera)).assertCountEquals(1)
            compose.onNode(hasContentDescription(camera))
                .assert(hasTestTag(KANSO_CAMERA_BUTTON_TAG))
                .assert(!insideTheColumn)
            compose.onAllNodesWithTag(HOME_CARD_TAG_PREFIX + "CAMERA").assertCountEquals(0)

            val settings = context.getString(R.string.settings)
            val clickableSettings = hasText(settings) and hasClickAction()
            compose.onAllNodes(clickableSettings).assertCountEquals(1)
            compose.onNode(clickableSettings)
                .assert(hasTestTag(kansoTabTag(KansoTab.SETTINGS)))
                .assert(hasAnyAncestor(hasTestTag(KANSO_TAB_BAR_TAG)))
                .assert(!insideTheColumn)

            val aiHeader = header(R.string.home_header_ai_tools)
            val editHeader = header(R.string.home_header_edit)
            compose.onAllNodes(aiHeader).assertCountEquals(1)
            compose.onAllNodes(editHeader).assertCountEquals(1)
            compose.onNode(aiHeader).assert(insideTheColumn)
            compose.onNode(editHeader).assert(insideTheColumn)

            compose.onAllNodes(isToolCard).assertCountEquals(5)
            Tool.entries.forEach { tool ->
                compose.onNodeWithTag(homeToolTag(tool))
                    .assert(hasText(label(tool)))
                    .assert(insideTheColumn)
                    .assertHasClickAction()
            }

            // positionInRoot, not boundsInRoot: the column is taller than the screen, and
            // boundsInRoot is clipped to what is visible (a row scrolled out of view reads 0).
            val aiHeaderTop = top(aiHeader)
            val editHeaderTop = top(editHeader)
            assertTrue("The AI tools header must come before the Edit header", aiHeaderTop < editHeaderTop)
            AI_TOOLS.forEach { tool ->
                val cardTop = top(hasTestTag(homeToolTag(tool)))
                assertTrue("${tool.name} must lie below the AI tools header", cardTop > aiHeaderTop)
                assertTrue("${tool.name} must lie above the Edit header", cardTop < editHeaderTop)
            }
            assertTrue(
                "The Edit card must lie below the Edit header",
                top(hasTestTag(homeToolTag(Tool.EDIT))) > editHeaderTop,
            )
        }
    }

    /**
     * DoD 1 and 5: each of the five tool cards opens the photo gallery titled
     * "<tool>: choose a photo", with no tab bar on it, and Back returns to Home each time.
     */
    @Test
    fun each_tool_opens_its_screen_and_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            Tool.entries.forEach { tool ->
                tapCard(tool)
                waitForGallery()
                compose.onNodeWithTag(GALLERY_SCREEN_TAG).assertIsDisplayed()
                // The gallery's title names the tool, so a card opening another tool's gallery
                // would fail here.
                compose.onNode(
                    hasText(galleryTitle(tool)) and hasAnyAncestor(hasTestTag(GALLERY_SCREEN_TAG)),
                ).assertIsDisplayed()
                waitForNoBar()

                Espresso.pressBack()
                waitForHomeOnly()
                assertEquals(Lifecycle.State.RESUMED, scenario.state)
                scenario.onActivity { activity -> assertFalse(activity.isFinishing) }
            }
        }
    }

    /**
     * DoD 15 (and DoD 17 of an earlier run): launching the app asks for no permission; the first
     * tap on a tool card (Edit) asks for the two photo permissions Android 15 uses, and nothing
     * else.
     */
    @Test
    fun launch_asks_for_nothing_and_the_gallery_asks_for_photo_access() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            scenario.onActivity { assertNull(shadowOf(it).lastRequestedPermission) }

            tapCard(Tool.EDIT)
            waitForGallery()

            assertLastRequest(scenario, PHOTO_PERMISSIONS)
        }
    }

    /**
     * DoD 15: launching asks for nothing; tapping "Open a photo" asks for the two photo
     * permissions, and nothing else.
     */
    @Test
    fun open_a_photo_asks_only_for_photo_access() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            scenario.onActivity { assertNull(shadowOf(it).lastRequestedPermission) }

            compose.onNodeWithTag(HOME_OPEN_PHOTO_TAG).tapAboveBar()
            waitForGallery()

            assertLastRequest(scenario, PHOTO_PERMISSIONS)
            // Robolectric keeps only the latest request, so "only" also rests on this: the camera
            // screen, the one place that asks for CAMERA, was never opened.
            assertEquals(0, count(CAMERA_SCREEN_TAG))
        }
    }

    /**
     * DoD 3 and 21: launching asks for nothing; on Home the round Camera button opens the camera,
     * which asks for the camera permission only, with no tab bar on it; Back returns to Home,
     * still selected. The camera is swapped for a fake, because Robolectric cannot run CameraX.
     */
    @Test
    fun from_home_the_camera_button_asks_only_for_camera_and_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Koin lets a later module replace an earlier recipe with the same type.
            loadKoinModules(module { factory<CameraController> { FakeCameraController() } })
            waitForHome()
            scenario.onActivity { assertNull(shadowOf(it).lastRequestedPermission) }

            tapCameraButton(compose)
            waitFor { count(CAMERA_SCREEN_TAG) > 0 }
            waitForNoBar()

            assertLastRequest(scenario, listOf(Manifest.permission.CAMERA))
            // Robolectric keeps only the latest request, so "only" also rests on this: the
            // gallery, the one place that asks for photos, was never opened.
            assertEquals(0, count(GALLERY_SCREEN_TAG))

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(CAMERA_SCREEN_TAG) == 0 && count(HOME_SCREEN_TAG) > 0 }
            waitForBar()
            assertSelected(KansoTab.HOME)
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }

    /**
     * DoD 3 and 21: the Settings tab asks for nothing; on Settings the round Camera button opens
     * the camera, which asks for the camera permission only; Back returns to Settings, still
     * selected.
     */
    @Test
    fun from_settings_the_camera_button_asks_only_for_camera_and_back_returns_to_settings() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadKoinModules(module { factory<CameraController> { FakeCameraController() } })
            waitForHome()
            openSettings()
            scenario.onActivity { assertNull(shadowOf(it).lastRequestedPermission) }

            tapCameraButton(compose)
            waitFor { count(CAMERA_SCREEN_TAG) > 0 }
            waitForNoBar()
            assertLastRequest(scenario, listOf(Manifest.permission.CAMERA))
            assertEquals(0, count(GALLERY_SCREEN_TAG))

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(CAMERA_SCREEN_TAG) == 0 && count(SETTINGS_SCREEN_TAG) > 0 }
            waitForBar()
            assertSelected(KansoTab.SETTINGS)
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }

    /**
     * DoD 3: with camera access granted, the Camera button on Home opens the camera; the shutter
     * and then Keep open the Edit tool on the kept shot, with no gallery in between and no tab
     * bar; one Back from the tool returns to Home, selected.
     */
    @Test
    fun from_home_keep_opens_the_edit_tool_and_one_back_returns_home() {
        keepFromTab(KansoTab.HOME, HOME_SCREEN_TAG)
    }

    /**
     * DoD 3: the same from Settings: Camera, shutter, Keep open the Edit tool on the kept shot,
     * and one Back from the tool returns to Settings, selected.
     */
    @Test
    fun from_settings_keep_opens_the_edit_tool_and_one_back_returns_to_settings() {
        keepFromTab(KansoTab.SETTINGS, SETTINGS_SCREEN_TAG)
    }

    /**
     * DoD 3: two taps on the Camera button faster than a frame open one camera: one Back leaves
     * it and shows Home again. The two taps call the button's click action twice in a row before
     * the screen can redraw, as a quick double tap does.
     */
    @Test
    fun a_double_tap_on_the_camera_button_opens_one_camera() {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadKoinModules(module { factory<CameraController> { FakeCameraController() } })
            waitForHome()
            waitForBar()
            val click = compose.onNodeWithTag(KANSO_CAMERA_BUTTON_TAG).fetchSemanticsNode()
                .config[SemanticsActions.OnClick].action
            compose.runOnUiThread {
                click?.invoke()
                click?.invoke()
            }
            waitFor { count(CAMERA_SCREEN_TAG) > 0 }

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(CAMERA_SCREEN_TAG) == 0 && count(HOME_SCREEN_TAG) > 0 }
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertIsDisplayed()
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }

    /**
     * DoD 1, 2, 15 and R3: the Settings tab opens Settings, which has a "Settings" heading and no
     * Back button, with the bar on it once and the Settings tab selected; Back (Espresso's)
     * returns to Home, selected.
     */
    @Test
    fun the_settings_tab_opens_settings_without_a_back_button_and_espresso_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            openSettings()
            compose.onNode(
                hasText(context.getString(R.string.settings)) and
                    hasAnyAncestor(hasTestTag(SETTINGS_SCREEN_TAG)) and
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
            ).assertIsDisplayed()
            compose.onAllNodes(
                hasContentDescription(context.getString(R.string.back)) and
                    hasAnyAncestor(hasTestTag(SETTINGS_SCREEN_TAG)),
            ).assertCountEquals(0)
            compose.onAllNodesWithTag(KANSO_TAB_BAR_TAG).assertCountEquals(1)
            assertSelected(KansoTab.SETTINGS)

            Espresso.pressBack()
            waitForHomeOnly()
            assertSelected(KansoTab.HOME)
            scenario.onActivity { assertFalse(it.isFinishing) }
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    /** DoD 2: Home, then Settings, then Back sent through the activity's back dispatcher: Home. */
    @Test
    fun the_settings_tab_then_dispatcher_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            openSettings()

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitForHomeOnly()
            assertSelected(KansoTab.HOME)
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }

    /**
     * DoD 2: moving between the tabs never piles them up. Home, Settings, Home, Settings, then
     * Back (Espresso's) shows Home, selected, and one more Back finishes the activity.
     */
    @Test
    fun tabs_never_pile_up_and_espresso_back_from_a_tab_goes_home_then_closes() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            openSettings()
            openTab(compose, KansoTab.HOME)
            waitForHomeOnly()
            openSettings()

            Espresso.pressBack()
            waitForHomeOnly()
            assertSelected(KansoTab.HOME)

            assertBackClosesTheApp(scenario)
        }
    }

    /**
     * DoD 2: the same walk with every Back sent through the activity's back dispatcher: one Back
     * from Settings shows Home, selected, and one more finishes the activity.
     */
    @Test
    fun tabs_never_pile_up_and_dispatcher_back_from_a_tab_goes_home_then_closes() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            openSettings()
            openTab(compose, KansoTab.HOME)
            waitForHomeOnly()
            openSettings()

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitForHomeOnly()
            assertSelected(KansoTab.HOME)

            assertBackClosesTheApp(scenario)
        }
    }

    /**
     * DoD 1: tapping the Home tab while Home is shown changes nothing: Home stays, selected, and
     * a single Back still closes the app (nothing was added to the back stack).
     */
    @Test
    fun tapping_the_selected_home_tab_changes_nothing() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            openTab(compose, KansoTab.HOME)
            waitForHomeOnly()
            assertSelected(KansoTab.HOME)
            assertBackClosesTheApp(scenario)
        }
    }

    /** DoD 7: Back on Home closes the app: the activity is destroyed. */
    @Test
    fun back_on_home_closes_the_app() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            assertBackClosesTheApp(scenario)
        }
    }

    /** DoD 8: rotating (recreating the activity) on the Edit gallery keeps it on screen. */
    @Test
    fun gallery_survives_recreation_and_one_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            tapCard(Tool.EDIT)
            waitForGallery()

            scenario.recreate()
            waitForGallery()
            compose.onNodeWithTag(GALLERY_SCREEN_TAG).assertIsDisplayed()

            Espresso.pressBack()
            waitForHomeOnly()
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    /**
     * DoD 10: a gallery's ViewModel is cleared when the gallery is popped, while Home's
     * ViewModel, whose entry stays on the back stack, is kept.
     */
    @Test
    fun popping_a_gallery_clears_its_view_model_but_not_homes() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome()
            val probe = KoinPlatform.getKoin().get<ViewModelClearedProbe>()

            tapCard(Tool.EDIT)
            waitForGallery()
            assertFalse(probe.wasCleared(GalleryViewModel::class))

            Espresso.pressBack()
            waitForHomeOnly()
            compose.waitUntil(timeoutMillis = 10_000) {
                probe.wasCleared(GalleryViewModel::class)
            }
            assertTrue(probe.wasCleared(GalleryViewModel::class))
            assertFalse(probe.wasCleared(HomeViewModel::class))
        }
    }

    /**
     * Grants camera access, starts the app, opens [tab] (whose screen is tagged [screenTag]), taps
     * the Camera button, takes a shot and keeps it, then checks the Edit tool shows the kept shot
     * with no camera, gallery or tab bar left, and that one Back returns to [tab], selected.
     */
    private fun keepFromTab(tab: KansoTab, screenTag: String) {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        val fake = FakeCameraController()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadKoinModules(
                module {
                    factory<CameraController> { fake }
                    single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                },
            )
            waitForHome()
            if (tab != KansoTab.HOME) {
                openTab(compose, tab)
                waitFor { count(screenTag) > 0 }
            }
            tapCameraButton(compose)
            waitFor {
                compose.onAllNodes(hasTestTag(CAMERA_SHUTTER_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            waitForNoBar()
            compose.onNodeWithTag(CAMERA_SHUTTER_TAG).performClick()
            waitFor { count(CAMERA_KEEP_TAG) > 0 }
            val shot = fake.takenFiles.single()
            compose.onNodeWithTag(CAMERA_KEEP_TAG).performClick()
            // The photo is shown once the editor has decoded the shot.
            waitFor { count(CAMERA_SCREEN_TAG) == 0 && count(TOOL_IMAGE_TAG) > 0 }

            assertEquals(1, count(TOOL_SCREEN_TAG))
            assertEquals(0, count(GALLERY_SCREEN_TAG))
            compose.onNode(
                hasTestTag(TOOL_IMAGE_TAG) and
                    SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, Uri.fromFile(shot).toString()),
            ).assertExists()
            // The Edit tool's title is shown on the tool screen.
            compose.onNode(
                hasText(context.getString(R.string.tool_edit)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)),
            ).assertExists()
            waitForNoBar()

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(TOOL_SCREEN_TAG) == 0 && count(screenTag) > 0 }
            assertEquals(0, count(GALLERY_SCREEN_TAG))
            assertEquals(0, count(CAMERA_SCREEN_TAG))
            waitForBar()
            assertSelected(tab)
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }

    /** Sends Back through the activity's back dispatcher and fails unless the activity finishes. */
    private fun assertBackClosesTheApp(scenario: ActivityScenario<MainActivity>) {
        var finishing = false
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        // finish() marks the activity as finishing at once; the teardown follows later.
        scenario.onActivity { finishing = it.isFinishing }
        assertTrue("Back on Home should finish the activity", finishing)
    }

    /** Fails unless [tab] is the one selected tab of the bar and the other two are not selected. */
    private fun assertSelected(tab: KansoTab) {
        KansoTab.entries.forEach { other ->
            val node = compose.onNodeWithTag(kansoTabTag(other))
            if (other == tab) node.assertIsSelected() else node.assertIsNotSelected()
        }
    }

    /** Taps the Settings tab and waits until the Settings screen is shown. */
    private fun openSettings() {
        openTab(compose, KansoTab.SETTINGS)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Returns the label Home shows for [tool]. */
    private fun label(tool: Tool): String = context.getString(tool.labelRes)

    /** Returns the title of [tool]'s gallery, such as "Retouch: choose a photo". */
    private fun galleryTitle(tool: Tool): String = context.getString(R.string.gallery_title, label(tool))

    /**
     * Matches the group header reading the string [textRes]: a text marked as a heading, which
     * the Edit card (same word, but a button) is not.
     */
    private fun header(textRes: Int): SemanticsMatcher =
        hasText(context.getString(textRes)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    /** Returns the top of the one node matching [matcher], where it is laid out, in pixels. */
    private fun top(matcher: SemanticsMatcher): Float = compose.onNode(matcher).fetchSemanticsNode().positionInRoot.y

    /** Scrolls Home to [tool]'s card, found by its tag, and taps it above the floating bar. */
    private fun tapCard(tool: Tool) {
        compose.onNodeWithTag(homeToolTag(tool)).tapAboveBar()
    }

    /**
     * Fails unless the last permission request Robolectric recorded for [scenario]'s activity
     * asked for exactly [permissions], in that order.
     */
    private fun assertLastRequest(scenario: ActivityScenario<MainActivity>, permissions: List<String>) {
        scenario.onActivity { activity ->
            val request = shadowOf(activity).lastRequestedPermission
            assertEquals(permissions, request?.requestedPermissions?.toList())
        }
    }

    /** Waits until the gallery screen is on screen. */
    private fun waitForGallery() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(GALLERY_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home is on screen, found by its own tag. */
    private fun waitForHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until the floating bar is on screen. */
    private fun waitForBar() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(KANSO_TAB_BAR_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Waits until neither the floating bar nor the Camera button is left on screen: once the
     * screen on top is not a tab, the bar plays its exit and is then removed.
     */
    private fun waitForNoBar() {
        waitFor { count(KANSO_TAB_BAR_TAG) == 0 && count(KANSO_CAMERA_BUTTON_TAG) == 0 }
    }

    /**
     * Waits until Home is shown and no gallery or Settings screen is left on screen. Home is
     * checked by its own tag, not by its title: Home keeps its scroll position, so after a test
     * scrolled down to a card and came back the title can be scrolled out of view while Home is
     * on screen.
     */
    private fun waitForHomeOnly() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag(GALLERY_SCREEN_TAG).fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag(HOME_SCREEN_TAG).assertIsDisplayed()
    }

    /**
     * Waits up to ten seconds of real time for [condition], letting the main thread run in
     * between: the camera and the editor finish their work on the main thread, and under
     * Robolectric the test runs on that thread itself, so its queue only moves when it is run.
     */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** Values and matchers the tests share. */
    private companion object {
        /** The four tools of the "AI tools" group, in the order Home lists them (assumption A-001). */
        val AI_TOOLS = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC)

        /** The two photo permissions a gallery asks for on Android 15. */
        val PHOTO_PERMISSIONS = listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        /** The start of every Home card's tag, `home-tool-`, taken from `homeToolTag` itself. */
        val HOME_CARD_TAG_PREFIX = homeToolTag(Tool.EDIT).removeSuffix(Tool.EDIT.name)

        /** Matches every tool card on Home, whatever its tool. */
        val isToolCard = SemanticsMatcher("is a Home tool card") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(HOME_CARD_TAG_PREFIX) == true
        }

        /** Matches a node with the Tab role, as each item of the floating bar is. */
        val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

        /** Matches a node inside a scrolling container: on Home, its one scrolling column. */
        val insideTheColumn = hasAnyAncestor(hasScrollAction())
    }
}
