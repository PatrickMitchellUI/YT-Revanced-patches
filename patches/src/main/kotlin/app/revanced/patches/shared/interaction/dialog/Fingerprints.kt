package app.revanced.patches.shared.interaction.dialog

import app.revanced.patcher.accessFlags
import app.revanced.patcher.after
import app.revanced.patcher.afterAtMost
import app.revanced.patcher.allOf
import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.definingClass
import app.revanced.patcher.firstMethodComposite
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.method
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.reference
import app.revanced.patcher.returnType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val BytecodePatchContext.adultContentRunnableMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L")
    instructions(
        Opcode.IGET_OBJECT(),
        "allowControversialContent"(),
        afterAtMost(1, method { returnType == "Z" && parameterTypes.isEmpty() }),
        "allowAdultContent"(),
    )
}

internal val BytecodePatchContext.adultContentSetPropertiesMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Ljava/lang/String;")
    parameterTypes()
    "lastAudioTurnedOnInlinePlaybackId"()
    "lastAudioTurnedOffInlinePlaybackId"()
    "captionsRequested"()
    instructions(
        Opcode.IGET_BOOLEAN(),
        after("allowAdultContent"()),
        afterAtMost(2, Opcode.IGET_BOOLEAN()),
        after("allowControversialContent"()),
    )
}

// region skip discretion dialog
internal fun BytecodePatchContext.getSkipDialogMethodMatch(adultContentRunnableMethod: MethodReference) =
    firstMethodComposite {
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returnType("V")
        parameterTypes("L")
        instructions(
            allOf(
                Opcode.INVOKE_VIRTUAL(),
                method { toString() == "Ljava/lang/Boolean;->booleanValue()Z" },
            ),
            after(Opcode.MOVE_RESULT()),
            after(2, Opcode.INVOKE_VIRTUAL()),
            after(Opcode.MOVE_RESULT()),
            after(
                3,
                allOf(
                    Opcode.INVOKE_DIRECT(),
                    method { name == "<init>" && definingClass == adultContentRunnableMethod.definingClass },
                ),
            ),
        )
    }

// region unlock related videos for restricted videos
internal fun BytecodePatchContext.getUnlockRelatedVideosMethodMatch(
    definingClass: String,
    firstFieldReference: String,
    secondFieldReference: String,
) = firstMethodComposite {
    definingClass(definingClass)
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L")
    instructions(
        allOf(Opcode.IPUT_BOOLEAN(), reference(firstFieldReference, String::equals)),
        afterAtMost(
            3,
            allOf(Opcode.IPUT_BOOLEAN(), reference(secondFieldReference, String::equals)),
        ),
    )
}
// endregion
