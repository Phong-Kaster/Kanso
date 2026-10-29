package com.phongkaster.kanso.ui.tool.model

/**
 * A group of Beautify tools, shown as one entry in the panel's top row (layer 1); tapping it
 * replaces the row with the group's own tools (layer 2) and a Back entry, as Meitu's Beautify does
 * (Face > Width, Lift, ...).
 *
 * A new face tool joins [FACE] by naming it as its group in [BeautifyTool]; the panel needs no
 * other change. A group with no tool yet is simply not listed here, so the panel never shows an
 * entry that does nothing.
 */
enum class BeautifyGroup {
    /** Reshaping the face: Width and Lift now, Temple later. */
    FACE,

    /** Reshaping the body, as Meitu's Body > Reshape: Waist and Arms now; Chest and Legs one at a time. */
    BODY,
    ;

    /** The group's tools, in the order the row shows them. */
    val tools: List<BeautifyTool>
        get() = BeautifyTool.entries.filter(predicate = { it.group == this })
}
