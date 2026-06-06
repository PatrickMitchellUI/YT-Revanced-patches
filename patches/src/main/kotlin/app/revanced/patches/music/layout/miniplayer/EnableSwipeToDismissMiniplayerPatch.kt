package app.revanced.patches.music.layout.miniplayer

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.*
import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.fieldReference
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.typeReference
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.music.misc.extension.sharedExtensionPatch
import app.revanced.patches.music.misc.settings.PreferenceScreen
import app.revanced.patches.music.misc.settings.settingsPatch
import app.revanced.patches.music.playservice.is_9_00_or_greater
import app.revanced.patches.music.playservice.is_9_03_or_greater
import app.revanced.patches.music.playservice.versionCheckPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.findFreeRegister
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/music/patches/EnableSwipeToDismissMiniplayerPatch;"

@Suppress("unused")
val enableSwipeToDismissMiniplayerPatch = bytecodePatch(
    name = "Enable swipe to dismiss miniplayer",
    description = "Adds an option to enable dismissing the miniplayer by swiping down on it.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        versionCheckPatch,
        addResourcesPatch,
    )

    compatibleWith(
        "com.google.android.apps.youtube.music"(
            "7.29.52",
            "8.37.56",
            "8.44.54",
            "8.47.56",
            "9.13.50",
        ),
    )

    apply {
        addResources("music", "layout.miniplayer.enableSwipeToDismissMiniplayer")

        PreferenceScreen.PLAYER.addPreferences(
            SwitchPreference("revanced_music_enable_swipe_to_dismiss_miniplayer"),
        )

        // region Dismiss miniplayer by swiping down

        val interactionLoggingSwipeField = interactionLoggingEnumMethod.let {
            it.method.getInstruction(it[1]).fieldReference!!
        }

        val widgetReferences: List<Reference> = listOf(1, 2, 3, 5, 7, 8, 9).map { i ->
            musicActivityWidgetMethod.method
                .getInstruction<ReferenceInstruction>(musicActivityWidgetMethod[i]).reference
        }

        val musicActivityPeerClass = (widgetReferences[0] as FieldReference).definingClass

        val onWatchWhileDismissed = firstMethodComposite {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            parameterTypes()
            returnType("V")
            instructions(
                allOf(
                    Opcode.IGET_OBJECT(),
                    field {
                        definingClass == musicActivityPeerClass &&
                            type == "Ljava/util/concurrent/atomic/AtomicBoolean;"
                    }
                ),
                afterAtMost(
                    3,
                    allOf(
                        Opcode.INVOKE_VIRTUAL(),
                        method {
                            definingClass == "Ljava/util/concurrent/atomic/AtomicBoolean;" &&
                                name == "set" &&
                                returnType == "V" &&
                                parameterTypes.size == 1 && parameterTypes.first().toString() == "Z"
                        }
                    )
                )
            )
        }

        val helperMethod = ImmutableMethod(
            onWatchWhileDismissed.classDef.type,
            "patch_swipeToDismissMiniplayer",
            listOf(
                ImmutableMethodParameter(musicActivityPeerClass, null, null),
            ),
            "V",
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(5),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    iget-object v0, p0, ${widgetReferences[0]}
                    invoke-interface { v0 }, ${widgetReferences[1]}
                    move-result-object v0
                    check-cast v0, ${widgetReferences[2]}
                    sget-object v1, $interactionLoggingSwipeField
                    new-instance v2, ${widgetReferences[3]}
                    const v3, 0x878b
                    invoke-static { v3 }, ${widgetReferences[4]}
                    move-result-object v3
                    invoke-direct { v2, v3 }, ${widgetReferences[5]}
                    const/4 v3, 0x0
                    invoke-interface { v0, v1, v2, v3 }, ${widgetReferences[6]}
                    return-void
                """,
            )
        }
        onWatchWhileDismissed.classDef.methods.add(helperMethod)

        onWatchWhileDismissed.method.apply {
            val insertIndex = onWatchWhileDismissed[0]
            val peerRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerB
            val freeRegister = findFreeRegister(insertIndex, peerRegister)

            addInstructionsAtControlFlowLabel(
                insertIndex,
                """
                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->enableSwipeToDismissMiniplayer()Z
                    move-result v$freeRegister
                    if-nez v$freeRegister, :dismiss
                    invoke-static { v$peerRegister }, $helperMethod
                    return-void
                    :dismiss
                    nop
                """,
            )
        }

        // endregion

        // region Hide cold start miniplayer text (R.string.mini_player_default_text)

        val coldStartMiniPlayerDefaultText = if (is_9_03_or_greater) {
            val defaultTextMethod = miniPlayerDefaultTextMethod

            firstMethodComposite {
                accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
                parameterTypes("Ljava/lang/Object;")
                returnType("V")
                instructions(
                    Opcode.IF_NE(),
                    allOf(
                        Opcode.INVOKE_VIRTUAL(),
                        method {
                            definingClass == defaultTextMethod.definingClass &&
                                name == defaultTextMethod.name
                        }
                    )
                )
            }
        } else {
            miniPlayerDefaultTextLegacyMethod
        }

        coldStartMiniPlayerDefaultText.method.apply {
            val insertIndex = coldStartMiniPlayerDefaultText[0]
            val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerB

            addInstructions(
                insertIndex,
                """
                    invoke-static { v$insertRegister }, $EXTENSION_CLASS_DESCRIPTOR->enableSwipeToDismissMiniplayer(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$insertRegister
                """,
            )
        }

        // endregion

        // region Hide warm start miniplayer text (R.string.mini_player_default_text)

        val watchWhileLayoutClass = watchWhileLayoutMethod.method.definingClass
        val warmStartMiniplayerClass = watchWhileLayoutMethod.method
            .getInstruction(watchWhileLayoutMethod[-1]).typeReference!!.type

        val warmStartMiniplayer = firstMethodComposite {
            definingClass(warmStartMiniplayerClass)
            parameterTypes("Landroid/view/View;", "I")
            instructions(
                allOf(
                    Opcode.IGET_OBJECT(),
                    field { definingClass == watchWhileLayoutClass }
                ),
                afterAtMost(
                    5,
                    allOf(
                        Opcode.INVOKE_VIRTUAL(),
                        method {
                            definingClass == "Lcom/google/android/material/bottomsheet/BottomSheetBehavior;" &&
                                returnType == "V" &&
                                parameterTypes.size == 1 && parameterTypes.first().toString() == "Z"
                        }
                    )
                )
            )
        }

        warmStartMiniplayer.method.apply {
            val insertIndex = warmStartMiniplayer[0]
            val jumpIndex = warmStartMiniplayer[-1] + 1
            val freeRegister = findFreeRegister(insertIndex)

            addInstructionsAtControlFlowLabel(
                insertIndex,
                """
                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->enableSwipeToDismissMiniplayer()Z
                    move-result v$freeRegister
                    if-nez v$freeRegister, :skip
                """,
                ExternalLabel("skip", getInstruction(jumpIndex)),
            )
        }

        // endregion

        // region Disable player page motion event

        if (is_9_00_or_greater) {
            playerPageBehaviorMethod.addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->enableSwipeToDismissMiniplayer()Z
                    move-result v0
                    if-eqz v0, :ignore
                    return-void
                    :ignore
                    nop
                """,
            )
        }

        // endregion
    }
}
