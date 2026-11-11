package com.phongkaster.kanso.ui.tool.model

import com.phongkaster.kanso.data.edit.edit.EditTool

/**
 * A group of Edit tools that share one entry on the Edit screen's top row. Tapping the entry swaps
 * the row for the group's [tools] and a Back entry, the way Beautify's Face and Body groups work.
 *
 * It is an enum so the row can list every group and a `when` over one must handle each case.
 */
enum class EditToolGroup(val tools: List<EditTool>) {

    /** Colour: the tools that change a photo's colour, RGB, HSL and Curves, in that order. */
    COLOUR(tools = listOf(EditTool.RGB, EditTool.HSL, EditTool.CURVES)),
}
