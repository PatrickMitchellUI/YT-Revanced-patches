package app.revanced.patches.music.misc.backgroundplayback

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.backgroundPlaybackDisableMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("Z")
    parameterTypes("L")
    opcodes(
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IGET,
        Opcode.AND_INT_LIT16,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.IGET,
    )
}

internal val BytecodePatchContext.kidsBackgroundPlaybackPolicyControllerMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("I", "L", "Z")
    opcodes(
        Opcode.SGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.IPUT_BOOLEAN,
    )
}
