package app.revanced.patches.music.layout.buttons

import app.revanced.patcher.*
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.mediaRouteButtonMethod by gettingFirstMethodDeclaratively("MediaRouteButton") {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returnType("Z")
}

internal val BytecodePatchContext.playerOverlayChipMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    instructions(ResourceType.ID("player_overlay_chip"))
}

internal val BytecodePatchContext.historyMenuItemMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/view/Menu;")
    instructions(
        ResourceType.ID("history_menu_item"),
        after(method("setVisible")),
        after(Opcode.RETURN_VOID()),
    )
    custom { immutableClassDef.methods.count() == 5 || immutableClassDef.methods.count() == 4 }
}

internal val BytecodePatchContext.historyMenuItemOfflineTabMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/view/Menu;")
    instructions(
        ResourceType.ID("history_menu_item"),
        ResourceType.ID("offline_settings_menu_item"),
        method("setVisible"),
        after(Opcode.RETURN_VOID()),
    )
}

internal val BytecodePatchContext.searchActionViewMethod by gettingFirstMethodDeclaratively {
    definingClass("/SearchActionProvider;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Landroid/view/View;")
    parameterTypes()
    instructions(ResourceType.LAYOUT("search_button"))
}

internal val BytecodePatchContext.topBarMenuItemImageViewMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Landroid/view/View;")
    parameterTypes()
    instructions(ResourceType.ID("top_bar_menu_item_image_view"))
}
