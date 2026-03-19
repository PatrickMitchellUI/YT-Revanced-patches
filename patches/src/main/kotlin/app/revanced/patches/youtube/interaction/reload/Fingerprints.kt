package app.revanced.patches.youtube.interaction.reload

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.util.getting
import app.revanced.util.using
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.miniAppOpenYtContentCommandEndpointMethodMatch by getting {
    firstMethodComposite {
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returnType("V")
        parameterTypes("L")

        instructions(
            "no error message"(),
            Opcode.CHECK_CAST(),
            after(
                allOf(
                    Opcode.INVOKE_VIRTUAL(),
                    method { returnType == "V" && parameterTypes.isEmpty() },
                ),
            ),
        )
    }
} using {
    firstImmutableMethodDeclaratively {
        instructions(" for MiniAppOpenYTContentCommand: "(String::contains))
    }
}
