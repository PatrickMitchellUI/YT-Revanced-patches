package app.revanced.patches.youtube.layout.buttons.navigation

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.featureflag.FeatureFlag
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.createPivotBarMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameterTypes(
        "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
        "Landroid/widget/TextView;",
        "Ljava/lang/CharSequence;",
    )
    instructions(
        method { name == "setText" && definingClass == "Landroid/widget/TextView;" },
        Opcode.RETURN_VOID(),
    )
}

internal val BytecodePatchContext.autoHideNavigationBarMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/support/v7/widget/RecyclerView;", "I", "I")
    instructions(
        FeatureFlag.AUTO_HIDE_NAVIGATION_BAR(),
    )
}

/**
 * 21.36+ feed scroll listener that shows or hides the navigation bar (show, animate).
 */
internal val BytecodePatchContext.navigationBarScrollMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/support/v7/widget/RecyclerView;", "I", "I")
    instructions(
        method { name == "getScaledTouchSlop" && definingClass == "Landroid/view/ViewConfiguration;" },
        allOf(
            Opcode.INVOKE_INTERFACE(),
            method { returnType == "V" && parameterTypes.map { it.toString() } == listOf("Z", "Z") },
        ),
    )
}

internal val BytecodePatchContext.pivotBarStyleMethodMatch by composingFirstMethod {
    definingClass("/PivotBar;")
    returnType("V")
    parameterTypes("L")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.XOR_INT_2ADDR
    )
}

internal val BytecodePatchContext.pivotBarChangedMethodMatch by composingFirstMethod {
    name("onConfigurationChanged")
    definingClass("/PivotBar;")
    returnType("V")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT
    )
}

/**
 * 21.36+ BottomBarModel.toString(), used to find the shouldShowPivotBar field.
 */
internal val BytecodePatchContext.bottomBarModelToStringMethodMatch by composingFirstMethod {
    name("toString")
    returnType("Ljava/lang/String;")
    instructions(
        "BottomBarModel{pivotBarStyle="(),
        ", shouldShowPivotBar="(),
        afterAtMost(5, allOf(Opcode.IGET_BOOLEAN(), field { type == "Z" })),
    )
}

/**
 * Sets the new content dot or unread count of a navigation button (index, show badge, count).
 */
internal val BytecodePatchContext.pivotBarSetBadgeMethodMatch by composingFirstMethod {
    definingClass("Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("I", "Z", "I")
    instructions(
        method { returnType == "V" && parameterTypes.map { it.toString() } == listOf("Z", "I") },
    )
}
