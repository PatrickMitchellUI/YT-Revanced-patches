package app.revanced.patches.youtube.layout.player.fullscreen

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.findFreeRegister
import app.revanced.util.toPublicAccessFlags

@Suppress("unused")
val disableFullscreenGesturesPatch =
    bytecodePatch(
        name = "Disable fullscreen gestures",
        description = "Adds options to selectively disable gestures for entering and exiting fullscreen mode.",
    ) {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            addResourcesPatch,
        )

        compatibleWith(
            "com.google.android.youtube"(
                "20.14.43",
                "20.21.37",
                "20.26.46",
                "20.31.42",
                "20.37.48",
                "20.45.36",
                "20.51.39",
            ),
        )

        // Cannot declare as top level since this patch is in the same package as
        // other patches that declare same constant name with internal visibility.
        @Suppress("LocalVariableName")
        val EXTENSION_CLASS_DESCRIPTOR =
            "Lapp/revanced/extension/youtube/patches/DisableFullscreenGesturesPatch;"

        apply {
            addResources("youtube", "layout.player.fullscreen.disableFullscreenGesturesPatch")

            PreferenceScreen.PLAYER.addPreferences(
                PreferenceScreenPreference(
                    key = "revanced_disable_fullscreen_gestures",
                    sorting = Sorting.UNSORTED,
                    preferences =
                        setOf(
                            SwitchPreference("revanced_disable_fullscreen_pulled_up_gesture", summaryKey = null),
                            SwitchPreference("revanced_disable_fullscreen_dragged_down_gesture", summaryKey = null),
                            SwitchPreference("revanced_disable_fullscreen_sliding_down_gesture", summaryKey = null),
                        ),
                ),
            )

            val playerDragGestureTypeMethod = playerDragGestureTypeMethodMatch.method
            playerDragGestureTypeMethodMatch.classDef.methods.apply {
                removeIf { it != playerDragGestureTypeMethod }
                playerDragGestureTypeMethod.accessFlags = playerDragGestureTypeMethod.accessFlags.toPublicAccessFlags()
            }

            playerDragGestureInitMethodMatch.method.apply {
                val index = playerDragGestureInitMethodMatch[-1]
                val free = findFreeRegister(index)

                addInstructionsAtControlFlowLabel(
                    index,
                    """
                    invoke-static { p4 }, ${playerDragGestureTypeMethod.definingClass}->${playerDragGestureTypeMethod.name}(I)Ljava/lang/String;
                    move-result-object v$free
                    invoke-static { v$free }, $EXTENSION_CLASS_DESCRIPTOR->disableFullscreenGestures(Ljava/lang/String;)Z
                    move-result v$free
                    if-eqz v$free, :disable_fullscreen_gesture
                    const/4 v$free, 0x0
                    return v$free
                    :disable_fullscreen_gesture
                    nop
                """,
                )
            }
        }
    }
