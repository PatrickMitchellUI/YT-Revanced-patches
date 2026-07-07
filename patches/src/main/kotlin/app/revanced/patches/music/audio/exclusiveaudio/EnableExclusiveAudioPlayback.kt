package app.revanced.patches.music.audio.exclusiveaudio

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.misc.extension.sharedExtensionPatch
import app.revanced.patches.music.misc.settings.settingsPatch
import app.revanced.util.returnEarly

@Suppress("unused")
val enableExclusiveAudioPlaybackPatch = bytecodePatch(
    name = "Enable exclusive audio playback",
    description = "Enables the option to play audio without video.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
    )

    compatibleWith(
        "com.google.android.apps.youtube.music"(
            "7.29.52",
            "8.37.56",
            "8.44.54",
            "8.47.56",
            "9.13.50",
            "9.15.51",
            "9.24.51"
        ),
    )

    apply {
        allowExclusiveAudioPlaybackMethod.returnEarly(true)
    }
}
