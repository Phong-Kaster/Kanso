package com.phongkaster.kanso.tool.edit

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.EDIT_TOOL_LIST_TAG
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editToolTag

/**
 * Scrolls the Edit screen's row of tools until [tool]'s entry is on screen.
 *
 * The row is a `LazyRow`, which composes only the entries on screen: Text and Date, the last two of
 * the eight, sit past the right edge of a phone-sized window and have no node at all until the row
 * is scrolled to them.
 * `performScrollToNode` asks the lazy list itself to scroll (by item index) until a node matching
 * the tag exists and is shown, which `performScrollTo` on the entry cannot do for an entry that
 * does not exist yet.
 */
fun SemanticsNodeInteractionsProvider.scrollToolRowTo(tool: EditTool) {
    onNodeWithTag(EDIT_TOOL_LIST_TAG).performScrollToNode(hasTestTag(editToolTag(tool)))
}
