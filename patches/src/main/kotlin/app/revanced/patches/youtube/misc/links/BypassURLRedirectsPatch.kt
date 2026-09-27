package app.revanced.patches.youtube.misc.links

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playservice.is_20_37_or_greater
import app.revanced.patches.youtube.misc.playservice.is_20_49_or_greater
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/BypassURLRedirectsPatch;"

@Suppress("unused")
val bypassURLRedirectsPatch = bytecodePatch(
    name = "Bypass URL redirects",
    description = "Adds an option to bypass URL redirects and open the original URL directly.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "20.40.45",
            "21.36.47"
        ),
    )

    apply {
        addResources("youtube", "misc.links.bypassURLRedirectsPatch")

        PreferenceScreen.MISC.addPreferences(
            SwitchPreference("revanced_bypass_url_redirects"),
        )

        if (is_20_49_or_greater) {
            // The redirect link is no longer parsed from the endpoint.
            // Replace it where it is opened in the browser.
            fun MutableMethod.replaceUriParameter(register: String) = addInstructions(
                0,
                """
                    invoke-virtual { $register }, Landroid/net/Uri;->toString()Ljava/lang/String;
                    move-result-object v0
                    invoke-static { v0 }, $EXTENSION_CLASS_DESCRIPTOR->parseRedirectUri(Ljava/lang/String;)Landroid/net/Uri;
                    move-result-object $register
                """,
            )

            // Browser.
            launchBrowserIntentForUriMethod.replaceUriParameter("p1")

            // Custom tab.
            customTabsIntentMethodMatch.classDef.methods.filter { method ->
                AccessFlags.PUBLIC.isSet(method.accessFlags) &&
                    !AccessFlags.STATIC.isSet(method.accessFlags) &&
                    method.returnType == "Z" &&
                    method.parameterTypes == listOf("Landroid/content/Context;", "Landroid/net/Uri;")
            }.also { methods ->
                if (methods.isEmpty()) throw PatchException("Could not find the custom tab methods")
            }.forEach { it.replaceUriParameter("p2") }
        } else {
            val match = if (is_20_37_or_greater) abUriParserMethodMatch else abUriParserLegacyMethodMatch
            val insertIndex = match[2]
            val uriStringRegister =
                match.method.getInstruction<FiveRegisterInstruction>(insertIndex).registerC

            match.method.replaceInstruction(
                insertIndex,
                "invoke-static { v$uriStringRegister }, $EXTENSION_CLASS_DESCRIPTOR->" +
                        "parseRedirectUri(Ljava/lang/String;)Landroid/net/Uri;",
            )
        }
    }
}
