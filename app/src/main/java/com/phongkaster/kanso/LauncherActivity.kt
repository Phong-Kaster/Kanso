package com.phongkaster.kanso

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * The activity every launcher icon opens: it starts [MainActivity] and closes itself at once.
 *
 * Why it exists: the launcher icon's colour follows the theme colour, so the launcher entries
 * (one `activity-alias` per colour, see LauncherIconSwitcher) are switched on and off while the
 * app runs. Android closes every open screen that was started through an entry being switched
 * off. If the entries opened MainActivity directly, changing the theme colour would close the
 * app under the person's finger. Android also removes a whole task when the entry that started
 * the task is switched off, so MainActivity runs in a task of its own (its own `taskAffinity` in
 * the manifest), started here with NEW_TASK under its own name, which is never switched off.
 * This activity lives in a throwaway task that may disappear without anyone noticing.
 *
 * CLEAR_TOP with SINGLE_TOP brings back the MainActivity already running, with its screens as
 * they were, instead of stacking a second one on top of it.
 */
class LauncherActivity : Activity() {

    /** Starts MainActivity and finishes, so this activity never shows a frame of its own. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
        )
        finish()
    }
}
