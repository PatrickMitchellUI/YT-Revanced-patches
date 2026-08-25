package app.revanced.patches.youtube.misc.fix.verticalscroll

import app.revanced.patcher.*
import app.revanced.patcher.invoke
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.canScrollVerticallyMethodMatch by composingFirstMethod {
    definingClass("Landroidx/swiperefreshlayout/widget/SwipeRefreshLayout;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes()
    opcodes(
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
    )
}

internal val BytecodePatchContext.choreographerPostFrameCallbackFeatureFlagMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameterTypes("L", "L")
    instructions(45782902L())
}
