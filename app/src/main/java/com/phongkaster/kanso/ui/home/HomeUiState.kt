package com.phongkaster.kanso.ui.home

import com.phongkaster.kanso.navigation.Tool

/**
 * Everything the Home screen shows that can differ from one snapshot to the next, in one value:
 * the tools of its two ways into a tool, each list in display order.
 *
 * - [carouselTools] are the banners of the hero carousel, left to right: Remove background,
 *   Enhance, Retouch and Artistic, the four AI tools. A banner shows the tool large, with a line
 *   about it and a "Try now" button, so only the tools that need introducing get one.
 * - [shortcutTools] are the round tool shortcuts in the row under the carousel, left to right:
 *   Edit, Remove background, Enhance, Retouch and Artistic, every tool once. It equals
 *   `Tool.entries`, the order the [Tool] enum declares them in, so every tool is one tap from Home
 *   without swiping the carousel.
 *
 * The rest of Home is always the same: the title "Kanso" and the greeting under it are drawn on
 * every Home, so they need no field here. The camera is not a tool: it takes a new photo instead
 * of working on one, so it is in neither list; it is the round button beside the floating tab bar,
 * which also leads to My edits and Settings, and none of those is part of Home's own state.
 *
 * What a UiState is. This is the first UiState a reader of Kanso meets, and every screen of the
 * app has one (`GalleryUiState`, `SettingsUiState` and the others point back here), so the idea
 * is explained once, here.
 *
 * - A UiState is one snapshot of everything a screen shows at one moment: for Home, the carousel's
 *   tools and the shortcuts' tools. It is a data class whose fields are all read-only `val`s, so
 *   once a snapshot is made it never changes; nobody can edit it in place.
 * - The screen's ViewModel produces it. The ViewModel keeps the current snapshot in a StateFlow (a
 *   holder that always has one current value and tells its readers each time a new value arrives)
 *   and hands the screen exactly that one StateFlow, always named `state`, to read.
 * - The screen never changes the snapshot it is given. When the person taps something, the screen
 *   reports it through a callback (for Home, `onToolClick`). If the tap should change what is
 *   shown, the ViewModel builds a new snapshot (a data class's `copy(...)` makes one with a few
 *   fields changed) and puts it into its StateFlow, where it replaces the old one. So data only
 *   travels one way: state goes down to the screen, taps go up from it. This is called
 *   "unidirectional data flow".
 * - The screen function reads the StateFlow with `collectAsState()`, which turns it into Compose
 *   state: whenever a new snapshot replaces the old one, the screen is drawn again with it.
 * - A UiState is a plain value with nothing of the running app inside it, so a preview can build
 *   one from sample data (`HomePreview` uses a `HomeUiState` holding the same two lists the real
 *   Home shows) and draw the screen exactly as the person would see it, with no ViewModel, Koin
 *   or phone data at all.
 *
 * Which banner the carousel shows is not in here: it is the carousel's own scroll position, kept
 * by the carousel itself (see `HomeContent`), the same way a list keeps how far it is scrolled.
 *
 * On Home a tap only opens another screen, so Home's snapshot never changes. Home still has a
 * ViewModel and a UiState all the same, so that every screen of the app reads the same way.
 */
data class HomeUiState(val carouselTools: List<Tool>, val shortcutTools: List<Tool>)
