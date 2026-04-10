package app.revanced.patches.youtube.misc.litho.node

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.litho.context.EXTENSION_CONTEXT_INTERFACE
import app.revanced.patches.shared.misc.litho.context.conversionContextPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.getFreeRegisterProvider
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/HookTreeNodeElementPatch;"

private lateinit var componentLoadedMethod: MutableMethod
private lateinit var lazilyConvertedElementLoadedMethod: MutableMethod

internal val hookTreeNodeElementPatch = bytecodePatch(
    description = "Hooks the tree node element lists to the extension."
) {
    dependsOn(
        sharedExtensionPatch,
        conversionContextPatch
    )

    apply {
        treeNodeResultListMethod.apply {
            val insertIndex = instructions.lastIndex
            val listRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            val registerProvider = getFreeRegisterProvider(insertIndex, 1)
            val freeRegister = registerProvider.getFreeRegister()

            addInstructionsAtControlFlowLabel(
                insertIndex,
                """
                    move-object/from16 v$freeRegister, p2
                    invoke-static { v$freeRegister, v$listRegister }, $EXTENSION_CLASS_DESCRIPTOR->onTreeNodeResultLoaded(${EXTENSION_CONTEXT_INTERFACE}Ljava/util/List;)V
                """
            )
        }

        componentLoadedMethod = componentPatchMethod
        lazilyConvertedElementLoadedMethod = lazilyConvertedElementPatchMethod
    }
}

internal fun hookTreeNodeResult(
    descriptor: String,
    isLazilyConvertedElement: Boolean = true,
) {
    val method = if (isLazilyConvertedElement) lazilyConvertedElementLoadedMethod
    else componentLoadedMethod

    method.addInstruction(
        0,
        "invoke-static { p0, p1 }, $descriptor(Ljava/lang/String;Ljava/util/List;)V",
    )
}
