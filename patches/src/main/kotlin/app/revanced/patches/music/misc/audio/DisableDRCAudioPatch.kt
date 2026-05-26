package app.revanced.patches.music.misc.audio

import app.revanced.patches.music.misc.extension.sharedExtensionPatch
import app.revanced.patches.music.misc.settings.PreferenceScreen
import app.revanced.patches.music.misc.settings.settingsPatch
import app.revanced.patches.music.playservice.is_9_19_or_greater
import app.revanced.patches.music.playservice.is_9_20_or_greater
import app.revanced.patches.music.playservice.versionCheckPatch
import app.revanced.patches.shared.misc.audio.drc.disableDRCAudioPatch

@Suppress("unused")
val disableDRCAudioPatch = disableDRCAudioPatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            versionCheckPatch,
        )

        compatibleWith(
            "com.google.android.apps.youtube.music"(
                "7.29.52",
                "8.37.56",
                "8.44.54",
                "8.47.56",
                "9.13.50",
                "9.15.51",
                "9.24.51",
            ),
        )
    },
    preferenceScreen = PreferenceScreen.MISC,
    useLegacyNormalizationFlag = { !is_9_19_or_greater },
    // Ignore 9.19 because it's missing a flag and requires version specific changes to support.
    useNormalizationFlag = { is_9_20_or_greater },
)
