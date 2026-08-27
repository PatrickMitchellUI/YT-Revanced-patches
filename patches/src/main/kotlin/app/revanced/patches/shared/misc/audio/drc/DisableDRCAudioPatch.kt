package app.revanced.patches.shared.misc.audio.drc

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.classDef
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.BytecodePatchBuilder
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.cloneParameters
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.getReference
import app.revanced.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION_CLASS = "Lapp/revanced/extension/shared/patches/DisableDRCAudioPatch;"

internal fun disableDRCAudioPatch(
    block: BytecodePatchBuilder.() -> Unit,
    preferenceScreen: BasePreferenceScreen.Screen,
    useLegacyNormalizationFlag: BytecodePatchBuilder.() -> Boolean,
    useNormalizationFlag: BytecodePatchBuilder.() -> Boolean,
) = bytecodePatch(
    name = "Disable DRC audio",
    description = "Adds an option to disable DRC (Dynamic Range Compression) audio.",
) {
    block()

    dependsOn(addResourcesPatch)

    apply {
        addResources("shared", "misc.audio.drc.disableDRCAudioPatch")

        preferenceScreen.addPreferences(
            SwitchPreference("revanced_disable_drc_audio"),
        )

        val formatField =
            compressionRatioMethodMatch.method
                .getInstruction<ReferenceInstruction>(compressionRatioMethodMatch[0])
                .getReference<FieldReference>()!!
        val loudnessDbField =
            compressionRatioMethodMatch.method
                .getInstruction<ReferenceInstruction>(compressionRatioMethodMatch[2])
                .getReference<FieldReference>()!!

        formatStreamModelConstructorMethod.cloneParameters().apply {
            val helperMethod =
                ImmutableMethod(
                    definingClass,
                    "patch_setLoudnessDb",
                    listOf(),
                    "V",
                    AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                    null,
                    null,
                    MutableMethodImplementation(7),
                ).toMutable()
                    .apply {
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
                    }.also(classDef.methods::add)

            findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
                addInstructionsAtControlFlowLabel(
                    index,
                    "invoke-direct/range { p0 .. p0 }, $helperMethod",
                )
            }
        }

        val setConfigDisabledMethod = "$EXTENSION_CLASS->disableDrcAudioConfig(Z)Z"

        if (useLegacyNormalizationFlag()) {
            // If this flag is enabled, the DRC level will depend on other values besides loudnessDb.
            volumeNormalizationConfigLegacyMethodMatch.method.insertLiteralOverride(
                45425391L,
                setConfigDisabledMethod,
            )
        }

        if (useNormalizationFlag()) {
            volumeNormalizationConfigMethodMatch.method.insertLiteralOverride(
                45784793L,
                setConfigDisabledMethod,
            )

            // Removed again in later app targets (e.g. YouTube 21.25), so only override if present.
            optionalVolumeNormalizationConfigMethodMatch.methodOrNull?.apply {
                val moveResultIndex = optionalVolumeNormalizationConfigMethodMatch[3]
                val moveResultRegister =
                    getInstruction<OneRegisterInstruction>(moveResultIndex).registerA

                addInstructionsAtControlFlowLabel(
                    moveResultIndex + 1,
                    """
                        invoke-static { v$moveResultRegister }, $EXTENSION_CLASS->enableDrcAudioConfig(Z)Z
                        move-result v$moveResultRegister
                    """,
                )

                insertLiteralOverride(
                    45785775L,
                    setConfigDisabledMethod,
                )
            }
        }
    }
}
