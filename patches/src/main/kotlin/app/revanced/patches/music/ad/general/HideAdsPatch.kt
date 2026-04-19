package app.revanced.patches.music.ad.general

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.music.misc.extension.sharedExtensionPatch
import app.revanced.patches.music.misc.settings.PreferenceScreen
import app.revanced.patches.music.misc.settings.settingsPatch
import app.revanced.patches.shared.ad.hideFullscreenAdsPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/music/patches/HideAdsPatch;"

@Suppress("unused")
val hideMusicAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Adds options to hide ads such as the fullscreen Premium popup and \"Get Music Premium\" label.",
) {
    dependsOn(
        sharedExtensionPatch,
        hideFullscreenAdsPatch(PreferenceScreen.ADS),
        settingsPatch,
        addResourcesPatch,
    )

    compatibleWith(
        "com.google.android.apps.youtube.music"(
            "7.29.52",
            "8.10.52",
            "8.37.56",
            "8.40.54",
            "8.44.54",
            "8.47.56",
            "9.13.50"
        ),
    )

    apply {
        addResources("music", "ad.general.hideAdsPatch")

        PreferenceScreen.ADS.addPreferences(
            SwitchPreference("revanced_music_hide_get_premium_label"),
        )

        // Hide 'Get Music Premium' label
        hideGetPremiumMethodMatch.let {
            val insertIndex = it[-1]

            it.method.apply {
                val setVisibilityInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)
                val getPremiumViewRegister = setVisibilityInstruction.registerC
                val visibilityRegister = setVisibilityInstruction.registerD

                replaceInstruction(
                    insertIndex,
                    "const/16 v$visibilityRegister, 0x8",
                )

                addInstruction(
                    insertIndex + 1,
                    "invoke-virtual {v$getPremiumViewRegister, v$visibilityRegister}, " +
                            "Landroid/view/View;->setVisibility(I)V",
                )
            }
        }

        membershipSettingsMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->hideGetPremiumLabel()Z
                move-result v0
                if-eqz v0, :show
                const/4 v0, 0x0
                return-object v0
                :show
                nop
            """,
        )
    }
}
