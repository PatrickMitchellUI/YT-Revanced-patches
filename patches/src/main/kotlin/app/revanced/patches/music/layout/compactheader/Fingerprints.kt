package app.revanced.patches.music.layout.compactheader

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.util.literal
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.chipCloudMethodMatch by composingFirstMethod {
    returnType("V")
    instructions(
        ResourceType.LAYOUT("chip_cloud"),
        after(Opcode.CONST_4()),
        after(Opcode.INVOKE_STATIC()),
        after(Opcode.MOVE_RESULT_OBJECT()),
    )
}
