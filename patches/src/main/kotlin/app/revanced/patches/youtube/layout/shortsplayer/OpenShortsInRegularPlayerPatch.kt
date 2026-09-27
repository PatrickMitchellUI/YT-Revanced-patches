package app.revanced.patches.youtube.layout.shortsplayer

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.fieldReference
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.ListPreference
import app.revanced.patches.youtube.layout.player.fullscreen.openVideosFullscreenHookPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.mainActivityOnCreateMethod
import app.revanced.patches.youtube.video.information.playbackStartDescriptorToStringMethodMatch
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

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
            "20.40.45",
            "21.36.47"
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

        shortsPlaybackIntentMethod.addInstructionsWithLabels(
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
        exitVideoPlayerMethod.apply {
            // Activity.finish() is only called if a boolean field is set.
            // Override every read of that field, so the player is minimized instead.
            val lastFinishIndex = indexOfFirstInstructionReversedOrThrow {
                val reference = methodReference
                reference?.name == "finish" && reference.parameterTypes.isEmpty()
            }
            val exitField = getInstruction(
                indexOfFirstInstructionReversedOrThrow(lastFinishIndex, Opcode.IGET_BOOLEAN),
            ).fieldReference

            findInstructionIndicesReversedOrThrow {
                opcode == Opcode.IGET_BOOLEAN && fieldReference == exitField
            }.forEach { index ->
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->overrideBackPressToExit(Z)Z
                        move-result v$register
                    """,
                )
            }
        }
    }
}
