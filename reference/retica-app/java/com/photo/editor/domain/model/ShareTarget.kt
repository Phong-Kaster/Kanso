package com.photo.editor.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.photo.editor.R

enum class ShareTarget(
    @DrawableRes val iconId: Int,
    @StringRes val titleId: Int,
    val packageId: String?,
) {
    Facebook(
        iconId = R.drawable.ic_share_facebook,
        titleId = R.string.facebook,
        packageId = "com.facebook.katana"
    ),
    Instagram(
        iconId = R.drawable.ic_share_instagram,
        titleId = R.string.instagram,
        packageId = "com.instagram.android"
    ),
    XTwitter(
        iconId = R.drawable.ic_share_x,
        titleId = R.string.x_twitter,
        packageId = "com.twitter.android"
    ),
    Threads(
        iconId = R.drawable.ic_share_threads,
        titleId = R.string.threads,
        packageId = "com.instagram.barcelona"
    ),
    More(
        iconId = R.drawable.ic_share_more,
        titleId = R.string.more,
        packageId = null,
    ),
}