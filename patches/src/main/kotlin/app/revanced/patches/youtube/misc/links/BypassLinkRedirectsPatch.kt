package app.revanced.patches.youtube.misc.links

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/BypassLinkRedirectsPatch;"

@Suppress("unused")
val bypassLinkRedirectsPatch =
    bytecodePatch(
        name = "Bypass link redirects",
        description = "Adds an option to bypass link redirects and open the original link directly.",
    ) {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
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
            addResources("youtube", "misc.links.bypassLinkRedirectsPatch")

            PreferenceScreen.MISC.addPreferences(
                SwitchPreference("revanced_bypass_link_redirects"),
            )

            fun patchLogic(register: String) =
                """
                invoke-static { $register }, $EXTENSION_CLASS_DESCRIPTOR->parseRedirectUri(Landroid/net/Uri;)Landroid/net/Uri;
                move-result-object $register
            """

            // Override the URI opened from a video comment.
            commentUriIntentMethodMatch.let {
                val insertIndex = it[1]
                val uriRegister =
                    it.method.getInstruction<FiveRegisterInstruction>(insertIndex).registerE

                it.method.addInstructions(insertIndex, patchLogic("v$uriRegister"))
            }

            // Override the URI opened from a video description or a community post.
            descriptionUriIntentMethod.addInstructions(0, patchLogic("p1"))
        }
    }
