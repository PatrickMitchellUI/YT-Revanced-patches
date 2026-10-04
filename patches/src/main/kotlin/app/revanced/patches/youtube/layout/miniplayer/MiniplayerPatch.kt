@file:Suppress("SpellCheckingInspection")

package app.revanced.patches.youtube.layout.miniplayer

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.featureflag.FeatureFlag
import app.revanced.patches.shared.misc.featureflag.featureFlagOverridePatch
import app.revanced.patches.shared.misc.featureflag.overrideBooleanFeatureFlag
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.*
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/MiniplayerPatch;"

@Suppress("unused")
val miniplayerPatch = bytecodePatch(
    name = "Miniplayer",
    description = "Adds options to change the in-app minimized player.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        resourceMappingPatch,
        featureFlagOverridePatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "20.40.45",
            "21.36.47"
        ),
    )

    apply {
        addResources("youtube", "layout.miniplayer.miniplayerPatch")

        PreferenceScreen.PLAYER.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_miniplayer_screen",
                sorting = PreferenceScreenPreference.Sorting.UNSORTED,
                preferences = setOf(
                    ListPreference("revanced_miniplayer_type"),
                    SwitchPreference("revanced_miniplayer_hide_subtext"),
                    SwitchPreference("revanced_miniplayer_hide_overlay_buttons"),
                    TextPreference("revanced_miniplayer_width_dip", inputType = InputType.NUMBER),
                ),
            ),
        )

        // Enables a handler that immediately closes the miniplayer.
        overrideBooleanFeatureFlag(
            FeatureFlag.MINIPLAYER_DISABLED,
            "$EXTENSION_CLASS_DESCRIPTOR->getMiniplayerOnCloseHandler(Z)Z",
        )

        miniplayerMinimumSizeMethodMatch.let {
            it.method.apply {
                val index = it[1]
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                // The width constant is used for both the minimum and the initial size.
                // Override the initial size, which is set by the second dip conversion.
                val dipConversionIndex = indexOfFirstInstructionOrThrow(index, Opcode.INVOKE_STATIC)
                val dipConversionMethod = getInstruction(dipConversionIndex).methodReference
                val initialSizeIndex = indexOfFirstInstructionOrThrow(dipConversionIndex + 1) {
                    methodReference == dipConversionMethod
                }
                val sizeRegister = getInstruction<FiveRegisterInstruction>(initialSizeIndex).registerD

                addInstructions(
                    initialSizeIndex,
                    """
                        const/16 v$sizeRegister, 0xc0
                        invoke-static { v$sizeRegister }, $EXTENSION_CLASS_DESCRIPTOR->getMiniplayerDefaultSize(I)I
                        move-result v$sizeRegister
                    """,
                )

                // Smaller sizes can be used, but the miniplayer will always start in size 170 if set any smaller.
                // The 170 initial limit probably could be patched to allow even smaller initial sizes,
                // but 170 is already half the horizontal space and smaller does not seem useful.
                replaceInstruction(index, "const/16 v$register, 170")
            }
        }

        // region Add hooks to hide modern miniplayer views.

        listOf(
            ClassDef::miniplayerModernExpandButtonMethodMatch to "hideMiniplayerOverlayButton",
            ClassDef::miniplayerModernCloseButtonMethodMatch to "hideMiniplayerOverlayButton",
            ClassDef::miniplayerModernActionButtonMethodMatch to "hideMiniplayerOverlayButton",
            ClassDef::miniplayerModernRewindButtonMethodMatch to "hideMiniplayerOverlayButton",
            ClassDef::miniplayerModernForwardButtonMethodMatch to "hideMiniplayerOverlayButton",
            ClassDef::miniplayerModernSubtitleTextMethodMatch to "hideMiniplayerSubTexts",
        ).forEach { (matching, methodName) ->
            val match = matching.get(miniplayerModernViewParentMethod.immutableClassDef)

            match.method.apply {
                val index = match[-1]
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V",
                )
            }
        }

        // endregion
    }
}
