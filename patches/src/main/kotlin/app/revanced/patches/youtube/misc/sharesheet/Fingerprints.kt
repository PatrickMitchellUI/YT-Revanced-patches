package app.revanced.patches.youtube.misc.sharesheet

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.queryIntentListMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("Ljava/util/List;")
    parameterTypes("Landroid/content/pm/PackageManager;")
    instructions(
        allOf(Opcode.INVOKE_VIRTUAL(), method("queryIntentActivities")),
        Opcode.MOVE_RESULT_OBJECT(),
        Opcode.RETURN_OBJECT(),
    )
}
