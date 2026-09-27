package app.revanced.patches.youtube.layout.shortsautoplay

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.classDef
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.fieldReference
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playservice.is_21_36_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.mainActivityOnCreateMethod
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/ShortsAutoplayPatch;"

@Suppress("unused")
val shortsAutoplayPatch = bytecodePatch(
    name = "Shorts autoplay",
    description = "Adds options to automatically play the next Short.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        resourceMappingPatch,
        versionCheckPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "20.40.45",
            "21.36.47"
        ),
    )

    apply {
        addResources("youtube", "layout.shortsautoplay.shortsAutoplayPatch")

        PreferenceScreen.SHORTS.addPreferences(
            SwitchPreference("revanced_shorts_autoplay"),
            SwitchPreference("revanced_shorts_autoplay_background"),
        )

        // Main activity is used to check if app is in pip mode.
        mainActivityOnCreateMethod.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->setMainActivity(Landroid/app/Activity;)V",
        )

        var reelEnumClass: String

        reelEnumConstructorMethodMatch.apply {
            reelEnumClass = immutableClassDef.type

            method.addInstructions(
                reelEnumConstructorMethodMatch[-1],
                """
                    # Pass the first enum value to extension.
                    # Any enum value of this type will work.
                    sget-object v0, $reelEnumClass->a:$reelEnumClass
                    invoke-static { v0 }, $EXTENSION_CLASS_DESCRIPTOR->setYTShortsRepeatEnum(Ljava/lang/Enum;)V
                """,
            )
        }

        if (is_21_36_or_greater) {
            getReelLoopBehaviorResolverMethod(reelEnumClass).apply {
                findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructionsAtControlFlowLabel(
                        index,
                        """
                            invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->changeShortsRepeatBehavior(Ljava/lang/Enum;)Ljava/lang/Enum;
                            move-result-object v$register
                            check-cast v$register, $reelEnumClass
                        """,
                    )
                }
            }
        } else {
            restoreShortsAutoplay(reelEnumClass)
        }
    }
}

/**
 * 20.x has no auto advance loop behavior and no code for it,
 * so the single play behavior is used and the 'Autoplay' code is restored.
 */
private fun BytecodePatchContext.restoreShortsAutoplay(reelEnumClass: String) {
    reelPlaybackRepeatParentMethod.immutableClassDef.getReelPlaybackRepeatMethod().apply {
        // The behavior enums are looked up from an ordinal value to an enum type.
        findInstructionIndicesReversedOrThrow {
            val reference = methodReference
            reference?.definingClass == reelEnumClass &&
                reference.parameterTypes.firstOrNull() == "I" &&
                reference.returnType == reelEnumClass
        }.forEach { index ->
            val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

            addInstructions(
                index + 2,
                """
                    invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->changeShortsRepeatBehavior(Ljava/lang/Enum;)Ljava/lang/Enum;
                    move-result-object v$register
                """,
            )
        }

        // The method advances to the next Short in another case.
        // Reuse its user action and sequence controller call.
        val userActionIndex = indexOfFirstInstructionOrThrow {
            val reference = methodReference
            opcode == Opcode.INVOKE_DIRECT && reference?.name == "<init>" &&
                reference.parameterTypes.size == 3 && reference.parameterTypes.first() == "I"
        }
        val userActionMethodReference = getInstruction(userActionIndex).methodReference!!
        val reelSequenceControllerMethodReference = getInstruction(
            indexOfFirstInstructionOrThrow(userActionIndex) {
                val reference = methodReference
                opcode == Opcode.INVOKE_VIRTUAL && reference?.returnType == "I" &&
                    reference.parameterTypes == listOf(userActionMethodReference.definingClass)
            },
        ).methodReference!!
        val getReelSequenceControllerReference = getInstruction<ReferenceInstruction>(
            indexOfFirstInstructionOrThrow {
                val reference = fieldReference
                opcode == Opcode.IGET_OBJECT && reference?.definingClass == definingClass &&
                    reference.type == reelSequenceControllerMethodReference.definingClass
            },
        ).reference

        // Find the first call modified by extension code above.
        val extensionReturnResultIndex = indexOfFirstInstructionOrThrow {
            opcode == Opcode.INVOKE_STATIC && methodReference?.definingClass == EXTENSION_CLASS_DESCRIPTOR
        } + 1
        val enumRegister = getInstruction<OneRegisterInstruction>(extensionReturnResultIndex).registerA

        // Add a helper method to avoid finding multiple free registers.
        // If enum is autoplay then method performs autoplay and returns null,
        // otherwise returns the same enum.
        val helperName = "patch_handleAutoPlay"
        val helperMethod = ImmutableMethod(
            definingClass,
            helperName,
            listOf(ImmutableMethodParameter("Ljava/lang/Enum;", null, null)),
            "Ljava/lang/Enum;",
            AccessFlags.PRIVATE.value,
            null,
            null,
            MutableMethodImplementation(7),
        ).toMutable().apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->isAutoPlay(Ljava/lang/Enum;)Z
                    move-result v0
                    if-eqz v0, :ignore
                    new-instance v0, ${userActionMethodReference.definingClass}
                    const/4 v1, 0x3
                    const/4 v2, 0x0
                    invoke-direct { v0, v1, v2, v2 }, $userActionMethodReference
                    iget-object v3, p0, $getReelSequenceControllerReference
                    invoke-virtual { v3, v0 }, $reelSequenceControllerMethodReference
                    const/4 v4, 0x0
                    return-object v4
                    :ignore
                    return-object p1
                """,
            )
        }
        classDef.methods.add(helperMethod)

        addInstructionsWithLabels(
            extensionReturnResultIndex + 1,
            """
                invoke-direct { p0, v$enumRegister }, $definingClass->$helperName(Ljava/lang/Enum;)Ljava/lang/Enum;
                move-result-object v$enumRegister
                if-nez v$enumRegister, :ignore
                return-void     # Autoplay was performed.
                :ignore
                nop
            """,
        )
    }
}
