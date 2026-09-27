package app.revanced.patches.youtube.misc.loopvideo

import app.revanced.patcher.accessFlags
import app.revanced.patcher.afterAtMost
import app.revanced.patcher.allOf
import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.field
import app.revanced.patcher.firstMethodComposite
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.method
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Called when the native loop mode of the player changes.
 */
internal val BytecodePatchContext.loopModeChangedMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("I", "Z")
    instructions("menu_item_single_video_playback_loop"())
}

/**
 * @return The method that sets the loop mode of the player,
 * found at the call site of the loop menu item click handler.
 */
internal fun BytecodePatchContext.getSetLoopModeMethodReference(
    playerField: FieldReference,
    getPlayerMethod: MethodReference,
) = firstMethodComposite {
    instructions(
        allOf(Opcode.IGET_OBJECT(), field { this == playerField }),
        afterAtMost(3, method { this == getPlayerMethod }),
        afterAtMost(
            10,
            allOf(
                Opcode.INVOKE_INTERFACE(),
                method {
                    definingClass == getPlayerMethod.returnType && returnType == "V" &&
                        parameterTypes.joinToString("") == "I"
                },
            ),
        ),
    )
}.let { it.method.getInstruction(it[2]).methodReference!! }
