package app.revanced.patches.youtube.interaction.copyvideolink

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import app.revanced.patches.youtube.layout.buttons.overlay.addPlayerOverlayPreferences
import app.revanced.patches.youtube.layout.buttons.overlay.playerOverlayButtonsSettingsPatch
import app.revanced.patches.youtube.misc.playercontrols.addBottomControl
import app.revanced.patches.youtube.misc.playercontrols.initializeBottomControl
import app.revanced.patches.youtube.misc.playercontrols.injectVisibilityCheckCall
import app.revanced.patches.youtube.misc.playercontrols.playerControlsPatch
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources

private val copyVideoLinkButtonResourcePatch =
    resourcePatch {
        dependsOn(
            settingsPatch,
            playerControlsPatch,
        )

        apply {
            copyResources(
                "copyvideolinkbutton",
                ResourceGroup(
                    resourceDirectoryName = "drawable",
                    "revanced_yt_copy.xml",
                ),
            )

            addBottomControl("copyvideolinkbutton")
        }
    }

@Suppress("unused")
val copyVideoLinkButtonPatch =
    bytecodePatch(
        name = "Copy video link",
        description = "Adds an option to display a button in the video player to copy video links.",
    ) {
        dependsOn(
            copyVideoLinkButtonResourcePatch,
            playerOverlayButtonsSettingsPatch,
            playerControlsPatch,
            videoInformationPatch,
            addResourcesPatch,
        )

        compatibleWith(
            "com.google.android.youtube"(
                "20.14.43",
                "20.21.37",
                "20.26.46",
                "20.31.42",
                "20.37.48",
                "20.45.36",
                "20.51.39",
            ),
        )

        apply {
            addResources("youtube", "interaction.copyvideelink.copyVideoLinkButtonPatch")

            addPlayerOverlayPreferences(
                noTitleUnsortedPreferenceCategory(
                    SwitchPreference("revanced_copy_video_link_button"),
                    SwitchPreference("revanced_copy_video_link_with_timestamp_button"),
                ),
            )

            val buttonDescriptor = "Lapp/revanced/extension/youtube/videoplayer/CopyVideoLinkButton;"

            initializeBottomControl(buttonDescriptor)
            injectVisibilityCheckCall(buttonDescriptor)
        }
    }
