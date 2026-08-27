package app.revanced.patches.youtube.layout.buttons.overlay

import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playservice.is_20_28_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.getLayoutConstructorMethodMatch
import app.revanced.patches.youtube.shared.subtitleButtonControllerMethod
import app.revanced.util.findFreeRegister
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstResourceIdOrThrow
import app.revanced.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/HidePlayerOverlayButtonsPatch;"

@Suppress("unused")
val hidePlayerOverlayButtonsPatch =
    bytecodePatch(
        name = "Hide player overlay buttons",
        description =
            "Adds options to hide the player Cast, Autoplay, Captions, Previous & Next buttons, and the player " +
                "control buttons background.",
    ) {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            addResourcesPatch,
            playerOverlayButtonsSettingsPatch,
            resourceMappingPatch, // Used for finding methods.
            versionCheckPatch,
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
            addResources("youtube", "layout.buttons.overlay.hidePlayerOverlayButtonsPatch")

            addPlayerOverlayPreferences(
                noTitleUnsortedPreferenceCategory(
                    SwitchPreference("revanced_hide_autoplay_button", summaryKey = null),
                    SwitchPreference("revanced_hide_captions_button", summaryKey = null),
                    SwitchPreference("revanced_hide_cast_button", summaryKey = null),
                    SwitchPreference("revanced_hide_collapse_button", summaryKey = null),
                    SwitchPreference("revanced_hide_fullscreen_button", summaryKey = null),
                    SwitchPreference("revanced_hide_player_control_buttons_background"),
                    SwitchPreference("revanced_hide_player_previous_next_buttons", summaryKey = null),
                    SwitchPreference("revanced_hide_settings_button", summaryKey = null),
                ),
            )

            // Verify resources exist.
            ResourceType.ID["player_control_next_button_touch_area"]
            ResourceType.ID["player_control_previous_button_touch_area"]
            ResourceType.ID["player_overflow_button"]

            // region Hide player previous/next & settings button.

            getLayoutConstructorMethodMatch().let {
                val insertIndex = it[-1]
                val viewRegister =
                    it.method.getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                it.method.addInstruction(
                    insertIndex,
                    "invoke-static { v$viewRegister }, $EXTENSION_CLASS_DESCRIPTOR" +
                        "->hidePreviousNextButtons(Landroid/view/View;)V",
                )

                it.method.addInstruction(
                    insertIndex,
                    "invoke-static { v$viewRegister }, $EXTENSION_CLASS_DESCRIPTOR" +
                        "->hideSettingsButton(Landroid/view/View;)V",
                )
            }

            // endregion

            // region Hide cast button.

            playerButtonMethodMatch.let {
                it.method.apply {
                    val setVisibilityIndex = it[0]
                    val visibilityRegister = getInstruction<FiveRegisterInstruction>(setVisibilityIndex).registerD

                    addInstructions(
                        setVisibilityIndex,
                        """
                        invoke-static { v$visibilityRegister }, $EXTENSION_CLASS_DESCRIPTOR->hideCastButton(I)I
                        move-result v$visibilityRegister
                    """,
                    )
                }
            }

            if (is_20_28_or_greater) {
                arrayOf(
                    castButtonPlayerFeatureFlagMethodMatch,
                    castButtonActionFeatureFlagMethodMatch, // Cast button in the feed.
                ).forEach { match ->
                    match.method.insertLiteralOverride(
                        match[0],
                        "$EXTENSION_CLASS_DESCRIPTOR->getCastButtonOverride(Z)Z",
                    )
                }
            }

            // endregion

            // region Hide captions button.

            subtitleButtonControllerMethod.apply {
                val insertIndex = indexOfFirstInstructionOrThrow(Opcode.IGET_BOOLEAN) + 1

                addInstruction(
                    insertIndex,
                    "invoke-static { v0 }, $EXTENSION_CLASS_DESCRIPTOR->hideCaptionsButton(Landroid/widget/ImageView;)V",
                )
            }

            // endregion

            // region Hide autoplay button.

            getLayoutConstructorMethodMatch().method.apply {
                val constIndex = indexOfFirstResourceIdOrThrow("autonav_toggle")
                val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA

                // Add a conditional branch around the code that inflates and adds the auto-repeat button.
                val gotoIndex =
                    indexOfFirstInstructionOrThrow(constIndex) {
                        val parameterTypes = methodReference?.parameterTypes
                        opcode == Opcode.INVOKE_VIRTUAL &&
                            parameterTypes?.size == 2 &&
                            parameterTypes.first() == "Landroid/view/ViewStub;"
                    } + 1

                addInstructionsWithLabels(
                    constIndex,
                    """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideAutoplayButton()Z
                    move-result v$constRegister
                    if-nez v$constRegister, :hidden
                """,
                    ExternalLabel("hidden", getInstruction(gotoIndex)),
                )
            }

            // endregion

            // region Hide collapse button.

            titleAnchorMethodMatch.let {
                it.method.apply {
                    val titleAnchorIndex = it[-1]
                    val titleAnchorRegister =
                        getInstruction<OneRegisterInstruction>(titleAnchorIndex).registerA

                    addInstruction(
                        titleAnchorIndex + 1,
                        "invoke-static { v$titleAnchorRegister }, ${EXTENSION_CLASS_DESCRIPTOR}->setTitleAnchorStartMargin(Landroid/view/View;)V",
                    )

                    val playerCollapseButtonIndex = it[1]
                    val playerCollapseButtonRegister =
                        getInstruction<OneRegisterInstruction>(playerCollapseButtonIndex).registerA

                    addInstruction(
                        playerCollapseButtonIndex + 1,
                        "invoke-static { v$playerCollapseButtonRegister }, ${EXTENSION_CLASS_DESCRIPTOR}->hideCollapseButton(Landroid/widget/ImageView;)V",
                    )
                }
            }

            // endregion

            // region Hide fullscreen button.

            fullscreenButtonMethodMatch.let {
                it.method.apply {
                    // Youtube 21.x doesn't cast the fullscreen button to ImageView anymore,
                    // so match on move-result-object after findViewById instead of check-cast.
                    val moveResultIndex = it[2]
                    val insertIndex = moveResultIndex + 1
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(moveResultIndex).registerA

                    addInstructionsWithLabels(
                        insertIndex,
                        """
                        invoke-static { v$insertRegister }, ${EXTENSION_CLASS_DESCRIPTOR}->hideFullscreenButton(Landroid/view/View;)Landroid/view/View;
                        move-result-object v$insertRegister
                        if-nez v$insertRegister, :show
                        return-void
                    """,
                        ExternalLabel("show", getInstruction(insertIndex)),
                    )
                }
            }

            // endregion

            // region Hide player control buttons background.

            inflateControlsGroupLayoutStubMethodMatch.let {
                it.method.apply {
                    val insertIndex = it[-1] + 1
                    val freeRegister = findFreeRegister(insertIndex)

                    addInstructions(
                        insertIndex,
                        """
                        # Move the inflated layout to a temporary register.
                        # The result of the inflate method is by default not moved to a register after the method is called.
                        # 21.21+ now uses the returned inflated view but the changes here still work.
                        move-result-object v$freeRegister
                        invoke-static { v$freeRegister }, $EXTENSION_CLASS_DESCRIPTOR->hidePlayerControlButtonsBackground(Landroid/view/View;)Landroid/view/View;
                    """,
                    )
                }
            }

            // endregion
        }
    }
