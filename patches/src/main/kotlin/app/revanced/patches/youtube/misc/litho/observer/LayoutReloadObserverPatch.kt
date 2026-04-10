@file:Suppress("SpellCheckingInspection")

package app.revanced.patches.youtube.misc.litho.observer

import app.revanced.patches.youtube.misc.litho.node.hookTreeNodeResult
import app.revanced.patches.youtube.misc.litho.node.hookTreeNodeElementPatch
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/LayoutReloadObserverPatch;"

val layoutReloadObserverPatch = bytecodePatch(
    description = "Hooks a method to detect in the extension when the RecyclerView at the bottom of the player is redrawn.",
) {
    dependsOn(
        sharedExtensionPatch,
        hookTreeNodeElementPatch
    )

    apply {
        hookTreeNodeResult("$EXTENSION_CLASS_DESCRIPTOR->onLazilyConvertedElementLoaded")
    }
}
