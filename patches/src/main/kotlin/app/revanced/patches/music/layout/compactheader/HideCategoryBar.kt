package app.revanced.patches.music.layout.compactheader

import app.revanced.patcher.extensions.addInstruction
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
    "Lapp/revanced/extension/music/patches/HideCategoryBarPatch;"

@Suppress("unused")
val hideCategoryBarPatch =
    bytecodePatch(
        name = "Hide category bar",
        description = "Adds an option to hide the category bar at the top of the homepage.",
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
                "9.13.50",
                "9.15.51",
                "9.24.51",
            ),
        )

        apply {
            addResources("music", "layout.compactheader.hideCategoryBarPatch")

            PreferenceScreen.GENERAL.addPreferences(
                SwitchPreference("revanced_music_hide_category_bar", summaryKey = null),
            )

            chipCloudMethodMatch.let {
                val targetIndex = it[-1]
                val targetRegister =
                    it.method.getInstruction<OneRegisterInstruction>(targetIndex).registerA

                it.method.addInstruction(
                    targetIndex + 1,
                    "invoke-static { v$targetRegister }, $EXTENSION_CLASS_DESCRIPTOR->hideCategoryBar(Landroid/view/View;)V",
                )
            }
        }
    }
