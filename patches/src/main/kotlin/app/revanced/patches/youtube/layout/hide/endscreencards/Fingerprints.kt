package app.revanced.patches.youtube.layout.hide.endscreencards

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.util.literal
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.layoutCircleMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes()
    returnType("Landroid/view/View;")
    instructions(
        ResourceType.LAYOUT("endscreen_element_layout_circle"),
        afterAtMost(10, Opcode.INVOKE_VIRTUAL()),
        after(Opcode.MOVE_RESULT_OBJECT()),
        after(Opcode.CHECK_CAST()),
    )
}

internal val BytecodePatchContext.layoutIconMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes()
    returnType("Landroid/view/View;")
    instructions(
        ResourceType.LAYOUT("endscreen_element_layout_icon"),
        afterAtMost(10, Opcode.INVOKE_VIRTUAL()),
        after(Opcode.MOVE_RESULT_OBJECT()),
        after(Opcode.CHECK_CAST()),
    )
}

internal val BytecodePatchContext.layoutVideoMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC)
    parameterTypes()
    returnType("Landroid/view/View;")
    instructions(
        ResourceType.LAYOUT("endscreen_element_layout_video"),
        afterAtMost(10, Opcode.INVOKE_VIRTUAL()),
        after(Opcode.MOVE_RESULT_OBJECT()),
        after(Opcode.CHECK_CAST()),
    )
}

internal val BytecodePatchContext.showEndscreenCardsMethod by gettingFirstMethodDeclaratively {
    returnType("V")
    parameterTypes("L")
    instructions(
        allOf(Opcode.IPUT_OBJECT(), field { type == "Ljava/lang/String;" }),
        allOf(Opcode.IGET_OBJECT(), field { type == "Ljava/lang/String;" }),
        afterAtMost(7, allOf(Opcode.INVOKE_VIRTUAL(), method("ordinal"))),
        5L(),
        8L(),
        9L()
    )
    custom {
        immutableClassDef.methods.count() == 5
                // 'public final' or 'final'
                && AccessFlags.FINAL.isSet(accessFlags)
    }
}

