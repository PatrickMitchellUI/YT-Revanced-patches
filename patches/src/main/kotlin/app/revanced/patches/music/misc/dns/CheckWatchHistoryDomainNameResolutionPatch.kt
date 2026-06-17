package app.revanced.patches.music.misc.dns

import app.revanced.patches.music.misc.extension.sharedExtensionPatch
import app.revanced.patches.music.shared.mainActivityOnCreateMethod
import app.revanced.patches.shared.misc.dns.checkWatchHistoryDomainNameResolutionPatch

val checkWatchHistoryDomainNameResolutionPatch = checkWatchHistoryDomainNameResolutionPatch(
    block = {
        dependsOn(
            sharedExtensionPatch
        )

        compatibleWith(
            "com.google.android.apps.youtube.music"(
                "7.29.52",
                "8.37.56",
                "8.44.54",
                "8.47.56",
                "9.13.50",
            "9.15.51"
            )
        )
    },

    getMainActivityMethod = { mainActivityOnCreateMethod }
)
