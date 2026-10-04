package app.revanced.patches.youtube.video.audio

import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.audio.forceOriginalAudioPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playservice.is_21_36_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.mainActivityOnCreateMethod

@Suppress("unused")
val forceOriginalAudioPatch = forceOriginalAudioPatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            versionCheckPatch,
        )

        compatibleWith(
            "com.google.android.youtube"(
                "20.40.45",
                "21.36.47"
            ),
        )
    },
    // Flag was removed in 21.36.
    fixUseLocalizedAudioTrackFlag = { !is_21_36_or_greater },
    getMainActivityOnCreateMethod = BytecodePatchContext::mainActivityOnCreateMethod::get,
    subclassExtensionClassDescriptor = "Lapp/revanced/extension/youtube/patches/ForceOriginalAudioPatch;",
    preferenceScreen = PreferenceScreen.VIDEO,
)
