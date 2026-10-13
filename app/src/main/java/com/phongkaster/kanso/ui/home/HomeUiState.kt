package com.phongkaster.kanso.ui.home

import com.phongkaster.kanso.navigation.Tool

/**
 * Everything the Home screen shows that can differ from one snapshot to the next, in one value:
 * the tools of its two groups of cards, each list in display order. [aiTools] are the cards under
 * the "AI tools" header (Remove background, Enhance, Retouch and Artistic) and [editTools] the
 * cards under the "Edit" header (Edit).
 *
 * The rest of Home is always the same: the "Open a photo" button is drawn on every Home, so it
 * needs no field here. The camera is not a tool: it takes a new photo instead of working on one,
 * so it has no place in either list; it is the round button beside the floating tab bar, which
 * also leads to Settings, and neither is part of Home's own state.
 *
 * What a UiState is. This is the first UiState a reader of Kanso meets, and every screen of the
 * app has one (`GalleryUiState`, `SettingsUiState` and the others point back here), so the idea
 * is explained once, here.
 *
 * - A UiState is one snapshot of everything a screen shows at one moment: for Home, the two
 *   groups of tools. It is a data class whose fields are all read-only `val`s, so once a snapshot
 *   is made it never changes; nobody can edit it in place.
 * - The screen's ViewModel produces it. The ViewModel keeps the current snapshot in a StateFlow (a
 *   holder that always has one current value and tells its readers each time a new value arrives)
 *   and hands the screen exactly that one StateFlow, always named `state`, to read.
 * - The screen never changes the snapshot it is given. When the person taps something, the screen
 *   reports it through a callback (for Home, `onOpenPhoto` and `onToolClick`). If the tap
 *   should change what is shown, the ViewModel builds a new
 *   snapshot (a data class's `copy(...)` makes one with a few fields changed) and puts it into its
 *   StateFlow, where it replaces the old one. So data only travels one way: state goes down to
 *   the screen, taps go up from it. This is called "unidirectional data flow".
 * - The screen function reads the StateFlow with `collectAsState()`, which turns it into Compose
 *   state: whenever a new snapshot replaces the old one, the screen is drawn again with it.
 * - A UiState is a plain value with nothing of the running app inside it, so a preview can build
 *   one from sample data (`HomePreview` uses a `HomeUiState` holding the same two groups the real
 *   Home shows) and draw the screen exactly as the person would see it, with no ViewModel, Koin
 *   or phone data at all.
 *
 * On Home a tap only opens another screen, so Home's snapshot never changes. Home still has a
 * ViewModel and a UiState all the same, so that every screen of the app reads the same way.
 */
data class HomeUiState(val aiTools: List<Tool>, val editTools: List<Tool>)
