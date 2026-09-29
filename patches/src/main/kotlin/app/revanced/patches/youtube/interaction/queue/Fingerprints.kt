package app.revanced.patches.youtube.interaction.queue

import app.revanced.patcher.accessFlags
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Resolves every innertube command to its handlers (`CommandMappingFactory$InOrderCommandMapping`).
 */
internal val BytecodePatchContext.commandRouterResolveMethod by gettingFirstMethodDeclaratively(
    "UNKNOWN_COMMAND",
) {
    returnType("V")
    parameterTypes("L", "Ljava/util/Map;")
}

/**
 * Creates a bottom sheet menu item from its renderer. Returns an absent Guava optional if the item has no text.
 */
internal val BytecodePatchContext.bottomSheetMenuItemMethod by gettingFirstMethodDeclaratively(
    "Text missing for BottomSheetMenuItem.",
) {
    accessFlags(AccessFlags.PROTECTED, AccessFlags.FINAL)
    returnType("L")
    parameterTypes("L")
}

/**
 * 20.x: Creates a list sheet menu item from its renderer. Returns an absent Guava optional if the item has no text.
 */
internal val BytecodePatchContext.sheetMenuItemLegacyMethod by gettingFirstMethodDeclaratively(
    "Text missing for BottomSheetMenuItem.",
) {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    parameterTypes("L")
}

/**
 * 21.36+: Creates a list sheet menu item from a model that holds the renderer.
 * Returns an empty optional if the item has no text.
 */
internal val BytecodePatchContext.sheetMenuItemFromModelMethod by gettingFirstMethodDeclaratively(
    "Text missing for BottomSheetMenuItem.",
) {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("Lj${'$'}/util/Optional;")
    parameterTypes("L")
}
