package app.revanced.patches.youtube.layout.buttons.action

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.BasePreference
import app.revanced.patches.shared.misc.settings.preference.ListPreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceCategory
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.shared.misc.litho.filter.addLithoFilter
import app.revanced.patches.youtube.misc.litho.filter.lithoFilterPatch
import app.revanced.patches.youtube.misc.playservice.is_20_22_or_greater
import app.revanced.patches.youtube.misc.playservice.is_21_36_or_greater
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

        val oldButtonPreferences = mutableSetOf<BasePreference>(
            SwitchPreference("revanced_hide_download_button"),
            SwitchPreference("revanced_hide_like_dislike_button"),
            SwitchPreference("revanced_hide_comments_button"),
            SwitchPreference("revanced_hide_save_button"),
            SwitchPreference("revanced_hide_remix_button"),
            SwitchPreference("revanced_hide_share_button"),
        )

        // 20.22+ cannot hide all action buttons because of buffer changes.
        // 21.36+ removes these buttons from the action bar before it is parsed.
        if (!is_20_22_or_greater || is_21_36_or_greater) {
            oldButtonPreferences += setOf(
                SwitchPreference("revanced_hide_hype_button"),
                SwitchPreference("revanced_hide_ask_button"),
                SwitchPreference("revanced_hide_clip_button"),
                SwitchPreference("revanced_hide_promote_button"),
                SwitchPreference("revanced_hide_report_button"),
                SwitchPreference("revanced_hide_shop_button"),
                SwitchPreference("revanced_hide_stop_ads_button"),
                SwitchPreference("revanced_hide_thanks_button"),
            )
        }

        val likeSubscribeGlowPreference = SwitchPreference("revanced_disable_like_subscribe_glow")

        // 21.36+ can use a compact action bar with different buttons.
        val preferences = if (is_21_36_or_greater) {
            setOf(
                ListPreference("revanced_action_button_style"),
                likeSubscribeGlowPreference,
                PreferenceCategory(
                    "revanced_hide_buttons_old_category",
                    preferences = oldButtonPreferences,
                ),
                PreferenceCategory(
                    "revanced_hide_buttons_new_category",
                    preferences = setOf(
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
                    ),
                ),
            )
        } else {
            oldButtonPreferences + likeSubscribeGlowPreference
        }

        PreferenceScreen.PLAYER.addPreferences(
            PreferenceScreenPreference(
                "revanced_hide_buttons_screen",
                sorting = if (is_21_36_or_greater) Sorting.UNSORTED else Sorting.BY_TITLE,
                preferences = preferences,
            ),
        )

        addLithoFilter("Lapp/revanced/extension/youtube/patches/litho/VideoActionButtonsFilter;")
    }
}
