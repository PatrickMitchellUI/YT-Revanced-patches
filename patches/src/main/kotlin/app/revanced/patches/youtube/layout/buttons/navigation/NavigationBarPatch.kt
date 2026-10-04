package app.revanced.patches.youtube.layout.buttons.navigation

import app.revanced.patcher.*
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.fieldReference
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.featureflag.FeatureFlag
import app.revanced.patches.shared.misc.featureflag.featureFlagOverridePatch
import app.revanced.patches.shared.misc.featureflag.overrideBooleanFeatureFlag
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.layout.toolbar.hookToolbar
import app.revanced.patches.youtube.layout.toolbar.toolbarHookPatch
import app.revanced.patches.youtube.misc.contexthook.Endpoint
import app.revanced.patches.youtube.misc.contexthook.addOSNameHook
import app.revanced.patches.youtube.misc.contexthook.hookClientContextPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.navigation.hookNavigationButtonCreated
import app.revanced.patches.youtube.misc.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.misc.playservice.is_19_25_or_greater
import app.revanced.patches.youtube.misc.playservice.is_20_15_or_greater
import app.revanced.patches.youtube.misc.playservice.is_20_31_or_greater
import app.revanced.patches.youtube.misc.playservice.is_21_36_or_greater
import app.revanced.patches.youtube.misc.playservice.is_20_45_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import kotlin.collections.plusAssign

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/NavigationBarPatch;"

val navigationBarPatch = bytecodePatch(
    name = "Navigation bar",
    description = "Adds options to hide and change the bottom navigation bar (such as the Shorts button) "
            + " and the upper navigation toolbar. Patching version 20.21.37 and lower also adds a setting to use a wide searchbar."
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        navigationBarHookPatch,
        versionCheckPatch,
        hookClientContextPatch,
        toolbarHookPatch,
        featureFlagOverridePatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "20.40.45",
            "21.36.47"
        ),
    )

    apply {
        addResources("youtube", "layout.buttons.navigation.navigationBarPatch")

        val preferences = mutableSetOf(
            SwitchPreference("revanced_hide_home_button"),
            SwitchPreference("revanced_hide_shorts_button"),
            SwitchPreference("revanced_hide_create_button"),
            SwitchPreference("revanced_hide_subscriptions_button"),
            SwitchPreference("revanced_hide_notifications_button"),
            SwitchPreference("revanced_switch_create_with_notifications_button"),
            SwitchPreference("revanced_hide_navigation_button_labels"),
            SwitchPreference("revanced_narrow_navigation_buttons"),
        )

        if (is_19_25_or_greater) {
            preferences += SwitchPreference("revanced_disable_translucent_navigation_bar_light")
            preferences += SwitchPreference("revanced_disable_translucent_navigation_bar_dark")

            PreferenceScreen.GENERAL.addPreferences(
                SwitchPreference("revanced_disable_translucent_status_bar")
            )

            if (is_20_15_or_greater && !is_20_45_or_greater) {
                // Feature has not worked well for a while and YT seems to have abandoned this a/b test.
                preferences += SwitchPreference("revanced_navigation_bar_animations")
            }

            if (is_20_31_or_greater) {
                preferences += SwitchPreference("revanced_disable_auto_hide_navigation_bar")
            }
        }

        PreferenceScreen.GENERAL.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_navigation_buttons_screen",
                sorting = Sorting.UNSORTED,
                preferences = preferences
            )
        )


        // Switch create with notifications button.
        addOSNameHook(
            Endpoint.GUIDE,
            "${EXTENSION_CLASS_DESCRIPTOR}->switchCreateWithNotificationButton(Ljava/lang/String;)Ljava/lang/String;",
        )

        // Hide navigation button labels.
        createPivotBarMethodMatch.let {
            it.method.apply {
                val setTextIndex = it[0]
                val targetRegister = getInstruction<FiveRegisterInstruction>(setTextIndex).registerC

                addInstruction(
                    setTextIndex,
                    "invoke-static { v$targetRegister }, " +
                            "$EXTENSION_CLASS_DESCRIPTOR->hideNavigationButtonLabels(Landroid/widget/TextView;)V",
                )
            }
        }

        // Hook navigation button created, in order to hide them.
        hookNavigationButtonCreated(EXTENSION_CLASS_DESCRIPTOR)

        // Force on/off translucent effect on status bar and navigation buttons.
        if (is_19_25_or_greater) {
            overrideBooleanFeatureFlag(
                FeatureFlag.TRANSLUCENT_STATUS_BAR,
                "$EXTENSION_CLASS_DESCRIPTOR->useTranslucentNavigationStatusBar(Z)Z",
            )
            overrideBooleanFeatureFlag(
                FeatureFlag.TRANSLUCENT_NAVIGATION_BUTTONS,
                "$EXTENSION_CLASS_DESCRIPTOR->useTranslucentNavigationButtons(Z)Z",
            )
            overrideBooleanFeatureFlag(
                FeatureFlag.TRANSLUCENT_SYSTEM_NAVIGATION_BAR,
                "$EXTENSION_CLASS_DESCRIPTOR->useTranslucentNavigationButtons(Z)Z",
            )
        }

        if (is_20_15_or_greater) {
            overrideBooleanFeatureFlag(
                FeatureFlag.ANIMATED_NAVIGATION_TABS,
                "$EXTENSION_CLASS_DESCRIPTOR->useAnimatedNavigationButtons(Z)Z",
            )
        }

        // 21.36 builds a hidden bottom bar model if the last feed scroll hid the pivot bar,
        // for example when the player is minimized. Scrolling up then cannot show the pivot bar,
        // because the model is only built again when the player or page changes.
        // Allow showing the pivot bar if the last scroll was up and the pivot bar is not blocked by the player.
        if (is_21_36_or_greater) {
            val shouldShowPivotBarField = bottomBarModelToStringMethodMatch.let {
                it.method.getInstruction<ReferenceInstruction>(it[-1]).reference as FieldReference
            }

            // The method that builds the bottom bar model, used to find the pivot bar controller fields.
            val modelMatch = firstMethodComposite {
                instructions(
                    allOf(Opcode.IGET_BOOLEAN(), field { this == shouldShowPivotBarField }),
                    after(Opcode.IPUT_BOOLEAN()),
                    afterAtMost(3, method { toString() == "Lj\$/util/Optional;->isPresent()Z" }),
                    afterAtMost(6, Opcode.SGET_OBJECT()), // Scroll direction DOWN.
                    after(Opcode.IF_NE()),
                )
            }

            val (pivotBarAllowedField, scrollDirectionField, scrollDirectionDown) = modelMatch.method.let {
                Triple(
                    it.getInstruction<ReferenceInstruction>(modelMatch[0] - 1).reference as FieldReference,
                    it.getInstruction<ReferenceInstruction>(modelMatch[2] - 1).reference as FieldReference,
                    it.getInstruction<ReferenceInstruction>(modelMatch[3]).reference as FieldReference,
                )
            }

            // Show pivot bar method.
            firstClassDef(pivotBarAllowedField.definingClass).methods.single { method ->
                method.returnType == "V" && method.parameterTypes == listOf("Z") &&
                    method.implementation?.instructions?.any { instruction ->
                        instruction.opcode == Opcode.IGET_BOOLEAN &&
                            (instruction as ReferenceInstruction).reference == shouldShowPivotBarField
                    } == true
            }.apply {
                val index = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IGET_BOOLEAN && fieldReference == shouldShowPivotBarField
                }
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                // Registers v1, v3 and v4 are free here.
                addInstructionsWithLabels(
                    index + 1,
                    """
                        iget-object v3, p0, $scrollDirectionField
                        invoke-virtual { v3 }, Lj${'$'}/util/Optional;->isPresent()Z
                        move-result v4
                        if-eqz v4, :original
                        invoke-virtual { v3 }, Lj${'$'}/util/Optional;->get()Ljava/lang/Object;
                        move-result-object v3
                        sget-object v4, $scrollDirectionDown
                        if-eq v3, v4, :original
                        iget-boolean v1, p0, $pivotBarAllowedField
                        if-eqz v1, :original
                        const/4 v$register, 0x1
                    """,
                    ExternalLabel("original", getInstruction(index + 1)),
                )
            }
        }

        arrayOf(
            pivotBarChangedMethodMatch,
            pivotBarStyleMethodMatch
        ).forEach { match ->
            match.method.apply {
                val targetIndex = match[1] + 1
                val register = getInstruction<OneRegisterInstruction>(targetIndex - 1).registerA

                addInstructions(
                    targetIndex,
                    """
                        invoke-static { v$register }, ${EXTENSION_CLASS_DESCRIPTOR}->enableNarrowNavigationButton(Z)Z
                        move-result v$register
                    """
                )
            }
        }

        if (is_21_36_or_greater) {
            // 21.36 no longer uses the flag of the auto-hide method.
            // Show the navigation bar on every scroll instead, so the last scroll direction is never down.
            navigationBarScrollMethodMatch.let {
                it.method.apply {
                    val index = it[1]
                    val register = getInstruction<FiveRegisterInstruction>(index).registerD

                    addInstructions(
                        index,
                        """
                            invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->showNavigationBarOnScroll(Z)Z
                            move-result v$register
                        """,
                    )
                }
            }
        } else if (is_20_31_or_greater) {
            autoHideNavigationBarMethod.addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->disableAutoHidingNavigationBar()Z
                    move-result v0      
                    if-eqz v0, :show
                    return-void      
                    :show
                    nop      
                """
            )
        }

        //
        // Toolbar.
        //

        val toolbarPreferences = mutableSetOf(
            SwitchPreference("revanced_hide_toolbar_create_button"),
            SwitchPreference("revanced_hide_toolbar_notification_button"),
            SwitchPreference("revanced_hide_toolbar_messages_button"),
            SwitchPreference("revanced_hide_toolbar_search_button")
        )
        if (!is_20_31_or_greater) {
            toolbarPreferences += SwitchPreference("revanced_wide_searchbar")
        }

        PreferenceScreen.GENERAL.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_toolbar_screen",
                sorting = Sorting.UNSORTED,
                preferences = toolbarPreferences
            )
        )

        hookToolbar("${EXTENSION_CLASS_DESCRIPTOR}->hideCreateButton")
        hookToolbar("${EXTENSION_CLASS_DESCRIPTOR}->hideNotificationButton")
        hookToolbar("${EXTENSION_CLASS_DESCRIPTOR}->hideMessagesButton")
        hookToolbar("${EXTENSION_CLASS_DESCRIPTOR}->hideSearchButton")


        //
        // Wide searchbar.
        //

        // YT removed the legacy text search text field all code required to use it.
        // This functionality could be restored by adding a search text field to the toolbar
        // with a listener that artificially clicks the toolbar search button.
        if (!is_20_31_or_greater) {
            // Navigate to the method that checks if the YT logo is shown beside the search bar.
            val shouldShowLogoMethod = with(setWordmarkHeaderMethod) {
                val invokeStaticIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_STATIC && methodReference?.returnType == "Z"
                }
                navigate(this).to(invokeStaticIndex).stop()
            }

            shouldShowLogoMethod.apply {
                findInstructionIndicesReversedOrThrow(Opcode.RETURN).forEach { index ->
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructionsAtControlFlowLabel(
                        index,
                        """
                            invoke-static { v$register }, ${EXTENSION_CLASS_DESCRIPTOR}->enableWideSearchbar(Z)Z
                            move-result v$register
                        """
                    )
                }
            }

            // Fix missing left padding when using wide searchbar.
            wideSearchbarLayoutMethod.apply {
                findInstructionIndicesReversedOrThrow {
                    val reference = methodReference
                    reference?.definingClass == "Landroid/view/LayoutInflater;" && reference.name == "inflate"
                }.forEach { inflateIndex ->
                    val register =
                        getInstruction<OneRegisterInstruction>(inflateIndex + 1).registerA

                    addInstruction(
                        inflateIndex + 2,
                        "invoke-static { v$register }, " +
                                "${EXTENSION_CLASS_DESCRIPTOR}->setActionBar(Landroid/view/View;)V"
                    )
                }
            }
        }
    }
}
