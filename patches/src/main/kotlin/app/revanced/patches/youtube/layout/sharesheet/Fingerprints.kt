package app.revanced.patches.youtube.layout.sharesheet

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.initializeShareSheetPanelContentMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L", "Ljava/util/Map;")
    instructions(
        allOf(Opcode.INVOKE_INTERFACE(), method("next")),
        after(Opcode.MOVE_RESULT_OBJECT()),
        after(allOf(Opcode.CHECK_CAST(), type("Landroid/content/pm/ResolveInfo;"))),
        after(allOf(Opcode.IGET_OBJECT(), field("activityInfo"))),
        after(allOf(Opcode.IGET_OBJECT(), field("applicationInfo"))),
        after(allOf(Opcode.IGET_OBJECT(), field("packageName"))),
        after(allOf(Opcode.IGET_OBJECT(), field("activityInfo"))),
        after(allOf(Opcode.IGET_OBJECT(), field("name"))),
        after(Opcode.IF_EQZ()),
        after(allOf(Opcode.INVOKE_STATIC(), method("isEmpty"))),
    )
}
