package app.revanced.patches.youtube.layout.feedspacing

import app.revanced.patcher.accessFlags
import app.revanced.patcher.definingClass
import app.revanced.patcher.field
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.instructions
import app.revanced.patcher.method
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import com.android.tools.smali.dexlib2.AccessFlags

internal const val RECYCLER_VIEW_CLASS_TYPE = "Landroid/support/v7/widget/RecyclerView;"

/**
 * Item decoration of the element feeds. Sets the top and bottom offset of an item from its model, in dp.
 */
internal val BytecodePatchContext.feedItemDecorationGetItemOffsetsMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/graphics/Rect;", "Landroid/view/View;", RECYCLER_VIEW_CLASS_TYPE, "L")
    instructions(
        field { toString() == "Landroid/util/DisplayMetrics;->density:F" },
        field { toString() == "Landroid/graphics/Rect;->top:I" },
        field { toString() == "Landroid/util/DisplayMetrics;->density:F" },
        field { toString() == "Landroid/graphics/Rect;->bottom:I" },
    )
}

/**
 * Sums the offsets of all item decorations of a child (getItemDecorInsetsForChild).
 */
internal val BytecodePatchContext.recyclerViewGetItemDecorInsetsMethod by gettingFirstMethodDeclaratively {
    definingClass(RECYCLER_VIEW_CLASS_TYPE)
    returnType("Landroid/graphics/Rect;")
    parameterTypes("Landroid/view/View;")
    instructions(
        method { toString() == "Landroid/graphics/Rect;->set(IIII)V" },
        method { toString() == "Landroid/graphics/Rect;->set(IIII)V" },
    )
}

internal val BytecodePatchContext.recyclerViewInvalidateItemDecorationsMethod by gettingFirstMethodDeclaratively(
    "Cannot invalidate item decorations during a scroll or layout",
) {
    definingClass(RECYCLER_VIEW_CLASS_TYPE)
    returnType("V")
    parameterTypes()
}
