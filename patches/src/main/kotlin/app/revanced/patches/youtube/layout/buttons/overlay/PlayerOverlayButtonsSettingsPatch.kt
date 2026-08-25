package app.revanced.patches.youtube.layout.buttons.overlay

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.BasePreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch

// Use an initially null field so an exception is thrown if this patch was not included.
private var playerOverlayPreferences: MutableSet<BasePreference>? = null

internal fun addPlayerOverlayPreferences(vararg preferences: BasePreference) {
    playerOverlayPreferences!!.addAll(preferences)
}

internal val playerOverlayButtonsSettingsPatch =
    bytecodePatch {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            addResourcesPatch,
        )

        apply {
            playerOverlayPreferences = mutableSetOf()
        }

        afterDependents {
            if (playerOverlayPreferences!!.isNotEmpty()) {
                PreferenceScreen.PLAYER.addPreferences(
                    PreferenceScreenPreference(
                        key = "revanced_overlay_buttons_screen",
                        preferences = playerOverlayPreferences!!,
                    ),
                )
            }

            playerOverlayPreferences = null
        }
    }
