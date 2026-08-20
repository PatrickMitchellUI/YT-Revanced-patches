package app.revanced.patches.youtube.layout.hide.player.flyoutmenu

import app.revanced.patcher.accessFlags
import app.revanced.patcher.afterAtMost
import app.revanced.patcher.allOf
import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.method
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.captionsOldBottomSheetLayoutInflaterMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Landroid/view/View;")
    parameterTypes(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;"
    )
    instructions(
        ResourceType.ID("bottom_sheet_title"),
        afterAtMost(3, method("findViewById")),
        ResourceType.STRING("subtitle_menu_settings_footer_info"),
        method("addFooterView"),
    )
}

internal val BytecodePatchContext.qualityOldBottomSheetLayoutInflaterMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Landroid/view/View;")
    parameterTypes(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;"
    )
    instructions(
        ResourceType.LAYOUT("video_quality_bottom_sheet_list_fragment_title"),
        method("addHeaderView"),
        method("addFooterView"),
    )
}
