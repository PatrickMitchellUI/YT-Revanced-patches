package app.revanced.patches.shared.ad

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.fieldReference
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.cloneMutableAndPreserveParameters
import app.revanced.util.findFreeRegister
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/shared/patches/HideFullscreenAdsPatch;"

/**
 * Patch shared with YouTube and YT Music.
 */
internal fun hideFullscreenAdsPatch(preferenceScreen: BasePreferenceScreen.Screen) =
    bytecodePatch(
        description = "Adds an option to hide fullscreen ads.",
    ) {
        dependsOn(
            addResourcesPatch,
            resourceMappingPatch,
        )

        apply {
            addResources("shared", "ad.hideFullscreenAdsPatch")

            preferenceScreen.addPreferences(
                SwitchPreference("revanced_hide_fullscreen_ads", summaryKey = null),
            )

            // Non-litho view, used in some old clients.
            interstitialsContainerMethodMatch.let {
                it.method.apply {
                    val viewIndex = it[0] + 2
                    val register = getInstruction<OneRegisterInstruction>(viewIndex).registerA

                    addInstruction(
                        viewIndex + 1,
                        "invoke-static { v$register }, ${EXTENSION_CLASS_DESCRIPTOR}->hideFullscreenAds(Landroid/view/View;)V",
                    )
                }
            }

            lithoDialogBuilderMethodMatch.let {
                // Find the class name of the custom dialog from the original method, before
                // cloneMutableAndPreserveParameters() shifts the fingerprint match indexes.
                val dialogClass =
                    it.method
                        .getInstruction(it[0])
                        .methodReference!!
                        .definingClass

                it.method.cloneMutableAndPreserveParameters().apply {
                    // The dialog can be closed after dialog.show(),
                    // and it is better to close the dialog after the layout of the dialog has changed
                    val insertIndex =
                        indexOfFirstInstructionReversedOrThrow {
                            opcode == Opcode.IPUT_OBJECT && fieldReference?.type == dialogClass
                        }
                    val insertRegister =
                        getInstruction<TwoRegisterInstruction>(insertIndex).registerA
                    val freeRegister = findFreeRegister(insertIndex, insertRegister)

                    addInstructionsAtControlFlowLabel(
                        insertIndex,
                        """
                        move-object/from16 v$freeRegister, p1
                        invoke-static { v$insertRegister, v$freeRegister }, ${EXTENSION_CLASS_DESCRIPTOR}->closeFullscreenAd(Ljava/lang/Object;[B)V
                    """,
                    )
                }
            }

            // Set close dialog method.
            fullscreenAdsCloseDialogMethod.addInstructions(
                0,
                """
                check-cast p0, ${customDialogOnBackPressedMethod.definingClass}
                invoke-virtual { p0 }, $customDialogOnBackPressedMethod
            """,
            )
        }
    }
