package app.revanced.patches.youtube.interaction.reload

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.classDef
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.layout.buttons.overlay.addPlayerOverlayPreferences
import app.revanced.patches.youtube.layout.buttons.overlay.playerOverlayButtonsSettingsPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playercontrols.addTopControl
import app.revanced.patches.youtube.misc.playercontrols.initializeTopControl
import app.revanced.patches.youtube.misc.playercontrols.injectVisibilityCheckCall
import app.revanced.patches.youtube.misc.playercontrols.playerControlsPatch
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.mainActivityOnCreateMethod
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.getMethodReferenceAtMatchedInstructionIndex
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil

private val reloadVideoButtonResourcePatch =
    resourcePatch {
        dependsOn(
            settingsPatch,
            playerControlsPatch,
        )

        apply {
            copyResources(
                "reloadbutton",
                ResourceGroup(
                    "drawable",
                    "revanced_reload_video_button.xml",
                ),
            )
        }

        afterDependents {
            addTopControl(
                "reloadbutton",
                "@+id/revanced_reload_video_button",
                "@+id/revanced_reload_video_button",
            )
        }
    }

private const val EXTENSION_BUTTON_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/videoplayer/ReloadVideoButton;"

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/LoadVideoPatch;"

private const val EXTENSION_PLAYER_INTERFACE_CLASS_DESCRIPTOR =
    $$"Lapp/revanced/extension/youtube/patches/LoadVideoPatch$PlayerInterface;"

@Suppress("unused")
val reloadVideoButtonPatch =
    bytecodePatch(
        name = "Reload video",
        description = "Adds options to display buttons in the video player to reload video.",
    ) {
        dependsOn(
            reloadVideoButtonResourcePatch,
            sharedExtensionPatch,
            playerControlsPatch,
            videoInformationPatch,
            playerOverlayButtonsSettingsPatch,
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
            addResources("youtube", "interaction.reload.reloadVideoButtonPatch")

            addPlayerOverlayPreferences(
                SwitchPreference("revanced_reload_video_button"),
            )

            initializeTopControl(EXTENSION_BUTTON_DESCRIPTOR)
            injectVisibilityCheckCall(EXTENSION_BUTTON_DESCRIPTOR)

            // Main activity is used to launch downloader intent.
            mainActivityOnCreateMethod.addInstruction(
                0,
                "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->setMainActivity(Landroid/app/Activity;)V",
            )

            val dismissPlayerInnerMethod =
                miniAppOpenYtContentCommandEndpointMethodMatch.getMethodReferenceAtMatchedInstructionIndex(2)
            val openNewVideoParcelableMethod = openNewVideoIntentParcelableMethod
            val openNewVideoParcelableDefiningClass = openNewVideoParcelableMethod.definingClass

            dismissPlayerInnerMethod.classDef.apply {
                // Add interface and helper methods to allow extension code to call obfuscated methods.
                interfaces.add(EXTENSION_PLAYER_INTERFACE_CLASS_DESCRIPTOR)
                // Add methods to access obfuscated player methods.
                methods.add(
                    ImmutableMethod(
                        dismissPlayerInnerMethod.definingClass,
                        "patch_dismissPlayer",
                        listOf(),
                        "V",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(2),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                            invoke-virtual { p0 }, $dismissPlayerInnerMethod
                            return-void
                        """,
                        )
                    },
                )

                methods.add(
                    ImmutableMethod(
                        dismissPlayerInnerMethod.definingClass,
                        "patch_getIntentParcelable",
                        listOf(ImmutableMethodParameter("Landroid/content/Intent;", null, null)),
                        "Landroid/os/Parcelable;",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                            invoke-static { p1 }, $openNewVideoParcelableDefiningClass->${openNewVideoParcelableMethod.name}(Landroid/content/Intent;)$openNewVideoParcelableDefiningClass
                            move-result-object v0
                            return-object v0
                        """,
                        )
                    },
                )

                methods
                    .single { method ->
                        MethodUtil.isConstructor(method)
                    }.apply {
                        val index = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_VOID)

                        addInstruction(
                            index,
                            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->initialize($EXTENSION_PLAYER_INTERFACE_CLASS_DESCRIPTOR)V",
                        )
                    }
            }
        }
    }
