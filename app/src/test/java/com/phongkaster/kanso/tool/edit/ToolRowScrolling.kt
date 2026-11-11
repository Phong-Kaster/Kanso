package com.phongkaster.kanso.tool.edit

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.EDIT_GROUP_BACK_TAG
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.EDIT_TOOL_LIST_TAG
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editGroupTag
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editToolTag
import com.phongkaster.kanso.ui.tool.model.EditToolGroup

/**
 * Brings [tool]'s entry on screen in the Edit screen's row of tools, changing the row's layer when
 * the tool lives on the other one.
 *
 * RGB, HSL and Curves are not in the first row any more: they sit in the Colour group's row. So
 * for one of them, when the first row is showing (the Colour entry exists), this first taps
 * Colour. For any other tool, when the Colour group's row is showing (its Back entry exists, and
 * no other tool's entry can be), it taps Back, which also closes an open colour sheet. Then it
 * scrolls as before.
 *
 * The row is a `LazyRow`, which composes only the entries on screen: the last entries of
 * the first row (Stickers, Text and Date, on a narrow window Filters too) sit past the right
 * edge of a phone-sized window and have no node at all until the row is scrolled to them.
 * `performScrollToNode` asks the lazy list itself to scroll (by item index) until a node matching
 * the tag exists and is shown, which `performScrollTo` on the entry cannot do for an entry that
 * does not exist yet.
 */
fun SemanticsNodeInteractionsProvider.scrollToolRowTo(tool: EditTool) {
    if (tool in EditToolGroup.COLOUR.tools) {
        val colour = onAllNodesWithTag(editGroupTag(EditToolGroup.COLOUR))
        if (colour.fetchSemanticsNodes().isNotEmpty()) colour[0].performClick()
    } else {
        val back = onAllNodesWithTag(EDIT_GROUP_BACK_TAG)
        if (back.fetchSemanticsNodes().isNotEmpty()) back[0].performClick()
    }
    onNodeWithTag(EDIT_TOOL_LIST_TAG).performScrollToNode(hasTestTag(editToolTag(tool)))
}
