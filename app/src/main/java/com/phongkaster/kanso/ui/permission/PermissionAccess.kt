package com.phongkaster.kanso.ui.permission

/** How much a set of runtime permissions currently lets the app do. */
enum class PermissionAccess {
    /** Every permission needed for full access is granted. */
    Full,

    /** Only a partial-access permission is granted (Android 14+ "Select photos"). */
    Partial,

    /** Nothing useful is granted. */
    None,
}
