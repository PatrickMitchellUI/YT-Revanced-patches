package app.revanced.patches.youtube.layout.player.fullscreen

import app.revanced.patcher.accessFlags
import app.revanced.patcher.after
import app.revanced.patcher.afterAtMost
import app.revanced.patcher.allOf
import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.definingClass
import app.revanced.patcher.field
import app.revanced.patcher.firstMethodComposite
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.method
import app.revanced.patcher.name
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import app.revanced.patcher.type
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method

/**
 * 19.46+
 */
internal val BytecodePatchContext.openVideosFullscreenPortraitMethodMatch by composingFirstMethod {
    returnType("V")
    parameterTypes("L", "Lj\$/util/Optional;")
    instructions(
        Opcode.MOVE_RESULT(), // Conditional check to modify.
        // Open videos fullscreen portrait feature flag.
        afterAtMost(5, 45666112L()), // Cannot be more than 5.
        afterAtMost(10, Opcode.MOVE_RESULT()),
    )
}

internal val BytecodePatchContext.openVideosFullscreenHookPatchExtensionMethod by gettingFirstMethodDeclaratively {
    name("isFullScreenPatchIncluded")
    definingClass(EXTENSION_CLASS_DESCRIPTOR)
    accessFlags(AccessFlags.PRIVATE, AccessFlags.STATIC)
    returnType("Z")
    parameterTypes()
}

internal val BytecodePatchContext.adPlayerFullscreenMethodMatch by composingFirstMethod {
    instructions(
        "Ad player fullscreen state entity is null in onSuccess on exit"(),
        afterAtMost(10, method { name == "getFullscreenForced" && returnType == "Ljava/lang/Boolean;" && parameterTypes.isEmpty() }),
        afterAtMost(10, allOf(Opcode.INVOKE_VIRTUAL(), method { returnType == "V" && parameterTypes.isEmpty() })),
    )
}

internal fun BytecodePatchContext.getNextGenWatchLayoutFullscreenConstructorMethodMatch(exitFullscreenMethodDefiningClass: String) =
    firstMethodComposite {
        definingClass("Lcom/google/android/apps/youtube/app/watch/nextgenwatch/ui/NextGenWatchLayout;")
        accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
        instructions(
            allOf(Opcode.CHECK_CAST(), type(exitFullscreenMethodDefiningClass)),
        )
    }

internal val BytecodePatchContext.playerDragGestureTypeMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.STATIC)
    returnType("Ljava/lang/String;")
    parameterTypes("I")
    "FULLSCREEN_DRAGGED_DOWN"()
    "MAXIMIZED_PULLED_UP"()
}

internal val BytecodePatchContext.playerDragGestureInitMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("I")
    parameterTypes("I", "I", "I", "I")
    instructions(
        Opcode.IGET_OBJECT(),
        after(Opcode.IF_EQZ()),
        after(Opcode.INVOKE_VIRTUAL()),
        after(Opcode.IGET()),
        after(Opcode.IF_NE()),
        after(Opcode.IGET()),
        after(Opcode.IF_NE()),
        after(Opcode.CONST_4()),
        after(Opcode.RETURN()),
        after(allOf(Opcode.IGET_OBJECT(), field { type.endsWith("/NextGenWatchLayout;") })),
    )
}
