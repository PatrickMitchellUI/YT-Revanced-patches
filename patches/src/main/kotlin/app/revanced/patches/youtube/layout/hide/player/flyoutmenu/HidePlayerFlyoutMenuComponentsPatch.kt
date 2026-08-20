package app.revanced.patches.youtube.layout.hide.player.flyoutmenu

import app.revanced.patcher.CompositeMatch
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.litho.filter.addLithoFilter
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.litho.filter.lithoFilterPatch
import app.revanced.patches.youtube.misc.litho.node.hookTreeNodeElementPatch
import app.revanced.patches.youtube.misc.litho.node.hookTreeNodeResult
import app.revanced.patches.youtube.misc.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.misc.protobuf.hookElement
import app.revanced.patches.youtube.misc.protobuf.hookProtobufElementParserPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/HidePlayerFlyoutMenuPatch;"
private const val EXTENSION_FILTER_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/litho/PlayerFlyoutMenuComponentsFilter;"

@Suppress("unused")
val hidePlayerFlyoutMenuComponentsPatch =
    bytecodePatch(
        name = "Hide player flyout menu components",
        description = "Adds options to hide menu components that appear when pressing the gear icon in the video player.",
    ) {
        dependsOn(
            lithoFilterPatch,
            playerTypeHookPatch,
            settingsPatch,
            addResourcesPatch,
            hookProtobufElementParserPatch,
            hookTreeNodeElementPatch,
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
            addResources("youtube", "layout.hide.player.flyoutmenupanel.hidePlayerFlyoutMenuPatch")

            PreferenceScreen.PLAYER.addPreferences(
                PreferenceScreenPreference(
                    key = "revanced_hide_player_flyout",
                    preferences =
                        setOf(
                            SwitchPreference("revanced_hide_player_flyout_captions", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_captions_footer", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_captions_header", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_listen_with_youtube_music", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_help", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_speed", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_lock_screen", summaryKey = null),
                            SwitchPreference(
                                key = "revanced_hide_player_flyout_audio_track",
                                summaryKey = null,
                                tag = "app.revanced.extension.youtube.settings.preference.HideAudioFlyoutMenuPreference",
                            ),
                            SwitchPreference(
                                key = "revanced_hide_player_flyout_audio_track_footer",
                                summaryKey = null,
                                tag = "app.revanced.extension.youtube.settings.preference.HideAudioFlyoutMenuPreference",
                            ),
                            SwitchPreference("revanced_hide_player_flyout_quality", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_quality_footer", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_quality_header", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_additional_settings", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_ambient_mode", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_stable_volume", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_loop_video", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_sleep_timer", summaryKey = null),
                            SwitchPreference("revanced_hide_player_flyout_watch_in_vr", summaryKey = null),
                        ),
                ),
            )

            addLithoFilter(EXTENSION_FILTER_CLASS_DESCRIPTOR)

            // Hide the native (litho) bottom sheet header and footer.
            hookElement("$EXTENSION_CLASS_DESCRIPTOR->hideNativeBottomSheetHeader([B)[B")
            hookTreeNodeResult(
                descriptor = "$EXTENSION_CLASS_DESCRIPTOR->hideNativeBottomSheetFooter",
                isLazilyConvertedElement = false,
            )

            // region Patch the old ListView bottom sheet used by the Shorts flyout.

            // Same method as MethodCall#replaceInvokeVirtualWithExtension.
            fun CompositeMatch.replaceInvokeVirtualWithExtension(
                index: Int,
                extensionMethod: String,
            ) {
                method.apply {
                    val registers =
                        with(getInstruction<FiveRegisterInstruction>(index)) {
                            arrayOf(registerC, registerD, registerE, registerF, registerG)
                                .take(registerCount)
                                .joinToString(", ") { "v$it" }
                        }

                    replaceInstruction(
                        index,
                        "invoke-static { $registers }, $EXTENSION_CLASS_DESCRIPTOR->$extensionMethod",
                    )
                }
            }

            captionsOldBottomSheetLayoutInflaterMethodMatch.replaceInvokeVirtualWithExtension(
                1,
                "hideCaptionsOldBottomSheetHeader(Landroid/view/View;I)Landroid/view/View;",
            )
            captionsOldBottomSheetLayoutInflaterMethodMatch.replaceInvokeVirtualWithExtension(
                3,
                "hideCaptionsOldBottomSheetFooter(Landroid/widget/ListView;Landroid/view/View;Ljava/lang/Object;Z)V",
            )
            qualityOldBottomSheetLayoutInflaterMethodMatch.replaceInvokeVirtualWithExtension(
                1,
                "hideQualityOldBottomSheetHeader(Landroid/widget/ListView;Landroid/view/View;Ljava/lang/Object;Z)V",
            )
            qualityOldBottomSheetLayoutInflaterMethodMatch.replaceInvokeVirtualWithExtension(
                2,
                "hideQualityOldBottomSheetFooter(Landroid/widget/ListView;Landroid/view/View;Ljava/lang/Object;Z)V",
            )

            // endregion
        }
    }
