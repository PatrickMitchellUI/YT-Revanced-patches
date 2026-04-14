package app.revanced.patches.youtube.misc.fix.pipchatbar

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/youtube/patches/FixPipChatBarPatch;"

internal val fixPipChatBarPatch = bytecodePatch(
    description = "Hides the bar that appears over the PiP video after using live chat text entry.",
) {
    dependsOn(sharedExtensionPatch)

    apply {
        pipModeChangedMethod.addInstruction(
            0,
            "invoke-static { p0, p1 }, $EXTENSION_CLASS_DESCRIPTOR->onPipModeChanged(Landroid/app/Activity;Z)V",
        )
    }
}
