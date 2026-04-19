package app.revanced.patches.shared.ad

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.lithoDialogBuilderMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("[B", "L")
    instructions(
        allOf(Opcode.INVOKE_VIRTUAL(), method("show")),
        ResourceType.STYLE("SlidingDialogAnimation")
    )
}
