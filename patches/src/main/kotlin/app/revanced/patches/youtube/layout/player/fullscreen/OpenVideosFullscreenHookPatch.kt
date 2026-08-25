package app.revanced.patches.youtube.layout.player.fullscreen

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.classDef
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.typeReference
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.shortsplayer.openShortsInRegularPlayerPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.util.getMethodReferenceAtMatchedInstructionIndex
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/OpenVideosFullscreenHookPatch;"

private const val EXTENSION_FULLSCREEN_INTERFACE =
    "Lapp/revanced/extension/youtube/patches/OpenVideosFullscreenHookPatch\$FullscreenInterface;"

/**
 * Used by both [openVideosFullscreenPatch] and [openShortsInRegularPlayerPatch].
 */

internal val openVideosFullscreenHookPatch =
    bytecodePatch {
        dependsOn(
            sharedExtensionPatch,
        )

        apply {
            val exitFullscreenMethod = adPlayerFullscreenMethodMatch.getMethodReferenceAtMatchedInstructionIndex(2)

            // Implement fullscreen interface.
            exitFullscreenMethod.classDef.apply {
                interfaces.add(EXTENSION_FULLSCREEN_INTERFACE)
                methods.add(
                    ImmutableMethod(
                        exitFullscreenMethod.definingClass,
                        "patch_exitFullscreen",
                        emptyList(),
                        "V",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(1),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                            invoke-virtual { p0 }, $exitFullscreenMethod
                            return-void
                        """,
                        )
                    },
                )
            }

            // Pass the fullscreen interface object to extension code.
            nextGenWatchLayoutFullscreenConstructorMethodMatch.method.apply {
                val index =
                    indexOfFirstInstructionOrThrow {
                        opcode == Opcode.CHECK_CAST &&
                            typeReference!!.type == exitFullscreenMethod.definingClass
                    }
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->setFullscreenInterface($EXTENSION_FULLSCREEN_INTERFACE)V",
                )
            }

            openVideosFullscreenPortraitMethodMatch.let {
                var insertIndex = openVideosFullscreenPortraitMethodMatch[0]

                // Remove A/B feature call that forces what this patch already does.
                // Cannot use the A/B flag to accomplish the same goal because 19.50+
                // Shorts fullscreen regular player does not use fullscreen
                // if the player is minimized, and it must be forced using other conditional check.
                it.method.insertLiteralOverride(
                    it[-1],
                    false,
                )

                it.method.apply {
                    val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex + 1,
                        """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->doNotOpenVideoFullscreenPortrait(Z)Z
                        move-result v$register
                    """,
                    )
                }
            }
        }
    }
