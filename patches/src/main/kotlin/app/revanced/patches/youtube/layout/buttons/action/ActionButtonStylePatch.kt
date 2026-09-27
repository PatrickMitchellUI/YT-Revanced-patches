package app.revanced.patches.youtube.layout.buttons.action

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playservice.is_21_36_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/ActionButtonStylePatch;"

internal val actionButtonStylePatch = bytecodePatch(
    description = "Adds an option to use the old action buttons, and hides old action buttons.",
) {
    dependsOn(
        sharedExtensionPatch,
        versionCheckPatch,
    )

    apply {
        if (!is_21_36_or_greater) return@apply

        // Some buttons of the old action bar cannot be identified by a litho filter.
        decodeElementMethod.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->removeActionBarButtons([B)[B
                move-result-object p1
            """,
        )
    }
}
