package app.revanced.patches.youtube.layout.shortsplayer

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.ListPreference
import app.revanced.patches.youtube.layout.player.fullscreen.openVideosFullscreenHookPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.misc.playservice.is_21_20_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.mainActivityOnCreateMethod
import app.revanced.patches.youtube.video.information.playbackStartDescriptorToStringMethodMatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.indexOfFirstInstructionReversed
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/OpenShortsInRegularPlayerPatch;"

@Suppress("unused")
val openShortsInRegularPlayerPatch = bytecodePatch(
    name = "Open Shorts in regular player",
    description = "Adds options to open Shorts in the regular video player.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        openVideosFullscreenHookPatch,
        navigationBarHookPatch,
        versionCheckPatch,
        resourceMappingPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "20.14.43",
            "20.21.37",
            "20.26.46",
            "20.31.42",
            "20.37.48",
            "20.45.36",
            "20.47.62"
        ),
    )

    apply {
        addResources("youtube", "layout.shortsplayer.shortsPlayerTypePatch")

        PreferenceScreen.SHORTS.addPreferences(
            ListPreference("revanced_shorts_player_type"),
        )

        // Activity is used as the context to launch an Intent.
        mainActivityOnCreateMethod.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->" +
                    "setMainActivity(Landroid/app/Activity;)V",
        )


        val playbackStartVideoIdMethod = playbackStartDescriptorToStringMethodMatch.let {
            navigate(it.method).to(it[1]).original()
        }

        (if (is_21_20_or_greater) shortsPlaybackIntentMethod else shortsPlaybackIntentLegacyMethod).addInstructionsWithLabels(
            0,
            """
                move-object/from16 v0, p1
                
                invoke-virtual { v0 }, $playbackStartVideoIdMethod
                move-result-object v1
                invoke-static { v1 }, ${EXTENSION_CLASS_DESCRIPTOR}->openShort(Ljava/lang/String;)Z
                move-result v1
                if-eqz v1, :disabled
                return-void
                
                :disabled
                nop
            """
        )

        // Fix issue with back button exiting the app instead of minimizing the player.
        // Without this change this issue can be difficult to reproduce, but seems to occur
        // most often with 'open video in regular player' and not open in fullscreen player.
        // Note: this patch must be applied on the conditional instructions that contains the return
        // instruction, to avoid to block the code that minimize the video player.
        exitVideoPlayerMethod.apply {
            val expectedChanges = 2
            var changesMade = 0
            var finishIndex = instructions.size

            while (true) {
                finishIndex = indexOfFirstInstructionReversed(finishIndex - 1) {
                    val reference = methodReference
                    reference?.name == "finish" && reference.parameterTypes.isEmpty()
                }
                if (finishIndex < 0) break

                val finishLabels = getInstruction(finishIndex).location.labels
                val equalsIndex = if (finishLabels.isNotEmpty()) {
                    // Find conditional instruction that jumps to this instruction.
                    indexOfFirstInstructionReversedOrThrow(finishIndex - 1) {
                        if (this !is BuilderOffsetInstruction) {
                            return@indexOfFirstInstructionReversedOrThrow false
                        }
                        val labels = target.location.labels
                        labels.any { it in finishLabels }
                    }
                } else {
                    indexOfFirstInstructionReversedOrThrow(finishIndex) {
                        opcode == Opcode.IF_EQZ || opcode == Opcode.IF_NEZ
                    }
                }

                val register = getInstruction<OneRegisterInstruction>(equalsIndex).registerA
                addInstructionsAtControlFlowLabel(
                    equalsIndex,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->overrideBackPressToExit(Z)Z
                        move-result v$register
                    """
                )
                changesMade++
            }

            if (changesMade != expectedChanges) {
                throw PatchException("Expected $expectedChanges changes but instead found: $changesMade")
            }
        }
    }
}
