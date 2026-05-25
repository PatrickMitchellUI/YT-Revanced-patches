package app.revanced.patches.youtube.layout.buttons.action

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.litho.filter.addLithoFilter
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.PreferenceCategory
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.litho.filter.lithoFilterPatch
import app.revanced.patches.youtube.misc.playservice.is_20_22_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen

@Suppress("unused")
val hideVideoActionButtonsPatch = resourcePatch(
    name = "Hide video action buttons",
    description = "Adds options to hide video action buttons in fullscreen and portrait modes."
) {
    dependsOn(
        resourceMappingPatch,
        lithoFilterPatch,
        addResourcesPatch,
        versionCheckPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "20.14.43",
            "20.21.37",
            "20.26.46",
            "20.31.42",
            "20.37.48",
            "20.45.36",
            "20.47.62",
            "20.51.39"
        ),
    )

    apply {
        addResources("youtube", "layout.buttons.action.hideButtonsPatch")

        val portraitPreferences = mutableSetOf(
            SwitchPreference("revanced_disable_like_subscribe_glow"),
            SwitchPreference("revanced_hide_download_button", summaryKey = null),
            SwitchPreference("revanced_hide_like_dislike_button", summaryKey = null),
            SwitchPreference("revanced_hide_comments_button", summaryKey = null),
            SwitchPreference("revanced_hide_save_button", summaryKey = null),
            SwitchPreference("revanced_hide_remix_button", summaryKey = null),
            SwitchPreference("revanced_hide_share_button", summaryKey = null),
        )

        // 20.22+ cannot hide all action buttons because of buffer changes.
        if (!is_20_22_or_greater) {
            portraitPreferences.addAll(
                listOf(
                    SwitchPreference("revanced_hide_hype_button", summaryKey = null),
                    SwitchPreference("revanced_hide_ask_button", summaryKey = null),
                    SwitchPreference("revanced_hide_clip_button", summaryKey = null),
                    SwitchPreference("revanced_hide_promote_button", summaryKey = null),
                    SwitchPreference("revanced_hide_report_button", summaryKey = null),
                    SwitchPreference("revanced_hide_shop_button", summaryKey = null),
                    SwitchPreference("revanced_hide_stop_ads_button", summaryKey = null),
                    SwitchPreference("revanced_hide_thanks_button", summaryKey = null),
                ),
            )
        }

        PreferenceScreen.PLAYER.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_action_buttons_screen",
                preferences = setOf(
                    PreferenceCategory(
                        titleKey = "revanced_portrait_buttons",
                        preferences = portraitPreferences,
                    ),
                    PreferenceCategory(
                        titleKey = "revanced_fullscreen_buttons",
                        preferences = setOf(
                            SwitchPreference("revanced_hide_quick_actions", summaryKey = null),
                            SwitchPreference("revanced_hide_quick_actions_ask_button", summaryKey = null),
                            SwitchPreference("revanced_hide_quick_actions_comments_button", summaryKey = null),
                            SwitchPreference("revanced_hide_quick_actions_dislike_button", summaryKey = null),
                            SwitchPreference("revanced_hide_quick_actions_like_button", summaryKey = null),
                            SwitchPreference("revanced_hide_quick_actions_live_chat_button", summaryKey = null),
                            SwitchPreference("revanced_hide_quick_actions_mix_button", summaryKey = null),
                            SwitchPreference("revanced_hide_quick_actions_more_button", summaryKey = null),
                            SwitchPreference("revanced_hide_quick_actions_more_videos_button", summaryKey = null),
                            SwitchPreference("revanced_hide_quick_actions_playlist_button", summaryKey = null),
                            SwitchPreference("revanced_hide_quick_actions_save_button", summaryKey = null),
                            SwitchPreference("revanced_hide_quick_actions_share_button", summaryKey = null),
                        ),
                    ),
                ),
            ),
        )

        addLithoFilter("Lapp/revanced/extension/youtube/patches/litho/VideoActionButtonsFilter;")
        addLithoFilter("Lapp/revanced/extension/youtube/patches/litho/QuickActionButtonsFilter;")
    }
}
