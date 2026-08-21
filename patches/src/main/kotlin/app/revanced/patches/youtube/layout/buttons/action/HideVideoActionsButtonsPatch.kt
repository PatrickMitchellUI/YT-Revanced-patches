package app.revanced.patches.youtube.layout.buttons.action

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.music.misc.extension.sharedExtensionPatch
import app.revanced.patches.shared.misc.litho.filter.addLithoFilter
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.litho.filter.lithoFilterPatch
import app.revanced.patches.youtube.misc.litho.node.hookTreeNodeElementPatch
import app.revanced.patches.youtube.misc.litho.node.hookTreeNodeResult
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.video.information.videoInformationPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val VIDEO_ACTION_BUTTON_FILTER_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/litho/VideoActionButtonsFilter;"

private const val QUICK_ACTION_BUTTON_FILTER_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/youtube/patches/litho/VideoActionButtonsFilter;"

@Suppress("unused")
val hideVideoActionButtonsPatch =
    bytecodePatch(
        name = "Hide video action buttons",
        description =
            "Adds options to hide quick actions and video action buttons " +
                "(such as the Download button) in fullscreen and portrait modes.",
    ) {
        dependsOn(
            settingsPatch,
            sharedExtensionPatch,
            resourceMappingPatch,
            lithoFilterPatch,
            videoInformationPatch,
            hookTreeNodeElementPatch,
            quickActionsMarginPatch,
            addResourcesPatch,
        )

        compatibleWith(
            "com.google.android.youtube"(
                "20.14.43",
                "20.21.37",
                "20.26.46",
                "20.31.42",
                "20.37.48",
                "20.45.36",
                "20.51.39",
            ),
        )

        apply {
            addResources("youtube", "layout.buttons.action.hideButtonsPatch")

            PreferenceScreen.PLAYER.addPreferences(
                PreferenceScreenPreference(
                    key = "revanced_hide_buttons_screen",
                    preferences =
                        setOf(
                            SwitchPreference("revanced_disable_like_subscribe_glow"),
                            SwitchPreference("revanced_hide_action_bar"),
                            SwitchPreference("revanced_hide_ask_button"),
                            SwitchPreference("revanced_hide_clip_button"),
                            SwitchPreference("revanced_hide_comments_button"),
                            SwitchPreference("revanced_hide_download_button"),
                            SwitchPreference("revanced_hide_hype_button"),
                            SwitchPreference("revanced_hide_like_dislike_button"),
                            SwitchPreference("revanced_hide_promote_button"),
                            SwitchPreference("revanced_hide_remix_button"),
                            SwitchPreference("revanced_hide_report_button"),
                            SwitchPreference("revanced_hide_save_button"),
                            SwitchPreference("revanced_hide_share_button"),
                            SwitchPreference("revanced_hide_shop_button"),
                            SwitchPreference("revanced_hide_stop_ads_button"),
                            SwitchPreference("revanced_hide_thanks_button"),
                        ),
                ),
                PreferenceScreenPreference(
                    key = "revanced_quick_actions_screen",
                    sorting = PreferenceScreenPreference.Sorting.UNSORTED,
                    preferences =
                        setOf(
                            SwitchPreference("revanced_hide_quick_actions_ask_button"),
                            SwitchPreference("revanced_hide_quick_actions_comments_button"),
                            SwitchPreference("revanced_hide_quick_actions_dislike_button"),
                            SwitchPreference("revanced_hide_quick_actions_like_button"),
                            SwitchPreference("revanced_hide_quick_actions_live_chat_button"),
                            SwitchPreference("revanced_hide_quick_actions_mix_button"),
                            SwitchPreference("revanced_hide_quick_actions_more_button"),
                            SwitchPreference("revanced_hide_quick_actions_more_videos_button"),
                            SwitchPreference("revanced_hide_quick_actions_playlist_button"),
                            SwitchPreference("revanced_hide_quick_actions_save_button"),
                            SwitchPreference("revanced_hide_quick_actions_share_button"),
                            SwitchPreference("revanced_hide_quick_actions"),
                        ),
                ),
            )

            addLithoFilter(VIDEO_ACTION_BUTTON_FILTER_CLASS_DESCRIPTOR)
            addLithoFilter(QUICK_ACTION_BUTTON_FILTER_CLASS_DESCRIPTOR)

            hookTreeNodeResult("$VIDEO_ACTION_BUTTON_FILTER_CLASS_DESCRIPTOR->onLazilyConvertedElementLoaded")

            parseWatchNextResponseMethodMatch.let {
                it.method.apply {
                    val index = it[5]
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstruction(
                        index + 1,
                        "invoke-static { v$register }, $VIDEO_ACTION_BUTTON_FILTER_CLASS_DESCRIPTOR->" +
                            "onSingleColumnWatchNextResultsLoaded(Lcom/google/protobuf/MessageLite;)V",
                    )
                }
            }
        }
    }
