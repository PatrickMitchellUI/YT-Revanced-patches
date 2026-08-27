package app.revanced.patches.youtube.layout.theme

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.layout.theme.*
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.*
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.revanced.patches.youtube.layout.seekbar.seekbarColorPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playservice.is_21_06_or_greater
import app.revanced.patches.youtube.misc.playservice.is_21_08_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.childElementsSequence
import app.revanced.util.forEachChildElement
import app.revanced.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/theme/ThemePatch;"
private const val SPLASH_BACKGROUND_COLOR = "revanced_splash_background_color"

private val lightThemeStyleFiles =
    arrayOf(
        "res/values/styles.xml",
        "res/values-v27/styles.xml",
        "res/values-v31/styles.xml",
    )

private val notificationColorThemePatch =
    resourcePatch {
        dependsOn(resourceMappingPatch)

        apply {
            val lightThemeBackgroundColor = lightThemeBackgroundColorOption.value!!
            val darkThemeBackgroundColor = darkThemeBackgroundColorOption.value!!

            fun Element.setStyleItem(
                document: Document,
                name: String,
                value: String,
            ) {
                val item =
                    childElementsSequence().firstOrNull {
                        it.nodeName == "item" && it.getAttribute("name") == name
                    } ?: document.createElement("item").also {
                        it.setAttribute("name", name)
                        appendChild(it)
                    }

                item.textContent = value
            }

            fun ResourcePatchContext.addColorResource(
                resourceFile: String,
                colorName: String,
                colorValue: String,
            ) = document(resourceFile).use { document ->
                val resources = document.getElementsByTagName("resources").item(0) as Element
                resources.appendChild(
                    document.createElement("color").apply {
                        setAttribute("name", colorName)
                        setAttribute("category", "color")
                        textContent = colorValue
                    },
                )
            }

            fun ResourcePatchContext.patchSplashScreens() {
                arrayOf(
                    "res/drawable/quantum_launchscreen_youtube.xml",
                    "res/drawable-sw600dp/quantum_launchscreen_youtube.xml",
                ).forEach editSplashScreen@{ resourceFile ->
                    document(resourceFile).use { document ->
                        document
                            .getElementsByTagName("layer-list")
                            .item(0)
                            .forEachChildElement { node ->
                                if (node.hasAttribute("android:drawable")) {
                                    node.setAttribute("android:drawable", "@color/$SPLASH_BACKGROUND_COLOR")
                                    return@editSplashScreen
                                }
                            }

                        throw PatchException("Failed to modify splash screen")
                    }
                }

                // In 19.32+ the dark mode splash screen is white and fades to black without this override.
                document("res/values-night/styles.xml").use { document ->
                    val style =
                        document.createElement("style").apply {
                            setAttribute("name", "Theme.YouTube.Home")
                            setAttribute("parent", "@style/Base.V27.Theme.YouTube.Home")
                        }
                    val splashColor = "@color/$SPLASH_BACKGROUND_COLOR"

                    arrayOf(
                        "android:navigationBarColor" to splashColor,
                        "android:windowBackground" to splashColor,
                        "android:colorBackground" to splashColor,
                        "colorPrimaryDark" to splashColor,
                        "android:windowLightStatusBar" to "false",
                    ).forEach { (name, value) ->
                        style.setStyleItem(document, name, value)
                    }

                    val resources = document.getElementsByTagName("resources").item(0) as Element
                    resources.appendChild(style)
                }
            }

            fun ResourcePatchContext.enableLightStatusBar() =
                lightThemeStyleFiles.forEach { stylesFile ->
                    runCatching {
                        document(stylesFile).use { document ->
                            val resources =
                                document.getElementsByTagName("resources").item(0) as? Element
                                    ?: return@use
                            val theme =
                                resources.childElementsSequence().firstOrNull {
                                    it.nodeName == "style" && it.getAttribute("name") == "Theme.YouTube.Home"
                                } ?: document.createElement("style").also {
                                    it.setAttribute("name", "Theme.YouTube.Home")
                                    it.setAttribute("parent", "@style/Base.V27.Theme.YouTube.Home")
                                    resources.appendChild(it)
                                }

                            theme.setStyleItem(document, "android:windowLightStatusBar", "true")
                        }
                    }
                }

            fun ResourcePatchContext.patchMaterialYouNavigation(
                useDarkTheme: Boolean,
                useLightTheme: Boolean,
            ) {
                if (!useDarkTheme && !useLightTheme) return

                val resDir = get("res")
                val darkIndicatorColor = "@android:color/system_accent1_100"
                val lightIndicatorColor = "@android:color/system_accent1_200"
                val lightCountBackgroundColor = "@android:color/system_accent1_100"

                if (useDarkTheme) {
                    createNotificationDrawable(
                        resDir,
                        "drawable/revanced_notification_dot_dark.xml",
                        darkIndicatorColor,
                        "oval",
                    )
                    createNotificationDrawable(
                        resDir,
                        "drawable/revanced_notification_count_dark.xml",
                        darkIndicatorColor,
                        "rectangle",
                        hasCorners = true,
                    )
                }

                if (useLightTheme) {
                    createNotificationDrawable(
                        resDir,
                        "drawable/revanced_notification_dot_light.xml",
                        lightIndicatorColor,
                        "oval",
                    )
                    createNotificationDrawable(
                        resDir,
                        "drawable/revanced_notification_count_light.xml",
                        lightCountBackgroundColor,
                        "rectangle",
                        hasCorners = true,
                    )
                }

                patchCountTextColor(resDir, "@android:color/system_neutral1_900")

                val stylesFile = "res/values/styles.xml"
                if (!get(stylesFile).exists()) return

                document(stylesFile).use { document ->
                    val resources = document.getElementsByTagName("resources").item(0) as? Element ?: return@use

                    resources.forEachChildElement { style ->
                        if (style.nodeName != "style") return@forEachChildElement

                        when (style.getAttribute("name")) {
                            "PivotBar.Dark" -> {
                                if (!useDarkTheme) return@forEachChildElement
                                style.setStyleItem(document, "dotBackground", "@drawable/revanced_notification_dot_dark")
                                style.setStyleItem(document, "countBackground", "@drawable/revanced_notification_count_dark")
                            }

                            "PivotBar.Default" -> {
                                if (!useLightTheme) return@forEachChildElement
                                style.setStyleItem(document, "dotBackground", "@drawable/revanced_notification_dot_light")
                                style.setStyleItem(document, "countBackground", "@drawable/revanced_notification_count_light")
                            }

                            "CairoDarkThemeUpdates" -> {
                                if (useDarkTheme) {
                                    style.setStyleItem(document, "ytRedIndicator", darkIndicatorColor)
                                }
                            }

                            "CairoLightThemeUpdates" -> {
                                if (useLightTheme) {
                                    style.setStyleItem(document, "ytRedIndicator", lightIndicatorColor)
                                }
                            }
                        }
                    }
                }
            }

            addColorResource(
                "res/values/colors.xml",
                SPLASH_BACKGROUND_COLOR,
                lightThemeBackgroundColor,
            )
            addColorResource(
                "res/values-night/colors.xml",
                SPLASH_BACKGROUND_COLOR,
                darkThemeBackgroundColor,
            )

            patchSplashScreens()
            enableLightStatusBar()
            patchMaterialYouNavigation(
                useDarkTheme = darkThemeBackgroundColor.startsWith("@android:color/system_"),
                useLightTheme = lightThemeBackgroundColor.startsWith("@android:color/system_"),
            )
        }
    }

private val themeColorResourcePatch =
    baseThemeResourcePatch(
        lightColorReplacement = { lightThemeBackgroundColorOption.value!! },
        getDarkColorNames = {
            THEME_DEFAULT_DARK_COLOR_NAMES +
                if (is_21_06_or_greater) {
                    setOf(
                        // yt_ref_color_constants_baseline_black_black0
                        // yt_ref_color_constants_baseline_black_black1
                        // yt_ref_color_constants_baseline_black_black3
                        "yt_sys_color_baseline_dark_menu_background",
                        "yt_sys_color_baseline_dark_raised_background",
                        "yt_sys_color_baseline_dark_base_background",
                        "yt_sys_color_baseline_dark_static_black",
                        "yt_sys_color_baseline_light_inverted_background",
                        "yt_sys_color_baseline_light_static_black",
                    )
                } else {
                    emptySet()
                }
        },
        getLightColorNames = {
            THEME_DEFAULT_LIGHT_COLOR_NAMES +
                if (is_21_06_or_greater) {
                    setOf(
                        "yt_sys_color_baseline_light_base_background",
                        "yt_sys_color_baseline_light_raised_background",
                    )
                } else {
                    emptySet()
                }
        },
    )

val themePatch =
    baseThemePatch(
        extensionClassDescriptor = EXTENSION_CLASS_DESCRIPTOR,
        includeLightThemeOption = true,
        block = {
            dependsOn(
                addResourcesPatch,
                seekbarColorPatch,
                settingsPatch,
                sharedExtensionPatch,
                versionCheckPatch,
                themeColorResourcePatch,
                notificationColorThemePatch,
            )

            compatibleWith(
                "com.google.android.youtube"(
                    "20.14.43",
                    "20.21.37",
                    "20.26.46",
                    "20.31.42",
                    "20.37.48",
                    "20.45.36",
                    "20.47.62",
                    "20.51.39",
                ),
            )
        },
        executeBlock = {
            addResources("youtube", "layout.theme.themePatch")

            PreferenceScreen.GENERAL.addPreferences(
                SwitchPreference("revanced_gradient_loading_screen"),
                ListPreference("revanced_splash_screen_animation_style"),
            )

            PreferenceScreen.SEEKBAR.addPreferences(
                noTitleUnsortedPreferenceCategory(
                    SwitchPreference("revanced_seekbar_custom_color", summaryKey = null),
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
                ),
            )

            useGradientLoadingScreenMethodMatch.method.insertLiteralOverride(
                useGradientLoadingScreenMethodMatch[0],
                "$EXTENSION_CLASS_DESCRIPTOR->gradientLoadingScreenEnabled(Z)Z",
            )

            if (is_21_08_or_greater) {
                carbonColorThemeFeatureFlagMethodMatch.method.insertLiteralOverride(
                    carbonColorThemeFeatureFlagMethodMatch[0],
                    false,
                )
            }

            // Lottie splash screen exists in earlier versions, but it may not be always on.
            splashScreenStyleMethodMatch.method.insertLiteralOverride(
                splashScreenStyleMethodMatch[0],
                "$EXTENSION_CLASS_DESCRIPTOR->getLoadingScreenType(I)I",
            )

            showSplashScreen1MethodMatch.let { match ->
                match.method.apply {
                    val index = match[-1]
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructions(
                        index + 1,
                        """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->showSplashScreen(Z)Z
                        move-result v$register
                    """,
                    )
                }
            }

            showSplashScreen2MethodMatch.let { match ->
                val insertIndex = match[1]
                match.method.apply {
                    val insertInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)
                    val registerA = insertInstruction.registerA
                    val registerB = insertInstruction.registerB

                    addInstructions(
                        insertIndex,
                        """
                        invoke-static { v$registerA, v$registerB }, $EXTENSION_CLASS_DESCRIPTOR->showSplashScreen(II)I
                        move-result v$registerA
                    """,
                    )
                }
            }
        },
    )
