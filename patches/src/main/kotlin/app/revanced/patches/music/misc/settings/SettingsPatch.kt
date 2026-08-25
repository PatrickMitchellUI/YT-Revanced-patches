package app.revanced.patches.music.misc.settings

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.links.disableAppLinksVerificationPatch
import app.revanced.patches.all.misc.packagename.setOrGetFallbackPackageName
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.music.misc.extension.sharedExtensionPatch
import app.revanced.patches.music.misc.gms.Constants.MUSIC_PACKAGE_NAME
import app.revanced.patches.music.playservice.is_8_40_or_greater
import app.revanced.patches.music.playservice.versionCheckPatch
import app.revanced.patches.shared.boldIconsFeatureFlagMethodMatch
import app.revanced.patches.shared.googleApiActivityOnCreateMethod
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.BasePreference
import app.revanced.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.revanced.patches.shared.misc.settings.preference.InputType
import app.revanced.patches.shared.misc.settings.preference.IntentPreference
import app.revanced.patches.shared.misc.settings.preference.NonInteractivePreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.shared.misc.settings.preference.TextPreference
import app.revanced.patches.shared.misc.settings.settingsPatch
import app.revanced.patches.youtube.misc.settings.modifyActivityForSettingsInjection
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.inputStreamFromBundledResource
import app.revanced.util.insertLiteralOverride

private const val MUSIC_ACTIVITY_HOOK_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/music/settings/MusicActivityHook;"

private val preferences = mutableSetOf<BasePreference>()

private val settingsResourcePatch =
    resourcePatch {
        dependsOn(
            resourceMappingPatch,
            settingsPatch(
                rootPreferences =
                    listOf(
                        IntentPreference(
                            titleKey = "revanced_settings_title",
                            summaryKey = null,
                            intent = newIntent("revanced_settings_intent"),
                        ) to "settings_headers",
                    ),
                preferences = preferences,
            ),
        )

        apply {
            copyResources(
                "settings",
                ResourceGroup(
                    "drawable",
                    "revanced_settings_screen_00_about.xml",
                    "revanced_settings_screen_00_about_bold.xml",
                    "revanced_settings_screen_01_ads.xml",
                    "revanced_settings_screen_01_ads_bold.xml",
                    "revanced_settings_screen_04_general.xml",
                    "revanced_settings_screen_04_general_bold.xml",
                    "revanced_settings_screen_05_player.xml",
                    "revanced_settings_screen_05_player_bold.xml",
                    "revanced_settings_screen_11_misc.xml",
                    "revanced_settings_screen_11_misc_bold.xml",
                ),
                ResourceGroup(
                    "layout",
                    "revanced_preference_with_icon.xml",
                ),
            )

            // Set the style for the ReVanced settings to follow the style of the music settings,
            // namely: action bar height, menu item padding and remove horizontal dividers.
            val targetResource = "values/styles.xml"
            inputStreamFromBundledResource(
                "settings/music",
                targetResource,
            )!!.let { inputStream ->
                "resources"
                    .copyXmlNode(
                        document(inputStream),
                        document("res/$targetResource"),
                    ).close()
            }

            // Remove horizontal dividers from the music settings.
            val styleFile = get("res/values/styles.xml")
            styleFile.writeText(
                styleFile
                    .readText()
                    .replace(
                        "allowDividerAbove\">true",
                        "allowDividerAbove\">false",
                    ).replace(
                        "allowDividerBelow\">true",
                        "allowDividerBelow\">false",
                    ),
            )
        }
    }

val settingsPatch =
    bytecodePatch(
        description = "Adds settings for ReVanced to YouTube Music.",
    ) {
        dependsOn(
            sharedExtensionPatch,
            settingsResourcePatch,
            addResourcesPatch,
            versionCheckPatch,
            disableAppLinksVerificationPatch,
        )

        apply {
            addResources("music", "misc.settings.settingsPatch")
            addResources("shared", "misc.debugging.enableDebuggingPatch")

            // Add an "About" preference to the top.
            preferences +=
                NonInteractivePreference(
                    key = "revanced_settings_music_screen_0_about",
                    summaryKey = null,
                    icon = "@drawable/revanced_settings_screen_00_about",
                    iconBold = "@drawable/revanced_settings_screen_00_about_bold",
                    layout = "@layout/revanced_preference_with_icon",
                    tag = "app.revanced.extension.shared.settings.preference.ReVancedAboutPreference",
                    selectable = true,
                )

            PreferenceScreen.GENERAL.addPreferences(
                SwitchPreference("revanced_settings_search_history", summaryKey = null),
                SwitchPreference("revanced_show_menu_icons", summaryKey = null),
            )

            if (is_8_40_or_greater) {
                PreferenceScreen.GENERAL.addPreferences(
                    SwitchPreference("revanced_settings_disable_bold_icons", summaryKey = null),
                )
            }

            PreferenceScreen.MISC.addPreferences(
                TextPreference(
                    key = null,
                    titleKey = "revanced_pref_import_export_title",
                    summaryKey = "revanced_pref_import_export_summary",
                    inputType = InputType.TEXT_MULTI_LINE,
                    tag = "app.revanced.extension.shared.settings.preference.ImportExportPreference",
                ),
            )

            modifyActivityForSettingsInjection(
                googleApiActivityOnCreateMethod,
                MUSIC_ACTIVITY_HOOK_CLASS_DESCRIPTOR,
                true,
            )

            if (is_8_40_or_greater) {
                boldIconsFeatureFlagMethodMatch.let {
                    it.method.insertLiteralOverride(
                        it[0],
                        "$MUSIC_ACTIVITY_HOOK_CLASS_DESCRIPTOR->useBoldIcons(Z)Z",
                    )
                }
            }
        }

        afterDependents {
            PreferenceScreen.close()
        }
    }

/**
 * Creates an intent to open ReVanced settings.
 */
fun newIntent(settingsName: String) =
    IntentPreference.Intent(
        data = settingsName,
        targetClass = "com.google.android.gms.common.api.GoogleApiActivity",
    ) {
        // The package name change has to be reflected in the intent.
        setOrGetFallbackPackageName(MUSIC_PACKAGE_NAME)
    }

object PreferenceScreen : BasePreferenceScreen() {
    val ADS =
        Screen(
            key = "revanced_settings_music_screen_1_ads",
            summaryKey = null,
            icon = "@drawable/revanced_settings_screen_01_ads",
            iconBold = "@drawable/revanced_settings_screen_01_ads_bold",
            layout = "@layout/revanced_preference_with_icon",
        )
    val GENERAL =
        Screen(
            key = "revanced_settings_music_screen_2_general",
            summaryKey = null,
            icon = "@drawable/revanced_settings_screen_04_general",
            iconBold = "@drawable/revanced_settings_screen_04_general_bold",
            layout = "@layout/revanced_preference_with_icon",
        )
    val PLAYER =
        Screen(
            key = "revanced_settings_music_screen_3_player",
            summaryKey = null,
            icon = "@drawable/revanced_settings_screen_05_player",
            iconBold = "@drawable/revanced_settings_screen_05_player_bold",
            layout = "@layout/revanced_preference_with_icon",
        )
    val MISC =
        Screen(
            key = "revanced_settings_music_screen_4_misc",
            summaryKey = null,
            icon = "@drawable/revanced_settings_screen_11_misc",
            iconBold = "@drawable/revanced_settings_screen_11_misc_bold",
            layout = "@layout/revanced_preference_with_icon",
        )

    override fun commit(screen: PreferenceScreenPreference) {
        preferences += screen
    }
}
