package app.revanced.patches.youtube.misc.privacy

import app.revanced.patches.shared.misc.privacy.sanitizeSharingLinksPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch

@Suppress("unused")
val sanitizeSharingLinksPatch = sanitizeSharingLinksPatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
        )

        compatibleWith(
            "com.google.android.youtube"(
                "20.40.45",
                "21.36.47"
            )
        )
    },
    preferenceScreen = PreferenceScreen.MISC
)
