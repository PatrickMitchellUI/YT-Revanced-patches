package app.revanced.patches.music.interaction.dialog

import app.revanced.patches.music.misc.extension.sharedExtensionPatch
import app.revanced.patches.music.misc.settings.PreferenceScreen
import app.revanced.patches.music.misc.settings.settingsPatch
import app.revanced.patches.shared.interaction.dialog.removeViewerDiscretionDialogPatch

@Suppress("unused")
val removeViewerDiscretionDialogPatch =
    removeViewerDiscretionDialogPatch(
        settingsPatch,
        sharedExtensionPatch,
        preferenceScreen = PreferenceScreen.GENERAL,
        "com.google.android.apps.youtube.music" to
            setOf(
                "7.29.52",
                "8.37.56",
                "8.44.54",
                "8.47.56",
                "9.13.50",
                "9.15.51",
                "9.24.51",
            ),
    )
