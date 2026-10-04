package app.revanced.patches.youtube.layout.shortsresuming

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.featureflag.FeatureFlag
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * 20.02+
 */
internal
val BytecodePatchContext.userWasInShortsListenerMethodMatch by composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("Ljava/lang/Object;")
    instructions(
        allOf(Opcode.CHECK_CAST(), type("Ljava/lang/Boolean;")),
        after(method { toString() == "Ljava/lang/Boolean;->booleanValue()Z" }),
        after(Opcode.MOVE_RESULT()),
        // 20.40+ string was merged into another string and is a partial match.
        afterAtMost(30, "ShortsStartup SetUserWasInShortsListener"(String::contains)),
    )
}

/**
 * 18.15.40+
 */
internal
val BytecodePatchContext.userWasInShortsConfigMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes()
    instructions(
        FeatureFlag.RESUME_SHORTS_ON_STARTUP(),
    )
}
