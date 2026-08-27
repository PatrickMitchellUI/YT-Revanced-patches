package app.revanced.patches.youtube.layout.buttons.navigation

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.util.getting
import app.revanced.util.using
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

internal val BytecodePatchContext.castMenuItemInitializeMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    instructions(
        ResourceType.LAYOUT("castmediaroutebutton"),
        method("setShowAsAction"),
    )
}

internal val BytecodePatchContext.castMenuItemVisibilityMethodMatch by getting {
    firstMethodComposite {
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returnType("V")
        instructions(
            method("setVisible"),
        )
    }
} using { castMenuItemInitializeMethodMatch.method }

internal val BytecodePatchContext.animatedNavigationTabsFeatureFlagMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    instructions(
        45680008L(),
    )
}

internal val BytecodePatchContext.autoHideNavigationBarMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/support/v7/widget/RecyclerView;", "I", "I")
    // 21.x removed the 45677535 feature flag literal, anchor on the ViewConfiguration calls instead.
    instructions(
        method { toString() == "Landroid/view/ViewConfiguration;->get(Landroid/content/Context;)Landroid/view/ViewConfiguration;" },
        afterAtMost(5, method { toString() == "Landroid/view/ViewConfiguration;->getScaledTouchSlop()I" }),
    )
}

internal val BytecodePatchContext.pivotBarStyleMethodMatch by composingFirstMethod {
    definingClass("/PivotBar;")
    returnType("V")
    parameterTypes("L")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.XOR_INT_2ADDR,
    )
}

internal val BytecodePatchContext.pivotBarChangedMethodMatch by composingFirstMethod {
    name("onConfigurationChanged")
    definingClass("/PivotBar;")
    returnType("V")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
    )
}

internal val BytecodePatchContext.translucentNavigationStatusBarFeatureFlagMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    instructions(
        45400535L(), // Translucent status bar feature flag.
    )
}

/**
 * YouTube nav buttons.
 */
internal val BytecodePatchContext.translucentNavigationButtonsFeatureFlagMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    instructions(
        45630927L(), // Translucent navigation bar buttons feature flag.
    )
}

/**
 * Device on screen back/home/recent buttons.
 */
internal val BytecodePatchContext.translucentNavigationButtonsSystemFeatureFlagMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    instructions(
        45632194L(), // Translucent system buttons feature flag.
    )
}

internal val BytecodePatchContext.setWordmarkHeaderMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/widget/ImageView;")
    instructions(
        ResourceType.ATTR("ytPremiumWordmarkHeader"),
        ResourceType.ATTR("ytWordmarkHeader"),
    )
}

internal val BytecodePatchContext.wideSearchbarLayoutMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Landroid/view/View;")
    parameterTypes("L", "L")
    instructions(ResourceType.LAYOUT("action_bar_ringo"))
}
