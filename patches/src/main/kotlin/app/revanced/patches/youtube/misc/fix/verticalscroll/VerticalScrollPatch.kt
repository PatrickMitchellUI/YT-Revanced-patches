package app.revanced.patches.youtube.misc.fix.verticalscroll

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.playservice.is_21_18_or_greater
import app.revanced.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal val verticalScrollPatch =
    bytecodePatch(
        description = "Fixes issues with refreshing the feed when the first component is of type EmptyComponent.",
    ) {

        apply {
            if (is_21_18_or_greater) {
                // Can cause issues with scrolling.
                choreographerPostFrameCallbackFeatureFlagMethodMatch.method.insertLiteralOverride(
                    45782902L,
                    false,
                )
            }

            canScrollVerticallyMethodMatch.let {
                it.method.apply {
                    val moveResultIndex = it[-1]
                    val moveResultRegister = getInstruction<OneRegisterInstruction>(moveResultIndex).registerA

                    val insertIndex = moveResultIndex + 1
                    addInstruction(
                        insertIndex,
                        "const/4 v$moveResultRegister, 0x0",
                    )
                }
            }
        }
    }
