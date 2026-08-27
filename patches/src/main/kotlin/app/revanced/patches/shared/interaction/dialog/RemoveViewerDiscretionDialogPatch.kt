package app.revanced.patches.shared.interaction.dialog

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/shared/patches/RemoveViewerDiscretionDialogPatch;"

@Suppress("unused")
internal fun removeViewerDiscretionDialogPatch(
    settingsPatch: Patch,
    sharedExtensionPatch: Patch,
    preferenceScreen: BasePreferenceScreen.Screen,
    vararg packages: Pair<String, Set<String>?>,
) = bytecodePatch(
    name = "Remove viewer discretion dialog",
    description =
        "Adds an option to remove the dialog that appears when opening a video that has been age-restricted " +
            "by accepting it automatically. This does not bypass the age restriction.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
    )

    compatibleWith(packages = packages)

    apply {
        addResources("youtube", "interaction.dialog.removeViewerDiscretionDialogPatch")

        PreferenceScreen.GENERAL.addPreferences(
            SwitchPreference("revanced_remove_viewer_discretion_dialog"),
        )

        fun applyPatch(
            instructionIndex: Int,
            instructionRegister: Int,
            method: MutableMethod,
        ) = method.addInstructions(
            instructionIndex,
            """
                invoke-static { v$instructionRegister }, $EXTENSION_CLASS_DESCRIPTOR->hideViewDiscretionDialog(Z)Z
                move-result v$instructionRegister
            """,
        )

        // region skip discretion dialog

        val skipDialogMatch =
            getSkipDialogMethodMatch(adultContentRunnableMethodMatch.method).also { match ->
                listOf(match[3], match[1]).forEach { instructionIndex ->
                    val instructionRegister =
                        match.method
                            .getInstruction<OneRegisterInstruction>(instructionIndex)
                            .registerA
                    applyPatch(instructionIndex + 1, instructionRegister, match.method)
                }
            }

        // endregion

        // region unlock related videos for restricted videos

        val firstFieldReference =
            adultContentSetPropertiesMethodMatch.method
                .getInstruction<ReferenceInstruction>(adultContentSetPropertiesMethodMatch[0])
                .reference
                .toString()
        val secondFieldReference =
            adultContentSetPropertiesMethodMatch.method
                .getInstruction<ReferenceInstruction>(adultContentSetPropertiesMethodMatch[2])
                .reference
                .toString()

        getUnlockRelatedVideosMethodMatch(
            skipDialogMatch.method.definingClass,
            firstFieldReference,
            secondFieldReference,
        ).let { match ->
            listOf(match[1], match[0]).forEach { instructionIndex ->
                val instructionRegister =
                    match.method
                        .getInstruction<TwoRegisterInstruction>(instructionIndex)
                        .registerA
                applyPatch(instructionIndex, instructionRegister, match.method)
            }
        }

        // endregion
    }
}
