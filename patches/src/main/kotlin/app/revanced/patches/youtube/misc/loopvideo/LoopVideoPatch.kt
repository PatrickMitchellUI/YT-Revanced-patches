package app.revanced.patches.youtube.misc.loopvideo

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.classDef
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.fieldReference
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.loopvideo.button.loopVideoButtonPatch
import app.revanced.patches.youtube.misc.playservice.is_21_36_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.video.information.playerStatusMethod
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.patches.youtube.video.videoid.hookVideoId
import app.revanced.patches.youtube.video.videoid.videoIdPatch
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/LoopVideoPatch;"

private const val EXTENSION_LOOP_VIDEO_CONTROLLER_INTERFACE =
    $$"Lapp/revanced/extension/youtube/patches/LoopVideoPatch$LoopVideoController;"

val loopVideoPatch = bytecodePatch(
    name = "Loop video",
    description = "Adds an option to loop videos and display loop video button in the video player.",
) {
    dependsOn(
        sharedExtensionPatch,
        addResourcesPatch,
        loopVideoButtonPatch,
        videoInformationPatch,
        videoIdPatch,
        versionCheckPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "20.40.45",
            "21.36.47"
        ),
    )

    apply {
        addResources("youtube", "misc.loopvideo.loopVideoPatch")

        PreferenceScreen.PLAYER.addPreferences(
            SwitchPreference("revanced_loop_video"),
        )

        if (is_21_36_or_greater) {
            // Use the native loop of the app, which is reset for every video.
            hookVideoId("$EXTENSION_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")

            loopModeChangedMethodMatch.method.apply {
                addInstruction(
                    0,
                    "invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS_DESCRIPTOR->setLoopMode(I)V",
                )

                // The player that sets the loop mode.
                val getPlayerIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_INTERFACE && methodReference?.parameterTypes?.isEmpty() == true
                }
                val getPlayerMethod = getInstruction(getPlayerIndex).methodReference!!
                val playerField = getInstruction(
                    indexOfFirstInstructionReversedOrThrow(getPlayerIndex, Opcode.IGET_OBJECT),
                ).fieldReference!!

                val setLoopModeMethod = getSetLoopModeMethodReference(playerField, getPlayerMethod)

                classDef.apply {
                    interfaces.add(EXTENSION_LOOP_VIDEO_CONTROLLER_INTERFACE)

                    methods.add(
                        ImmutableMethod(
                            type,
                            "patch_setLoopVideo",
                            listOf(ImmutableMethodParameter("Z", null, null)),
                            "V",
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            null,
                            null,
                            MutableMethodImplementation(4),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                """
                                    iget-object v0, p0, $playerField
                                    invoke-interface { v0 }, $getPlayerMethod
                                    move-result-object v0
                                    const/4 v1, 0x0
                                    if-eqz p1, :set_loop_mode
                                    const/4 v1, 0x2
                                    :set_loop_mode
                                    invoke-interface { v0, v1 }, $setLoopModeMethod
                                    return-void
                                """,
                            )
                        },
                    )

                    methods.first { AccessFlags.CONSTRUCTOR.isSet(it.accessFlags) }.apply {
                        findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
                            addInstruction(
                                index,
                                "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->" +
                                    "setLoopVideoController($EXTENSION_LOOP_VIDEO_CONTROLLER_INTERFACE)V",
                            )
                        }
                    }
                }
            }
        } else {
            playerStatusMethod.apply {
                // Add call to start playback again, but must happen before "Exit fullscreen" patch call.
                val insertIndex = indexOfFirstInstructionOrThrow(Opcode.SGET_OBJECT)
                // Since instructions are added just above Opcode.SGET_OBJECT, instead of calling findFreeRegister(),
                // a register from Opcode.SGET_OBJECT is used.
                val freeRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                // Since 'videoInformationPatch' is used as a dependency of this patch,
                // the loop is implemented through 'VideoInformation.seekTo(0)'.
                addInstructionsWithLabels(
                    insertIndex,
                    """
                        invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS_DESCRIPTOR->shouldLoopVideo(Ljava/lang/Enum;)Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :do_not_loop
                        return-void
                        :do_not_loop
                        nop
                    """
                )
            }
        }
    }
}
