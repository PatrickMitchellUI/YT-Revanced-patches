package app.revanced.patches.youtube.interaction.copyvideourl

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.playercontrols.addBottomControl
import app.revanced.patches.youtube.misc.playercontrols.initializeBottomControl
import app.revanced.patches.youtube.misc.playercontrols.injectVisibilityCheckCall
import app.revanced.patches.youtube.misc.playercontrols.playerControlsPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources

private val copyVideoURLResourcePatch = resourcePatch {
    dependsOn(
        settingsPatch,
        playerControlsPatch,
        addResourcesPatch,
    )

    apply {
        addResources("youtube", "interaction.copyvideourl.copyVideoURLResourcePatch")

        PreferenceScreen.PLAYER.addPreferences(
            SwitchPreference("revanced_copy_video_url"),
            SwitchPreference("revanced_copy_video_url_timestamp"),
        )

        copyResources(
            "copyvideourl",
            ResourceGroup(
                resourceDirectoryName = "drawable",
                "revanced_yt_copy.xml",
            ),
        )

        addBottomControl("copyvideourl")
    }
}

@Suppress("unused")
val copyVideoURLPatch = bytecodePatch(
    name = "Copy video URL",
    description = "Adds an option to display a button in the video player to copy the video URL.",
) {
    dependsOn(
        copyVideoURLResourcePatch,
        playerControlsPatch,
        videoInformationPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "20.14.43",
            "20.21.37",
            "20.26.46",
            "20.31.42",
            "20.37.48",
            "20.45.36",
            "20.47.62"
        ),
    )

    apply {
        val buttonDescriptor = "Lapp/revanced/extension/youtube/videoplayer/CopyVideoURLButton;"

        initializeBottomControl(buttonDescriptor)
        injectVisibilityCheckCall(buttonDescriptor)
    }
}
