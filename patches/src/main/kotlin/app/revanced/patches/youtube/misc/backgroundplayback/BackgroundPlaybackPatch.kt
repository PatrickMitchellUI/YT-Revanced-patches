package app.revanced.patches.youtube.misc.backgroundplayback

import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.featureflag.featureFlagOverridePatch
import app.revanced.patches.shared.misc.featureflag.overrideBooleanFeatureFlag
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.misc.playservice.is_19_34_or_greater
import app.revanced.patches.youtube.misc.playservice.is_20_29_or_greater
import app.revanced.patches.youtube.misc.playservice.is_21_36_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.backgroundPlaybackManagerShortsMethod
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.util.*
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal var prefBackgroundAndOfflineCategoryId = -1L
    private set

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/BackgroundPlaybackPatch;"

@Suppress("unused")
val removeBackgroundPlaybackRestrictionsPatch = bytecodePatch(
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
        featureFlagOverridePatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "20.40.45",
            "21.36.47"
        ),
    )

    apply {
        addResources("youtube", "misc.backgroundplayback.backgroundPlaybackPatch")

        PreferenceScreen.SHORTS.addPreferences(
            SwitchPreference("revanced_shorts_disable_background_playback"),
        )

        prefBackgroundAndOfflineCategoryId =
            ResourceType.STRING["pref_background_and_offline_category"]

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
                        """
                    )
                }
            }
        }

        // Enable background playback option in YouTube settings
        backgroundPlaybackSettingsMethod.apply {
            val booleanCalls = instructions.withIndex().filter {
                it.value.methodReference?.returnType == "Z"
            }

            val settingsBooleanIndex = booleanCalls.elementAt(1).index

            val settingsBooleanMethod = navigate(this).to(settingsBooleanIndex).stop()

            settingsBooleanMethod.returnEarly(true)
        }

        // Force allowing background play for Shorts.
        shortsBackgroundPlaybackFeatureFlagMethod.returnEarly(true)

        // Don't show the education of background playback, a notification and a dialog
        // that YouTube shows Premium users once when leaving the app during playback.
        val settingsStoreType = backgroundPlaybackInfoControllerMethod.immutableClassDef.methods.first {
            it.name == "<init>"
        }.parameterTypes[3].toString()
        getShowBackgroundPlaybackEducationMethod(settingsStoreType).returnEarly(false)

        // Force allowing background play for videos labeled for kids.
        kidsBackgroundPlaybackPolicyControllerMethod.returnEarly()

        // Fix PiP buttons not working after locking/unlocking device screen.
        // Flag was removed in 21.36.
        if (is_19_34_or_greater && !is_21_36_or_greater) {
            pipInputConsumerFeatureFlagMethodMatch.let {
                it.method.insertLiteralOverride(
                    it[0],
                    false,
                )
            }
        }

        // Client flag that interferes with background playback of some video types.
        // Exact purpose is not clear and it's used in ~ 100 locations.
        // 21.36 requires the flag, otherwise the player controls are not shown.
        if (is_20_29_or_greater && !is_21_36_or_greater) {
            overrideBooleanFeatureFlag(45698813L, false)
        }
    }
}
