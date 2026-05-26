package app.revanced.patches.youtube.video.audio

import app.revanced.patches.shared.misc.audio.drc.disableDRCAudioPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playservice.is_21_19_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch

@Suppress("unused")
val disableDRCAudioPatch = disableDRCAudioPatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            versionCheckPatch,
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
    },
    preferenceScreen = PreferenceScreen.VIDEO,
    useLegacyNormalizationFlag = { !is_21_19_or_greater },
    useNormalizationFlag = { is_21_19_or_greater },
)
