package app.revanced.patches.shared.misc.audio.drc

import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.classDef
import app.revanced.patcher.patch.BytecodePatchBuilder
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.cloneMutableAndPreserveParameters
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.getReference
import app.revanced.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION_CLASS = "Lapp/revanced/extension/shared/patches/DisableDRCAudioPatch;"

internal fun disableDRCAudioPatch(
    block: BytecodePatchBuilder.() -> Unit,
    preferenceScreen: BasePreferenceScreen.Screen,
) = bytecodePatch(
    name = "Disable DRC audio",
    description = "Adds an option to disable dynamic range compression (DRC).",
) {
    block()

    dependsOn(addResourcesPatch)

    apply {
        addResources("shared", "misc.audio.drc.disableDRCAudioPatch")

        preferenceScreen.addPreferences(
            SwitchPreference("revanced_disable_drc_audio"),
        )

        val formatField = compressionRatioMethodMatch.method
            .getInstruction<ReferenceInstruction>(compressionRatioMethodMatch[0])
            .getReference<FieldReference>()!!
        val loudnessDbField = compressionRatioMethodMatch.method
            .getInstruction<ReferenceInstruction>(compressionRatioMethodMatch[2])
            .getReference<FieldReference>()!!

        formatStreamModelConstructorMethod.cloneMutableAndPreserveParameters().apply {
            val helperMethod = ImmutableMethod(
                definingClass,
                "patch_setLoudnessDb",
                listOf(),
                "V",
                AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                null,
                null,
                MutableMethodImplementation(7),
            ).toMutable().apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $EXTENSION_CLASS->disableDrcAudio()Z
                        move-result v0
                        if-eqz v0, :exit

                        # Get format field.
                        iget-object v0, p0, $formatField

                        # Set loudnessDb to 0.
                        const/4 v1, 0x0
                        iput v1, v0, $loudnessDbField

                        # Set format field.
                        iput-object v0, p0, $formatField

                        :exit
                        return-void
                    """,
                )
            }

            classDef.methods.add(helperMethod)

            findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
                addInstructionsAtControlFlowLabel(
                    index,
                    "invoke-direct/range { p0 .. p0 }, $helperMethod",
                )
            }
        }

        // If this flag is enabled, the DRC level will depend on other values besides loudnessDb.
        volumeNormalizationConfigMethodMatch.method.insertLiteralOverride(
            45425391L,
            "$EXTENSION_CLASS->disableDrcAudioFeatureFlag(Z)Z",
        )
    }
}
