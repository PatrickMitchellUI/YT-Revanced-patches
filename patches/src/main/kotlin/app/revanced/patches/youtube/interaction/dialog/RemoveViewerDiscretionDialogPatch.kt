package app.revanced.patches.youtube.interaction.dialog

import app.revanced.patches.shared.interaction.dialog.removeViewerDiscretionDialogPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch

@Suppress("unused")
val removeViewerDiscretionDialogPatch =
    removeViewerDiscretionDialogPatch(
        settingsPatch,
        sharedExtensionPatch,
        preferenceScreen = PreferenceScreen.GENERAL,
        "com.google.android.youtube" to
            setOf(
                "20.14.43",
                "20.21.37",
                "20.26.46",
                "20.31.42",
                "20.37.48",
                "20.45.36",
                "20.51.39",
            ),
    )
