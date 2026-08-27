package app.revanced.patches.youtube.video.quality

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.cloneParameters
import app.revanced.util.findFreeRegister
import app.revanced.util.numberOfParameterRegistersLogical
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/playback/quality/PrioritizeVideoQualityPatch;"

internal val prioritizeVideoQualityPatch =
    bytecodePatch {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            addResourcesPatch,
        )

        apply {
            addResources("youtube", "video.quality.prioritizeVideoQualityPatch")

            settingsMenuVideoQualityGroup.add(
                SwitchPreference("revanced_prioritize_video_quality"),
            )

            getVideoStreamingDataConstructorMethod().let {
                // Clone method to preserve parameters.
                it.method.cloneParameters().apply {
                    // Must offset match indexes since cloning adds additional move instructions.
                    val matchIndexOffset = numberOfParameterRegistersLogical
                    val videoIdIndex = it[1] + matchIndexOffset
                    val videoIdField = getInstruction<ReferenceInstruction>(videoIdIndex).reference

                    val adaptiveFormatsIndex = it[3] + matchIndexOffset
                    val adaptiveFormatsRegister =
                        getInstruction<TwoRegisterInstruction>(adaptiveFormatsIndex).registerA

                    val insertIndex = adaptiveFormatsIndex + 1
                    val videoIdRegister = findFreeRegister(insertIndex, adaptiveFormatsRegister)

                    addInstructions(
                        insertIndex,
                        """
                        # Get video id.
                        move-object/from16 v$videoIdRegister, p0
                        iget-object v$videoIdRegister, v$videoIdRegister, $videoIdField

                        # Override adaptive formats.
                        invoke-static { v$videoIdRegister, v$adaptiveFormatsRegister }, $EXTENSION_CLASS_DESCRIPTOR->prioritizeVideoQuality(Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
                        move-result-object v$adaptiveFormatsRegister
                    """,
                    )
                }
            }
        }
    }
