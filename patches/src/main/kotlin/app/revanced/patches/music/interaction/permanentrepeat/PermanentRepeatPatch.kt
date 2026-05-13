package app.revanced.patches.music.interaction.permanentrepeat

import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.music.misc.extension.sharedExtensionPatch
import app.revanced.patches.music.misc.settings.PreferenceScreen
import app.revanced.patches.music.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/music/patches/PermanentRepeatPatch;"

@Suppress("unused")
val permanentRepeatPatch = bytecodePatch(
    name = "Permanent repeat",
    description = "Adds an option to always repeat even if the playlist ends or another track is played.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
    )

    compatibleWith(
        "com.google.android.apps.youtube.music"(
            "7.29.52",
            "8.37.56",
            "8.44.54",
            "8.47.56",
            "9.13.50"
        ),
    )

    apply {
        addResources("music", "interaction.permanentrepeat.permanentRepeatPatch")

        PreferenceScreen.PLAYER.addPreferences(
            SwitchPreference("revanced_music_play_permanent_repeat", summaryKey = null),
        )

        repeatTrackMethodMatch.method.apply {
            val startIndex = repeatTrackMethodMatch[-1]
            val moveResultIndex = repeatTrackMethodMatch[4]

            // Start index is at a branch, but the same
            // register is clobbered in both branch paths.
            val targetRegister = getInstruction<OneRegisterInstruction>(moveResultIndex).registerA

            addInstructionsWithLabels(
                startIndex,
                """
                    if-nez v$targetRegister, :skip_override
                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->permanentRepeat()Z
                    move-result v$targetRegister
                    :skip_override
                    nop
                """
            )
        }
    }
}
