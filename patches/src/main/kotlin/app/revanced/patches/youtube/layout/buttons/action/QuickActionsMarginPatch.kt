package app.revanced.patches.youtube.layout.buttons.action

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/youtube/patches/QuickActionsMarginPatch;"

@Suppress("unused")
val quickActionsMarginPatch =
    bytecodePatch(
        description = "Injects a configurable top margin into the quick actions container view.",
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
                        "invoke-static { v$insertRegister }, $EXTENSION_CLASS->setQuickActionsMargin(Landroid/view/View;)V",
                    )
                }
            }
        }
    }
