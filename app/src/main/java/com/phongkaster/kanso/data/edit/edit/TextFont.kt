package com.phongkaster.kanso.data.edit.edit

import androidx.annotation.FontRes
import com.phongkaster.kanso.R

/**
 * The font families a text layer can be written in, in the order the Text sheet's font row shows
 * them. Every bundled font is free (SIL Open Font License, the licence texts are in
 * `assets/licenses/fonts/`), ships inside the app (Kanso has no network permission) and draws
 * every Vietnamese letter (ă, ơ, ư, ộ, ữ…), which was checked against each font's character map
 * before it was added.
 *
 * [res] is the font file in `res/font`, or null for [NONE], the phone's own default font.
 */
enum class TextFont(@FontRes val res: Int?) {

    /** No bundled font: the phone's default (Roboto on most phones, Samsung's on a Galaxy). */
    NONE(null),

    /** Be Vietnam Pro Bold: a clean, strong sans-serif. */
    BE_VIETNAM_PRO(R.font.be_vietnam_pro_bold),

    /** Playfair Display: an elegant high-contrast serif. */
    PLAYFAIR_DISPLAY(R.font.playfair_display_regular),

    /** Dancing Script Bold: a flowing script. */
    DANCING_SCRIPT(R.font.dancing_script),

    /** Patrick Hand: a friendly handwritten print. */
    PATRICK_HAND(R.font.patrick_hand_regular),

    /** Space Mono: a typewriter-like monospace. */
    SPACE_MONO(R.font.space_mono_regular),
}
