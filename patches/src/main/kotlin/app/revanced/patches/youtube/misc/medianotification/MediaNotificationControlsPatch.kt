package app.revanced.patches.youtube.misc.medianotification

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/MediaNotificationControlsPatch;"

@Suppress("unused")
val mediaNotificationControlsPatch = bytecodePatch(
    name = "Media notification controls",
    description = "Adds options to disable the seekbar and previous/next buttons in the " +
            "media notification and headphone controls.",
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
            "20.51.39"
        ),
    )

    apply {
        addResources("youtube", "misc.medianotification.mediaNotificationControlsPatch")

        PreferenceScreen.PLAYER.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_notification_media_screen",
                preferences = setOf(
                    SwitchPreference("revanced_hide_notification_media_prev_next"),
                    SwitchPreference("revanced_disable_notification_media_seekbar"),
                ),
            ),
        )

        mediaSessionSetPlaybackStateMethodMatch.let {
            it.method.apply {
                val setPlaybackStateIndex = it[0]
                val register = getInstruction<FiveRegisterInstruction>(setPlaybackStateIndex).registerD

                addInstructions(
                    setPlaybackStateIndex,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->changePlaybackState(Landroid/media/session/PlaybackState;)Landroid/media/session/PlaybackState;
                        move-result-object v$register
                    """,
                )
            }
        }
    }
}
