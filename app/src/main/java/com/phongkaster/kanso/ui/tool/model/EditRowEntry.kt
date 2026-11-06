package com.phongkaster.kanso.ui.tool.model

import com.phongkaster.kanso.data.edit.edit.EditTool

/**
 * One entry of the Edit screen's top row: either a [Group] of tools, which opens its own row, or a
 * [Single] tool, which opens its panel at once.
 *
 * It is a sealed interface: a `when` over an entry must handle both cases, so the row cannot
 * forget one.
 */
sealed interface EditRowEntry {

    /** A group, such as Colour: tapping it shows the group's tools and a Back entry. */
    data class Group(val group: EditToolGroup) : EditRowEntry

    /** A tool of its own, such as Crop: tapping it opens its panel. */
    data class Single(val tool: EditTool) : EditRowEntry

    /** The top row's entries. */
    companion object {

        /**
         * The top row, in order: Crop, Rotate, Adjust, Colour (the group holding RGB, HSL and
         * Curves), Filters, Stickers, Text, Date.
         */
        val TOP_ROW: List<EditRowEntry> = listOf(
            Single(EditTool.CROP),
            Single(EditTool.ROTATE),
            Single(EditTool.ADJUST),
            Group(EditToolGroup.COLOUR),
            Single(EditTool.FILTERS),
            Single(EditTool.STICKERS),
            Single(EditTool.TEXT),
            Single(EditTool.DATE),
        )
    }
}
