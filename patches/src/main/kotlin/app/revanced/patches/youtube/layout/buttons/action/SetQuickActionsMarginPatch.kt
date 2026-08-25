package app.revanced.patches.youtube.layout.buttons.action

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/SetQuickActionsMarginPatch;"

@Suppress("unused")
val setQuickActionsMarginPatch =
    bytecodePatch(
        description = "Set top margin of the quick actions container view.",
    ) {
        dependsOn(
            sharedExtensionPatch,
            resourceMappingPatch,
        )

        apply {
            quickActionsElementSyntheticMethodMatch.let {
                it.method.apply {
                    val checkCastIndex = it[-1]
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(checkCastIndex).registerA

                    addInstruction(
                        checkCastIndex + 1,
                        "invoke-static { v$insertRegister }, $EXTENSION_CLASS_DESCRIPTOR->setQuickActionsMargin(Landroid/view/View;)V",
                    )
                }
            }
        }
    }
