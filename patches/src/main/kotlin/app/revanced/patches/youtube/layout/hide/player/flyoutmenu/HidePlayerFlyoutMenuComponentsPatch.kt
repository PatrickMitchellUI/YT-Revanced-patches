package app.revanced.patches.youtube.layout.hide.player.flyoutmenu

import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.extensions.methodReference
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
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/HidePlayerFlyoutMenuPatch;"
private const val EXTENSION_FILTER_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/litho/PlayerFlyoutMenuComponentsFilter;"

@Suppress("unused")
val hidePlayerFlyoutMenuComponentsPatch = bytecodePatch(
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
            "20.40.45",
            "20.45.36"
        ),
    )

    apply {
        addResources("youtube", "layout.hide.player.flyoutmenupanel.hidePlayerFlyoutMenuPatch")

        PreferenceScreen.PLAYER.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_hide_player_flyout",
                preferences = setOf(
                    SwitchPreference("revanced_hide_player_flyout_captions"),
                    SwitchPreference("revanced_hide_player_flyout_captions_footer"),
                    SwitchPreference("revanced_hide_player_flyout_captions_header"),
                    SwitchPreference("revanced_hide_player_flyout_listen_with_youtube_music"),
                    SwitchPreference("revanced_hide_player_flyout_help"),
                    SwitchPreference("revanced_hide_player_flyout_speed"),
                    SwitchPreference("revanced_hide_player_flyout_lock_screen"),
                    SwitchPreference(
                        key = "revanced_hide_player_flyout_audio_track",
                        tag = "app.revanced.extension.youtube.settings.preference.HideAudioFlyoutMenuPreference"
                    ),
                    SwitchPreference(
                        key = "revanced_hide_player_flyout_audio_track_footer",
                        tag = "app.revanced.extension.youtube.settings.preference.HideAudioFlyoutMenuPreference"
                    ),
                    SwitchPreference("revanced_hide_player_flyout_quality"),
                    SwitchPreference("revanced_hide_player_flyout_quality_footer"),
                    SwitchPreference("revanced_hide_player_flyout_quality_header"),
                    SwitchPreference("revanced_hide_player_flyout_additional_settings"),
                    SwitchPreference("revanced_hide_player_flyout_ambient_mode"),
                    SwitchPreference("revanced_hide_player_flyout_stable_volume"),
                    SwitchPreference("revanced_hide_player_flyout_loop_video"),
                    SwitchPreference("revanced_hide_player_flyout_sleep_timer"),
                    SwitchPreference("revanced_hide_player_flyout_watch_in_vr"),
                ),
            )
        )

        addLithoFilter(EXTENSION_FILTER_CLASS_DESCRIPTOR)

        // Hide the native (litho) bottom sheet header and footer.
        hookElement("$EXTENSION_CLASS_DESCRIPTOR->hideNativeBottomSheetHeader([B)[B")
        hookTreeNodeResult(
            descriptor = "$EXTENSION_CLASS_DESCRIPTOR->hideNativeBottomSheetFooter",
            isLazilyConvertedElement = false,
        )

        // region Patch the old ListView bottom sheet used by the Shorts flyout.

        captionsOldBottomSheetLayoutInflaterMethod.apply {
            val footerViewIndex = indexOfFirstInstructionOrThrow {
                methodReference?.name == "addFooterView"
            }
            val footerViewArgs = with(getInstruction<FiveRegisterInstruction>(footerViewIndex)) {
                arrayOf(registerC, registerD, registerE, registerF, registerG)
                    .take(registerCount).joinToString(", ") { "v$it" }
            }
            replaceInstruction(
                footerViewIndex,
                "invoke-static { $footerViewArgs }, $EXTENSION_CLASS_DESCRIPTOR->" +
                    "hideCaptionsOldBottomSheetFooter(Landroid/widget/ListView;Landroid/view/View;Ljava/lang/Object;Z)V",
            )

            val headerViewIndex = indexOfFirstInstructionOrThrow {
                methodReference?.name == "findViewById"
            }
            val headerViewArgs = with(getInstruction<FiveRegisterInstruction>(headerViewIndex)) {
                arrayOf(registerC, registerD, registerE, registerF, registerG)
                    .take(registerCount).joinToString(", ") { "v$it" }
            }
            replaceInstruction(
                headerViewIndex,
                "invoke-static { $headerViewArgs }, $EXTENSION_CLASS_DESCRIPTOR->" +
                    "hideCaptionsOldBottomSheetHeader(Landroid/view/View;I)Landroid/view/View;",
            )
        }

        qualityOldBottomSheetLayoutInflaterMethod.apply {
            val footerViewIndex = indexOfFirstInstructionOrThrow {
                methodReference?.name == "addFooterView"
            }
            val footerViewArgs = with(getInstruction<FiveRegisterInstruction>(footerViewIndex)) {
                arrayOf(registerC, registerD, registerE, registerF, registerG)
                    .take(registerCount).joinToString(", ") { "v$it" }
            }
            replaceInstruction(
                footerViewIndex,
                "invoke-static { $footerViewArgs }, $EXTENSION_CLASS_DESCRIPTOR->" +
                    "hideQualityOldBottomSheetFooter(Landroid/widget/ListView;Landroid/view/View;Ljava/lang/Object;Z)V",
            )

            val headerViewIndex = indexOfFirstInstructionOrThrow {
                methodReference?.name == "addHeaderView"
            }
            val headerViewArgs = with(getInstruction<FiveRegisterInstruction>(headerViewIndex)) {
                arrayOf(registerC, registerD, registerE, registerF, registerG)
                    .take(registerCount).joinToString(", ") { "v$it" }
            }
            replaceInstruction(
                headerViewIndex,
                "invoke-static { $headerViewArgs }, $EXTENSION_CLASS_DESCRIPTOR->" +
                    "hideQualityOldBottomSheetHeader(Landroid/widget/ListView;Landroid/view/View;Ljava/lang/Object;Z)V",
            )
        }

        // endregion
    }
}
