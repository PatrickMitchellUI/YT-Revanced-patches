package app.revanced.patches.youtube.video.quality

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.settings.preference.BasePreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceCategory
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.revanced.patches.youtube.misc.playservice.is_21_36_or_greater
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.util.insertLiteralOverride

/**
 * Video quality settings. Used to organize all speed related settings together.
 */
internal val settingsMenuVideoQualityGroup = mutableSetOf<BasePreference>()

@Suppress("unused")
val videoQualityPatch = bytecodePatch(
    name = "Video quality",
    description = "Adds options to set default video qualities and always use the advanced video quality menu.",
) {
    dependsOn(
        rememberVideoQualityPatch,
        advancedVideoQualityMenuPatch,
        hidePremiumVideoQualityPatch,
        videoQualityDialogButtonPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "20.40.45",
            "21.36.47"
        ),
    )

    apply {
        PreferenceScreen.VIDEO.addPreferences(
            // Keep the preferences organized together.
            PreferenceCategory(
                key = "revanced_01_video_key", // Dummy key to force the quality preferences first.
                titleKey = null,
                sorting = Sorting.UNSORTED,
                tag = "app.revanced.extension.shared.settings.preference.NoTitlePreferenceCategory",
                preferences = settingsMenuVideoQualityGroup,
            ),
        )

        // 21.36 reads the flag elsewhere and is handled by the advanced video quality menu patch.
        if (!is_21_36_or_greater) {
            // Flag breaks opening advanced quality menu.
            // Alternatively can be fixed by using a delay when simulating the UI click.
            newAdvancedQualityMenuStyleFlyoutMethodMatch.let {
                it.method.insertLiteralOverride(it[0], false)
            }
        }
    }
}
