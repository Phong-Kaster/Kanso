@file:Suppress("PropertyName")

package com.photo.editor.data.remoteconfig

import com.photo.editor.data.repository.RemoteConfigRepository

data class CommonConfig(
    // Enable or disable fast reload banner
    val fast_reload_banner: Boolean = true,
    // Amount of seconds between banner reloads
    val fast_reload_banner_period: Int = 15,
    // Should collapsible banner be showed interleave with normal banner
    val collap_banner_interleave: Int = 0,
    val force_update: Boolean = false,
    val disable_ads: Boolean = false,
    val inter_item_steps: Int = 2,
    val no_internet_dialog: Boolean = true,
    val experimental_feature_1: Int = 0,
    val experimental_feature_message_index: Int = 0,
    val time_reload_native_without_video: Int = 30,
    val reload_native_seq: Boolean = false,
) {
    constructor(remoteConfigRepository: RemoteConfigRepository) : this(
        fast_reload_banner = remoteConfigRepository.getBooleanConfig("fast_reload_banner", true),
        fast_reload_banner_period = remoteConfigRepository.getIntConfig("fast_reload_banner_period", 15),
        collap_banner_interleave = remoteConfigRepository.getIntConfig("collap_banner_interleave", 0),
        force_update = remoteConfigRepository.getBooleanConfig("force_update", false),
        disable_ads = remoteConfigRepository.getBooleanConfig("disable_ads", false),
        inter_item_steps = remoteConfigRepository.getIntConfig("inter_item_steps", 2),
//        no_internet_dialog = remoteConfigRepository.getBooleanConfig("no_internet_dialog", true),
        no_internet_dialog = false,
        experimental_feature_1 = remoteConfigRepository.getIntConfig("experimental_feature_1", 0),
        experimental_feature_message_index = remoteConfigRepository.getIntConfig("experimental_feature_message_index", 0),
        time_reload_native_without_video = remoteConfigRepository.getIntConfig("time_reload_native_without_video", 30),
        reload_native_seq = remoteConfigRepository.getBooleanConfig("reload_native_seq", false),
    )

    companion object {
        const val SPLASH_LOGIC_ONLY_INTER = "only_inter"
        const val SPLASH_LOGIC_INTER_THEN_NA = "inter_then_na"
        const val SPLASH_LOGIC_NA_THEN_INTER = "native_then_in"
    }
}