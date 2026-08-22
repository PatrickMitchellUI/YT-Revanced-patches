package app.revanced.patches.youtube.layout.shortsautoplay

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.util.getting
import app.revanced.util.using
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

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

internal val BytecodePatchContext.reelPlaybackRepeatMethod by getting {
    firstMethodDeclaratively {
        returnType("V")
        parameterTypes("L")
        instructions(
            anyOf(
                method { toString() == "Lcom/google/common/util/concurrent/ListenableFuture;->isDone()Z" },
                // 21.17+
                method { toString() == "Lj\$/util/Optional;->ofNullable(Ljava/lang/Object;)Lj\$/util/Optional;" },
            ),
        )
    }
} using {
    firstImmutableMethodDeclaratively {
        returnType("V")
        instructions(
            method { toString() == "Lj\$/time/Instant;->toEpochMilli()J" },
            "r_tr"(),
        )
    }
}

internal val BytecodePatchContext.reelPlaybackMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("J")
    returnType("V")

    val methodParametersPrefix = listOf("I", "L", "L")

    instructions(
        field { definingClass == "Ljava/util/concurrent/TimeUnit;" && name == "MILLISECONDS" },
        afterAtMost(
            15,
            method {
                name == "<init>" && parameterTypes.size == methodParametersPrefix.size &&
                    parameterTypes.zip(methodParametersPrefix).all { (a, b) -> a.startsWith(b) }
            },
        ),
        afterAtMost(
            5,
            allOf(
                Opcode.INVOKE_VIRTUAL(),
                method { returnType == "I" && parameterTypes.count() == 1 && parameterTypes.first().startsWith("L") },
            ),
        ),
    )
}
