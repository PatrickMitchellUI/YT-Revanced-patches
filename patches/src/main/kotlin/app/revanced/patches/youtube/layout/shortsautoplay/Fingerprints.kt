package app.revanced.patches.youtube.layout.shortsautoplay

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

internal val BytecodePatchContext.reelEnumConstructorMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR)
    instructions(
        "REEL_LOOP_BEHAVIOR_UNKNOWN"(),
        "REEL_LOOP_BEHAVIOR_SINGLE_PLAY"(),
        "REEL_LOOP_BEHAVIOR_REPEAT"(),
        "REEL_LOOP_BEHAVIOR_END_SCREEN"(),
        Opcode.RETURN_VOID(),
    )
}

/**
 * 20.x
 */
internal val BytecodePatchContext.reelPlaybackRepeatParentMethod by gettingFirstImmutableMethodDeclaratively {
    returnType("V")
    instructions(
        "Reels[%s] Playback Time: %d ms"(),
    )
}

/**
 * 20.x, matches class found in [reelPlaybackRepeatParentMethod].
 */
context(_: BytecodePatchContext)
internal fun ClassDef.getReelPlaybackRepeatMethod() = firstMethodDeclaratively {
    returnType("V")
    parameterTypes("L")
    instructions(method { toString() == "Lcom/google/common/util/concurrent/ListenableFuture;->isDone()Z" })
}

/**
 * 21.36+, resolves the loop behavior of a Short.
 */
internal fun BytecodePatchContext.getReelLoopBehaviorResolverMethod(reelEnumClass: String) = firstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType(reelEnumClass)
    parameterTypes("L", "L")
    instructions(
        method {
            definingClass == reelEnumClass && returnType == reelEnumClass &&
                parameterTypes.joinToString("") == "I"
        },
    )
}
