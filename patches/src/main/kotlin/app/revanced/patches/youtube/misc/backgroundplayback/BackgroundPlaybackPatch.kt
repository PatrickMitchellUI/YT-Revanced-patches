package app.revanced.patches.youtube.misc.backgroundplayback

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.CompositeMatch
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.firstMethod
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.misc.playservice.is_20_29_or_greater
import app.revanced.patches.youtube.misc.playservice.is_21_04_or_greater
import app.revanced.patches.youtube.misc.playservice.is_21_21_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.backgroundPlaybackManagerShortsMethod
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.getReference
import app.revanced.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/BackgroundPlaybackPatch;"

@Suppress("unused")
val removeBackgroundPlaybackRestrictionsPatch =
    bytecodePatch(
        name = "Remove background playback restrictions",
        description = "Removes restrictions on background playback, including playing kids videos in the background.",
    ) {
        dependsOn(
            resourceMappingPatch,
            addResourcesPatch,
            sharedExtensionPatch,
            playerTypeHookPatch,
            videoInformationPatch,
            settingsPatch,
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
            addResources("youtube", "misc.backgroundplayback.backgroundPlaybackPatch")

            PreferenceScreen.SHORTS.addPreferences(
                SwitchPreference("revanced_shorts_disable_background_playback", summaryKey = null),
            )

            PreferenceScreen.MISC.addPreferences(
                SwitchPreference("revanced_remove_background_playback_restrictions", summaryKey = null),
            )

            arrayOf(
                backgroundPlaybackManagerMethod to "isBackgroundPlaybackAllowed",
                backgroundPlaybackManagerShortsMethod to "isBackgroundShortsPlaybackAllowed",
            ).forEach { (method, integrationsMethod) ->
                method.apply {
                    findInstructionIndicesReversedOrThrow(Opcode.RETURN).forEach { index ->
                        val register = getInstruction<OneRegisterInstruction>(index).registerA

                        addInstructionsAtControlFlowLabel(
                            index,
                            """
                            invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->$integrationsMethod(Z)Z
                            move-result v$register 
                        """,
                        )
                    }
                }
            }

            fun MutableMethod.addBackgroundPlaybackIsPatchEnabledHook() {
                val returnInstruction = if (returnType == "Z") "return v0" else "return-void"

                addInstructionsWithLabels(
                    0,
                    """
                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->isPatchEnabled()Z
                    move-result v0
                    if-eqz v0, :disabled
                    $returnInstruction
                    :disabled
                    nop
                """,
                )
            }

            fun CompositeMatch.addBackgroundPlaybackFeatureFlagHook(enable: Boolean) {
                val methodName = if (enable) "enableFeatureFlag" else "disableFeatureFlag"
                method.insertLiteralOverride(this[0], "$EXTENSION_CLASS_DESCRIPTOR->$methodName(Z)Z")
            }

            // Enable background playback option in YouTube settings
            backgroundPlaybackSettingsMethod.apply {
                val booleanCall =
                    instructions.first {
                        it.methodReference?.returnType == "Z"
                    }

                firstMethod(booleanCall.methodReference!!).addBackgroundPlaybackIsPatchEnabledHook()
            }

            // Force allowing background play for Shorts.
            shortsBackgroundPlaybackFeatureFlagMethodMatch.addBackgroundPlaybackFeatureFlagHook(true)

            // Force allowing background play for videos labeled for kids.
            kidsBackgroundPlaybackPolicyControllerMethod.addBackgroundPlaybackIsPatchEnabledHook()

            // Fix PiP buttons not working after locking/unlocking device screen.
            if (!is_21_21_or_greater) {
                pipInputConsumerFeatureFlagMethodMatch.addBackgroundPlaybackFeatureFlagHook(false)
            }

            if (is_20_29_or_greater) {
                // Client flag that interferes with background playback of some video types.
                // Exact purpose is not clear and it's used in ~ 100 locations.
                newPlayerTypeEnumFeatureFlagMethodMatch.addBackgroundPlaybackFeatureFlagHook(false)
            }

            if (is_21_04_or_greater) {
                // If newPlayerTypeEnumFeatureFlagMethodMatch is present and forced off then this flag
                // must also be disabled, otherwise the player is a black screen with no buttons and no playback.
                newPlayerOverlaysFeatureFlagMethodMatch.addBackgroundPlaybackFeatureFlagHook(false)
            }
        }
    }
