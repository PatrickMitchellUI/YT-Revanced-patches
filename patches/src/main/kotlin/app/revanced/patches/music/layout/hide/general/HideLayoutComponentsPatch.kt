package app.revanced.patches.music.layout.hide.general

import app.revanced.patches.music.misc.litho.filter.lithoFilterPatch
import app.revanced.patches.music.misc.settings.PreferenceScreen
import app.revanced.patches.music.misc.settings.settingsPatch
import app.revanced.patches.shared.layout.hide.general.hideLayoutComponentsPatch

val hideLayoutComponentsPatch = hideLayoutComponentsPatch(
    lithoFilterPatch = lithoFilterPatch,
    settingsPatch = settingsPatch,
    generalSettingsScreen = PreferenceScreen.GENERAL,
    filterClasses = setOf("Lapp/revanced/extension/shared/patches/litho/CustomFilter;"),
    compatibleWithPackages = arrayOf(
        "com.google.android.apps.youtube.music" to setOf(
            "7.29.52",
            "8.37.56",
            "8.44.54",
            "8.47.56",
            "9.13.50",
            "9.15.51"
        )
    ),
)
