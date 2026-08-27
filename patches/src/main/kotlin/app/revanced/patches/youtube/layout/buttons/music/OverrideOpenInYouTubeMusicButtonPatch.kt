package app.revanced.patches.youtube.layout.buttons.music

import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.asSequence
import app.revanced.util.forEachInstructionAsSequence
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import org.w3c.dom.Element

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/OverrideOpenInYouTubeMusicButtonPatch;"
private val overrideOpenInYouTubeMusicManifestResourcePatch =
    resourcePatch {
        apply {
            document("AndroidManifest.xml").use { document ->
                if (document
                        .getElementsByTagName("uses-permission")
                        .asSequence()
                        .filterIsInstance<Element>()
                        .none { it.getAttribute("android:name") == "android.permission.QUERY_ALL_PACKAGES" }
                ) {
                    document
                        .createElement("uses-permission")
                        .apply {
                            setAttribute("android:name", "android.permission.QUERY_ALL_PACKAGES")
                        }.let(document.documentElement::appendChild)
                }
            }
        }
    }

@Suppress("unused")
val overrideOpenInYouTubeMusicButtonPatch =
    bytecodePatch(
        name = "Override 'Open in YouTube Music' button",
        description =
            "Adds an option to override the button to open YouTube Music under a different package name.",
    ) {
        dependsOn(
            settingsPatch,
            addResourcesPatch,
            overrideOpenInYouTubeMusicManifestResourcePatch,
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
            addResources("youtube", "layout.buttons.music.overrideOpenInYouTubeMusicButtonPatch")

            PreferenceScreen.GENERAL.addPreferences(
                noTitleUnsortedPreferenceCategory(
                    SwitchPreference(key = "revanced_override_open_in_youtube_music_button"),
                    SwitchPreference(key = "revanced_override_open_in_youtube_music_button_target_package_name"),
                ),
            )

            forEachInstructionAsSequence({ _, _, instruction, index ->
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachInstructionAsSequence null
                val reference = instruction.methodReference ?: return@forEachInstructionAsSequence null
                if (reference.definingClass != "Landroid/content/Intent;") return@forEachInstructionAsSequence null

                when (reference.name) {
                    "setPackage" if reference.parameterTypes == listOf("Ljava/lang/String;") -> {
                        index to "overrideSetPackage(Landroid/content/Intent;Ljava/lang/String;)Landroid/content/Intent;"
                    }

                    "setData" if reference.parameterTypes == listOf("Landroid/net/Uri;") -> {
                        index to "overrideSetData(Landroid/content/Intent;Landroid/net/Uri;)Landroid/content/Intent;"
                    }

                    else -> {
                        null
                    }
                }
            }) { method, (index, methodDescriptor) ->
                if (method.definingClass == EXTENSION_CLASS_DESCRIPTOR) return@forEachInstructionAsSequence

                val invokeString =
                    when (val instruction = method.getInstruction(index)) {
                        is RegisterRangeInstruction -> {
                            "invoke-static/range { v${instruction.startRegister} .. v${instruction.startRegister + instruction.registerCount - 1} }"
                        }

                        is FiveRegisterInstruction -> {
                            "invoke-static { v${instruction.registerC}, v${instruction.registerD} }"
                        }

                        else -> {
                            return@forEachInstructionAsSequence
                        }
                    }

                method.replaceInstruction(
                    index,
                    "$invokeString, $EXTENSION_CLASS_DESCRIPTOR->$methodDescriptor",
                )
            }
        }
    }
