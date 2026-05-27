package app.revanced.patches.youtube.layout.buttons.action

import app.revanced.patcher.accessFlags
import app.revanced.patcher.afterAtMost
import app.revanced.patcher.allOf
import app.revanced.patcher.composingFirstMethod
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
            allOf(Opcode.CHECK_CAST(), type("Landroid/view/ViewGroup;"))
        ),
    )
}
