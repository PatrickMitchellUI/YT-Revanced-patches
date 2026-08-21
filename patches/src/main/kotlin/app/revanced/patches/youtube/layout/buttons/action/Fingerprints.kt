package app.revanced.patches.youtube.layout.buttons.action

import app.revanced.patcher.accessFlags
import app.revanced.patcher.after
import app.revanced.patcher.afterAtMost
import app.revanced.patcher.allOf
import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.field
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import app.revanced.patcher.type
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Matches the method that processes the quick actions container view.
 * Used to inject a top margin adjustment into the quick actions bar.
 */
internal val BytecodePatchContext.quickActionsElementSyntheticMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("v")
    parameterTypes("Landroid/view/View;")

    instructions(
        ResourceType.ID("quick_actions_element_container"),
        afterAtMost(
            10,
            allOf(Opcode.CHECK_CAST(), type("Landroid/view/ViewGroup;")),
        ),
    )
}

internal val BytecodePatchContext.parseWatchNextResponseMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("Ljava/lang/Object;")
    returnType("Ljava/util/List;")
    instructions(
        49399797L(),
        Opcode.SGET_OBJECT(),
        after(allOf(Opcode.IGET_OBJECT(), field())),
        51779735L(),
        afterAtMost(5, allOf(Opcode.IGET_OBJECT(), field { type == "Ljava/lang/Object;" })),
        after(Opcode.CHECK_CAST()),
        46659098L(),
    )
}
