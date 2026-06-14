package app.revanced.patches.youtube.layout.shortsplayer

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.ListPreference
import app.revanced.patches.youtube.interaction.reload.reloadVideoButtonPatch
import app.revanced.patches.youtube.layout.player.fullscreen.openVideosFullscreenHookPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.misc.playservice.is_21_20_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.mainActivityOnCreateMethod
import app.revanced.patches.youtube.video.information.playbackStartDescriptorToStringMethodMatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/OpenShortsInRegularPlayerPatch;"

@Suppress("unused")
val openShortsInRegularPlayerPatch = bytecodePatch(
    name = "Open Shorts in regular player",
    description = "Adds options to open Shorts in the regular video player.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        openVideosFullscreenHookPatch,
        reloadVideoButtonPatch,
        navigationBarHookPatch,
        versionCheckPatch,
        resourceMappingPatch,
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

    apply {
        addResources("youtube", "layout.shortsplayer.shortsPlayerTypePatch")

        PreferenceScreen.SHORTS.addPreferences(
            ListPreference("revanced_shorts_player_type"),
        )

        // Activity is used as the context to launch an Intent.
        mainActivityOnCreateMethod.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->" +
                    "setMainActivity(Landroid/app/Activity;)V",
        )


        val playbackStartVideoIdMethod = playbackStartDescriptorToStringMethodMatch.let {
            navigate(it.method).to(it[1]).original()
        }

        (if (is_21_20_or_greater) shortsPlaybackIntentMethod else shortsPlaybackIntentLegacyMethod).addInstructionsWithLabels(
            0,
            """
                move-object/from16 v0, p1
                
                invoke-virtual { v0 }, $playbackStartVideoIdMethod
                move-result-object v1
                invoke-static { v1 }, ${EXTENSION_CLASS_DESCRIPTOR}->openShort(Ljava/lang/String;)Z
                move-result v1
                if-eqz v1, :disabled
                return-void
                
                :disabled
                nop
            """
        )
    }
}
