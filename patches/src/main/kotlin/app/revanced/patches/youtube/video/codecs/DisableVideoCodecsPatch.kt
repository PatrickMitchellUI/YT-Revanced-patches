package app.revanced.patches.youtube.video.codecs

import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.forEachInstructionAsSequence
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/DisableVideoCodecsPatch;"

@Suppress("unused")
val disableVideoCodecsPatch = bytecodePatch(
    name = "Disable video codecs",
    description = "Adds options to disable HDR and VP9 codecs.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
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
            "20.47.62",
            "20.51.39"
        ),
    )

    apply {
        // Override all calls of `getSupportedHdrTypes`.
        forEachInstructionAsSequence(
            match@{ classDef, _, instruction, instructionIndex ->
                if (classDef.type.startsWith("Lapp/revanced/")) {
                    return@match null
                }

                val reference = instruction.methodReference
                if (reference?.definingClass == "Landroid/view/Display\$HdrCapabilities;" && reference.name == "getSupportedHdrTypes") {
                    return@match instruction to instructionIndex
                }
                return@match null
            },
            { method, entry ->
                val (instruction, index) = entry
                val register = (instruction as FiveRegisterInstruction).registerC

                method.replaceInstruction(
                    index,
                    "invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS_DESCRIPTOR->" +
                            $$"disableHdrVideo(Landroid/view/Display$HdrCapabilities;)[I",
                )
            },
        )

        addResources("youtube", "video.codecs.disableVideoCodecsPatch")

        PreferenceScreen.VIDEO.addPreferences(
            SwitchPreference("revanced_disable_hdr_video", summaryKey = null),
            SwitchPreference(
                key = "revanced_force_avc_codec", summaryKey = null,
                tag = "app.revanced.extension.youtube.settings.preference.ForceAVCSwitchPreference"
            )
        )

        vp9CapabilityMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->allowVP9()Z
                move-result v0
                if-nez v0, :default
                return v0
                :default
                nop
            """,
        )
    }
}
