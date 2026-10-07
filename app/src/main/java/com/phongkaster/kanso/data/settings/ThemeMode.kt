package com.phongkaster.kanso.data.settings

/**
 * Whether the app is drawn light or dark.
 *
 * [System] follows the phone's own dark-theme switch; [Light] and [Dark] force one look no matter
 * what the phone is set to. The names are saved to disk as text, so never rename an entry.
 */
enum class ThemeMode {
    /** Follow the phone's system-wide dark-theme setting. */
    System,

    /** Always use the light colour scheme. */
    Light,

    /** Always use the dark colour scheme. */
    Dark,
}
