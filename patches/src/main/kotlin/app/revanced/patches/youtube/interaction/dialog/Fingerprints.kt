package app.revanced.patches.youtube.interaction.dialog

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

internal val BytecodePatchContext.allowControversialContentMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L")
    instructions(
        Opcode.IGET_OBJECT(),
        "allowControversialContent"(),
        afterAtMost(4, method { returnType == "Z" && parameterTypes.isEmpty() }),
        "allowAdultContent"(),
    )
}

context(_: BytecodePatchContext)
internal fun ClassDef.allowAdultContentMethod() = firstMethodDeclaratively {
    accessFlags(AccessFlags.PROTECTED, AccessFlags.FINAL)
    returnType("Ljava/lang/Boolean;")
    parameterTypes()
    instructions(
        0L(),
        method { toString() == "Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;" },
    )
}
