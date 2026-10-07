package app.revanced.patches.youtube.layout.buttons.action

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.ListPreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceCategory
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.shared.misc.litho.filter.addLithoFilter
import app.revanced.patches.youtube.misc.litho.filter.lithoFilterPatch
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen

@Suppress("unused")
val hideVideoActionButtonsPatch = resourcePatch(
    name = "Hide video action buttons",
    description = "Adds options to hide action buttons (such as the Download button) under videos. " +
            "Patching version 20.21.37 or lower can hide more player button types."
) {
    dependsOn(
        resourceMappingPatch,
        lithoFilterPatch,
        addResourcesPatch,
        versionCheckPatch,
        actionButtonStylePatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "20.40.45",
            "21.36.47"
        ),
    )

    apply {
        addResources("youtube", "layout.buttons.action.hideButtonsPatch")

        val likeSubscribeGlowPreference = SwitchPreference("revanced_disable_like_subscribe_glow")

        // Buttons in the order they are shown, rarely shown buttons after the common ones.
        val preferences = setOf(
            ListPreference("revanced_action_button_style"),
            likeSubscribeGlowPreference,
            PreferenceCategory(
                "revanced_hide_buttons_new_category",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("revanced_show_compact_like_dislike_counts"),
                    SwitchPreference("revanced_hide_compact_like_button"),
                    SwitchPreference("revanced_hide_compact_dislike_button"),
                    SwitchPreference(
                        "revanced_hide_compact_share_button",
                        titleKey = "revanced_hide_share_button_title",
                        summaryOnKey = "revanced_hide_share_button_summary_on",
                        summaryOffKey = "revanced_hide_share_button_summary_off",
                    ),
                    SwitchPreference(
                        "revanced_hide_compact_save_button",
                        titleKey = "revanced_hide_save_button_title",
                        summaryOnKey = "revanced_hide_save_button_summary_on",
                        summaryOffKey = "revanced_hide_save_button_summary_off",
                    ),
                    SwitchPreference("revanced_hide_compact_more_actions_button"),
                    SwitchPreference("revanced_hide_compact_thanks_button"),
                    SwitchPreference("revanced_hide_compact_download_button"),
                    SwitchPreference("revanced_hide_compact_report_button"),
                ),
            ),
            PreferenceCategory(
                "revanced_hide_buttons_old_category",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("revanced_hide_like_dislike_button"),
                    SwitchPreference("revanced_hide_share_button"),
                    SwitchPreference("revanced_hide_save_button"),
                    SwitchPreference("revanced_hide_thanks_button"),
                    SwitchPreference("revanced_hide_download_button"),
                    SwitchPreference("revanced_hide_remix_button"),
                    SwitchPreference("revanced_hide_clip_button"),
                    SwitchPreference("revanced_hide_hype_button"),
                    SwitchPreference("revanced_hide_ask_button"),
                    SwitchPreference("revanced_hide_promote_button"),
                    SwitchPreference("revanced_hide_shop_button"),
                    SwitchPreference("revanced_hide_stop_ads_button"),
                    SwitchPreference("revanced_hide_comments_button"),
                    SwitchPreference("revanced_hide_report_button"),
                ),
            ),
        )

        PreferenceScreen.PLAYER.addPreferences(
            PreferenceScreenPreference(
                "revanced_hide_buttons_screen",
                sorting = Sorting.UNSORTED,
                preferences = preferences,
            ),
        )

        addLithoFilter("Lapp/revanced/extension/youtube/patches/litho/VideoActionButtonsFilter;")
    }
}
