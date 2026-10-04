package app.revanced.patches.youtube.layout.theme


import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.layout.theme.THEME_COLOR_OPTION_DESCRIPTION
import app.revanced.patches.shared.layout.theme.THEME_DEFAULT_DARK_COLOR_NAMES
import app.revanced.patches.shared.layout.theme.THEME_DEFAULT_LIGHT_COLOR_NAMES
import app.revanced.patches.shared.layout.theme.baseThemePatch
import app.revanced.patches.shared.layout.theme.baseThemeResourcePatch
import app.revanced.patches.shared.layout.theme.darkThemeBackgroundColorOption
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.overrideThemeColors
import app.revanced.patches.shared.misc.settings.preference.InputType
import app.revanced.patches.shared.misc.settings.preference.ListPreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceCategory
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.shared.misc.settings.preference.TextPreference
import app.revanced.patches.youtube.layout.seekbar.seekbarColorPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playservice.is_21_36_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.findElementByAttributeValue
import app.revanced.util.forEachChildElement
import app.revanced.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/theme/ThemePatch;"

val themePatch = baseThemePatch(
    extensionClassDescriptor = EXTENSION_CLASS_DESCRIPTOR,

    block = {
        val lightThemeBackgroundColor by stringOption(
            name = "Light theme background color",
            default = "@android:color/white",
            values = mapOf(
                "White" to "@android:color/white",
                "Material You (Neutral)" to "@android:color/system_neutral1_100",
                "Material You - Primary" to "@android:color/system_accent1_200",
                "Material You - Secondary" to "@android:color/system_accent2_200",
                "Material You - Tertiary" to "@android:color/system_accent3_200",
                "Catppuccin (Latte)" to "#E6E9EF",
                "Light pink" to "#FCCFF3",
                "Light blue" to "#D1E0FF",
                "Light green" to "#CCFFCC",
                "Light yellow" to "#FDFFCC",
                "Light orange" to "#FFE6CC",
                "Light red" to "#FFD6D6",
            ),
            description = THEME_COLOR_OPTION_DESCRIPTION,
        )

        val themeResourcePatch = resourcePatch {
            dependsOn(resourceMappingPatch)

            apply {
                overrideThemeColors(
                    lightThemeBackgroundColor!!,
                    darkThemeBackgroundColorOption.value!!,
                )

                fun addColorResource(
                    resourceFile: String,
                    colorName: String,
                    colorValue: String,
                ) {
                    document(resourceFile).use { document ->
                        val resourcesNode =
                            document.getElementsByTagName("resources").item(0) as Element

                        resourcesNode.appendChild(
                            document.createElement("color").apply {
                                setAttribute("name", colorName)
                                setAttribute("category", "color")
                                textContent = colorValue
                            },
                        )
                    }
                }

                // Add a dynamic background color to the colors.xml file.
                val splashBackgroundColorKey = "revanced_splash_background_color"
                addColorResource(
                    "res/values/colors.xml",
                    splashBackgroundColorKey,
                    lightThemeBackgroundColor!!,
                )
                addColorResource(
                    "res/values-night/colors.xml",
                    splashBackgroundColorKey,
                    darkThemeBackgroundColorOption.value!!,
                )

                // Edit splash screen files and change the background color.
                arrayOf(
                    "res/drawable/quantum_launchscreen_youtube.xml",
                    "res/drawable-sw600dp/quantum_launchscreen_youtube.xml",
                ).forEach editSplashScreen@{ resourceFileName ->
                    document(resourceFileName).use { document ->
                        document.getElementsByTagName(
                            "layer-list",
                        ).item(0).forEachChildElement { node ->
                            if (node.hasAttribute("android:drawable")) {
                                node.setAttribute(
                                    "android:drawable",
                                    "@color/$splashBackgroundColorKey",
                                )
                                return@editSplashScreen
                            }
                        }

                        throw PatchException("Failed to modify launch screen")
                    }
                }

                // Fix the splash screen dark mode background color.
                // In 19.32+ the dark mode splash screen is white and fades to black.
                document("res/values-night/styles.xml").use { document ->
                    // Create a night mode specific override for the splash screen background,
                    // or use the existing style if the app already declares it.
                    val existingStyle = document.childNodes.findElementByAttributeValue(
                        "name",
                        "Theme.YouTube.Home",
                    )
                    val style = existingStyle ?: document.createElement("style").apply {
                        setAttribute("name", "Theme.YouTube.Home")
                        setAttribute("parent", "@style/Base.V27.Theme.YouTube.Home")
                    }

                    // Fix status and navigation bar showing white on some Android devices,
                    // such as SDK 28 Android 10 medium tablet.
                    val colorSplashBackgroundColor = "@color/$splashBackgroundColorKey"
                    arrayOf(
                        "android:navigationBarColor" to colorSplashBackgroundColor,
                        "android:windowBackground" to colorSplashBackgroundColor,
                        "android:colorBackground" to colorSplashBackgroundColor,
                        "colorPrimaryDark" to colorSplashBackgroundColor,
                        "android:windowLightStatusBar" to "false",
                    ).forEach { (name, value) ->
                        val styleItem = document.createElement("item")
                        styleItem.setAttribute("name", name)
                        styleItem.textContent = value
                        style.appendChild(styleItem)
                    }

                    if (existingStyle == null) {
                        val resourcesNode =
                            document.getElementsByTagName("resources").item(0) as Element
                        resourcesNode.appendChild(style)
                    }
                }

                // 21.36+ sets the background colors as theme attributes instead of color resources.
                if (is_21_36_or_greater) {
                    val backgroundAttributes = setOf(
                        "yt_sys_color_baseline_base_background",
                        "yt_sys_color_baseline_raised_background",
                        "yt_sys_color_baseline_menu_background",
                    )
                    val styleColors = mapOf(
                        "yt.sys.color.baseline" to lightThemeBackgroundColor!!,
                        "yt.sys.color.baseline.dark" to darkThemeBackgroundColorOption.value!!,
                        "yt.sys.color.baseline.dark.cooler" to darkThemeBackgroundColorOption.value!!,
                    )

                    document("res/values/styles.xml").use { document ->
                        val resourcesNode = document.getElementsByTagName("resources").item(0)
                        var stylesFound = 0

                        resourcesNode.forEachChildElement { style ->
                            val color = styleColors[style.getAttribute("name")] ?: return@forEachChildElement
                            stylesFound++

                            style.forEachChildElement { item ->
                                if (item.getAttribute("name") in backgroundAttributes) {
                                    item.textContent = color
                                }
                            }
                        }

                        if (stylesFound != styleColors.size) {
                            throw PatchException("Could not find the background color styles")
                        }
                    }
                }
            }
        }
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            addResourcesPatch,
            seekbarColorPatch,
            versionCheckPatch,
            baseThemeResourcePatch(
                lightColorReplacement = { lightThemeBackgroundColor!! },
                getDarkColorNames = {
                    THEME_DEFAULT_DARK_COLOR_NAMES + if (is_21_36_or_greater)
                        setOf(
                            // yt_ref_color_constants_baseline_black_black0
                            "yt_sys_color_baseline_dark_menu_background",
                            // yt_ref_color_constants_baseline_black_black1
                            "yt_sys_color_baseline_dark_static_black",
                            "yt_sys_color_baseline_dark_raised_background",
                            // yt_ref_color_constants_baseline_black_black3
                            "yt_sys_color_baseline_dark_base_background",
                            "yt_sys_color_baseline_dark_static_black",
                            "yt_sys_color_baseline_light_inverted_background",
                            "yt_sys_color_baseline_light_static_black",
                        ) else emptySet()
                },
                getLightColorNames = {
                    THEME_DEFAULT_LIGHT_COLOR_NAMES + if (is_21_36_or_greater)
                        setOf(
                            "yt_sys_color_baseline_light_base_background",
                            "yt_sys_color_baseline_light_raised_background"
                        )
                    else emptySet()
                }
            ),
            themeResourcePatch,
        )

        compatibleWith(
            "com.google.android.youtube"(
                "20.40.45",
                "21.36.47"
            ),
        )
    },

    executeBlock = {
        addResources("youtube", "layout.theme.themePatch")

        PreferenceScreen.GENERAL.addPreferences(
            SwitchPreference("revanced_gradient_loading_screen"),
        )

        val preferences = mutableSetOf(
            SwitchPreference("revanced_seekbar_custom_color"),
            TextPreference(
                "revanced_seekbar_custom_color_primary",
                tag = "app.revanced.extension.shared.settings.preference.ColorPickerPreference",
                inputType = InputType.TEXT_CAP_CHARACTERS,
            ),
            TextPreference(
                "revanced_seekbar_custom_color_accent",
                tag = "app.revanced.extension.shared.settings.preference.ColorPickerPreference",
                inputType = InputType.TEXT_CAP_CHARACTERS,
            ),
        )

        PreferenceScreen.SEEKBAR.addPreferences(
            PreferenceCategory(
                titleKey = null,
                sorting = Sorting.UNSORTED,
                tag = "app.revanced.extension.shared.settings.preference.NoTitlePreferenceCategory",
                preferences = preferences,
            ),
        )


        PreferenceScreen.GENERAL.addPreferences(
            ListPreference("revanced_splash_screen_animation_style"),
        )

        useGradientLoadingScreenMethodMatch.method.insertLiteralOverride(
            useGradientLoadingScreenMethodMatch[0],
            "$EXTENSION_CLASS_DESCRIPTOR->gradientLoadingScreenEnabled(Z)Z",
        )

        splashScreenStyleMethodMatch.method.insertLiteralOverride(
            splashScreenStyleMethodMatch[0],
            "$EXTENSION_CLASS_DESCRIPTOR->getLoadingScreenType(I)I",
        )

        showSplashScreen1MethodMatch.let {
            it.method.apply {
                val index = it[-1]
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1,
                    """
                        invoke-static { v$register }, ${EXTENSION_CLASS_DESCRIPTOR}->showSplashScreen(Z)Z
                        move-result v$register
                    """
                )
            }
        }

        showSplashScreen2MethodMatch.let {
            val insertIndex = it[1]
            it.method.apply {
                val insertInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)
                val registerA = insertInstruction.registerA
                val registerB = insertInstruction.registerB

                addInstructions(
                    insertIndex,
                    """
                        invoke-static { v$registerA, v$registerB }, ${EXTENSION_CLASS_DESCRIPTOR}->showSplashScreen(II)I
                        move-result v$registerA
                    """
                )
            }
        }
    },
)
