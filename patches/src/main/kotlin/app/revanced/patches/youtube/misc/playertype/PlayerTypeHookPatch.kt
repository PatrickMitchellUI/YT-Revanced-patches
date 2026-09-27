package app.revanced.patches.youtube.misc.playertype

import app.revanced.patcher.*
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.fieldReference
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playservice.is_21_36_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.util.forEachInstructionAsSequence
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/youtube/patches/PlayerTypeHookPatch;"

val playerTypeHookPatch = bytecodePatch(
    description = "Hook to get the current player type and video playback state.",
) {
    dependsOn(sharedExtensionPatch, resourceMappingPatch, versionCheckPatch)

    apply {
        val playerTypeClass = playerTypeEnumMethod.immutableClassDef.type

        if (is_21_36_or_greater) {
            // The overlay layout is no longer notified of player type changes.
            // Hook the field that holds the current player type instead.
            val playerTypeField = getPlayerTypeControllerMethod(playerTypeClass).immutableClassDef
                .fields.single { it.type == playerTypeClass }

            forEachInstructionAsSequence(
                match = { _, _, instruction, index ->
                    index.takeIf {
                        instruction.opcode == Opcode.IPUT_OBJECT &&
                            instruction.fieldReference == playerTypeField
                    }
                },
                transform = { method, index ->
                    val register = method.getInstruction<TwoRegisterInstruction>(index).registerA

                    method.addInstruction(
                        index + 1,
                        "invoke-static/range { v$register .. v$register }, " +
                            "$EXTENSION_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V",
                    )
                },
            )
        } else {
            firstMethodDeclaratively {
                definingClass("/YouTubePlayerOverlaysLayout;")
                accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
                returnType("V")
                parameterTypes(playerTypeClass)
            }.addInstruction(
                0,
                "invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V",
            )
        }

        reelWatchPagerMethodMatch.method.apply {
            val index = reelWatchPagerMethodMatch[-1]
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstruction(
                index + 1,
                "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->onShortsCreate(Landroid/view/View;)V",
            )
        }

        val controlStateType = controlsStateToStringMethod.immutableClassDef.type

        val videoStateEnumMethod = videoStateEnumMethod
        firstMethodComposite {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returnType("V")
            parameterTypes(controlStateType)
            instructions(
                field {
                    definingClass == controlStateType && type == videoStateEnumMethod.immutableClassDef.type
                },
                // Obfuscated parameter field name.
                ResourceType.STRING("accessibility_play"),
                ResourceType.STRING("accessibility_pause"),
            )
        }.let {
            it.method.apply {
                val videoStateFieldName = getInstruction<ReferenceInstruction>(it[0]).reference

                addInstructions(
                    0,
                    """
                        iget-object v0, p1, $videoStateFieldName  # copy VideoState parameter field
                        invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->setVideoState(Ljava/lang/Enum;)V
                    """,
                )
            }
        }
    }
}
