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
import app.revanced.patches.youtube.misc.playservice.is_21_06_or_greater
import app.revanced.patches.youtube.misc.playservice.is_21_08_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
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

                    arrayOf(
                        "res/values/styles.xml",
                        "res/values-v27/styles.xml",
                        "res/values-v31/styles.xml"
                    ).forEach { stylesPath ->
                        try {
                            document(stylesPath).use { document ->
                                val resourcesNode =
                                    document.getElementsByTagName("resources").item(0) as? Element
                                        ?: return@use
                                var themeNode: Element? = null

                                resourcesNode.forEachChildElement { node ->
                                    if (node.nodeName == "style" && node.getAttribute("name") == "Theme.YouTube.Home") {
                                        themeNode = node
                                    }
                                }

                                if (themeNode == null) {
                                    themeNode = document.createElement("style").apply {
                                        setAttribute("name", "Theme.YouTube.Home")
                                        setAttribute("parent", "@style/Base.V27.Theme.YouTube.Home")
                                        resourcesNode.appendChild(this)
                                    }
                                }

                                var hasLightStatusBar = false
                                themeNode!!.forEachChildElement { node ->
                                    if (node.nodeName == "item" && node.getAttribute("name") == "android:windowLightStatusBar") {
                                        node.textContent = "true"
                                        hasLightStatusBar = true
                                    }
                                }

                                if (!hasLightStatusBar) {
                                    val styleItem = document.createElement("item")
                                    styleItem.setAttribute("name", "android:windowLightStatusBar")
                                    styleItem.textContent = "true"
                                    themeNode.appendChild(styleItem)
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }

                    val isMaterialYouDark =
                        darkThemeBackgroundColorOption.value!!.startsWith("@android:color/system_")
                    val isMaterialYouLight =
                        lightThemeBackgroundColor!!.startsWith("@android:color/system_")

                    // YouTube applies CairoLightThemeUpdates/CairoDarkThemeUpdates to the context theme
                    // before Cairo renders, so ?ytRedIndicator in Cairo drawables resolves
                    // from the style - not from night mode resource qualifiers.
                    if (isMaterialYouDark || isMaterialYouLight) {
                        fun createNotifDrawable(
                            resPath: String,
                            color: String,
                            shape: String,
                            hasCorners: Boolean = false,
                        ) {
                            val file = get("res").resolve(resPath)
                            file.parentFile?.mkdirs()
                            val cornersLine = if (hasCorners)
                                "\n    <corners android:radius=\"@dimen/new_content_count_radius\" />"
                            else ""
                            file.writeText(
                                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                        "<shape android:shape=\"$shape\"\n" +
                                        "  xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                                        "    <solid android:color=\"$color\" />$cornersLine\n" +
                                        "</shape>"
                            )
                        }

                        fun patchCountTextColor(resPath: String, color: String) {
                            val resDir = get("res")
                            val sourceFile =
                                listOf("layout-v31", "layout-v26", "layout").firstNotNullOfOrNull {
                                    resDir.resolve("$it/new_content_count.xml")
                                        .takeIf { f -> f.exists() }
                                } ?: return
                            val targetFile = resDir.resolve(resPath)
                            targetFile.parentFile?.mkdirs()
                            val patchedXml = sourceFile.readText().replace(
                                Regex("""android:textColor="[^"]+""""),
                                """android:textColor="$color""""
                            )
                            targetFile.writeText(patchedXml)
                        }

                        if (isMaterialYouDark) {
                            createNotifDrawable(
                                "drawable/revanced_notif_dot_dark.xml",
                                "@android:color/system_accent1_100",
                                "oval"
                            )
                            createNotifDrawable(
                                "drawable/revanced_notif_count_dark.xml",
                                "@android:color/system_accent1_100",
                                "rectangle",
                                hasCorners = true
                            )
                            patchCountTextColor(
                                "layout-v31/new_content_count.xml",
                                "@android:color/system_neutral1_900"
                            )
                        }
                        if (isMaterialYouLight) {
                            createNotifDrawable(
                                "drawable/revanced_notif_dot_light.xml",
                                "@android:color/system_accent1_200",
                                "oval"
                            )
                            createNotifDrawable(
                                "drawable/revanced_notif_count_light.xml",
                                "@android:color/system_accent1_100",
                                "rectangle",
                                hasCorners = true
                            )
                            patchCountTextColor(
                                "layout-v31/new_content_count.xml",
                                "@android:color/system_neutral1_900"
                            )
                        }

                        document("res/values/styles.xml").use { doc ->
                            val resources =
                                doc.getElementsByTagName("resources").item(0) as? Element
                                    ?: return@use

                            resources.forEachChildElement { style ->
                                if (style.nodeName != "style") return@forEachChildElement

                                val overrides: Map<String, String> =
                                    when (style.getAttribute("name")) {
                                        "PivotBar.Dark" -> if (isMaterialYouDark) mapOf(
                                            "dotBackground" to "@drawable/revanced_notif_dot_dark",
                                            "countBackground" to "@drawable/revanced_notif_count_dark"
                                        ) else return@forEachChildElement

                                        "PivotBar.Default" -> if (isMaterialYouLight) mapOf(
                                            "dotBackground" to "@drawable/revanced_notif_dot_light",
                                            "countBackground" to "@drawable/revanced_notif_count_light"
                                        ) else return@forEachChildElement

                                        "CairoLightThemeUpdates" -> if (isMaterialYouLight) mapOf(
                                            "ytRedIndicator" to "@android:color/system_accent1_200"
                                        ) else return@forEachChildElement

                                        "CairoDarkThemeUpdates" -> if (isMaterialYouDark) mapOf(
                                            "ytRedIndicator" to "@android:color/system_accent1_100"
                                        ) else return@forEachChildElement

                                        else -> return@forEachChildElement
                                    }

                                overrides.forEach { (attrName, attrValue) ->
                                    var found = false
                                    style.forEachChildElement { item ->
                                        if (item.nodeName == "item" && item.getAttribute("name") == attrName) {
                                            item.textContent = attrValue
                                            found = true
                                        }
                                    }
                                    if (!found) {
                                        style.appendChild(doc.createElement("item").apply {
                                            setAttribute("name", attrName)
                                            textContent = attrValue
                                        })
                                    }
                                }
                            }
                        }
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
                    // Create a night mode specific override for the splash screen background.
                    val style = document.createElement("style")
                    style.setAttribute("name", "Theme.YouTube.Home")
                    style.setAttribute("parent", "@style/Base.V27.Theme.YouTube.Home")

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

                    val resourcesNode =
                        document.getElementsByTagName("resources").item(0) as Element
                    resourcesNode.appendChild(style)
                }

                arrayOf(
                    "res/values/styles.xml",
                    "res/values-v27/styles.xml",
                    "res/values-v31/styles.xml"
                ).forEach { stylesPath ->
                    try {
                        document(stylesPath).use { document ->
                            val resourcesNode =
                                document.getElementsByTagName("resources").item(0) as? Element
                                    ?: return@use
                            var themeNode: Element? = null

                            resourcesNode.forEachChildElement { node ->
                                if (node.nodeName == "style" && node.getAttribute("name") == "Theme.YouTube.Home") {
                                    themeNode = node
                                }
                            }

                            if (themeNode == null) {
                                themeNode = document.createElement("style").apply {
                                    setAttribute("name", "Theme.YouTube.Home")
                                    setAttribute("parent", "@style/Base.V27.Theme.YouTube.Home")
                                    resourcesNode.appendChild(this)
                                }
                            }

                            var hasLightStatusBar = false
                            themeNode!!.forEachChildElement { node ->
                                if (node.nodeName == "item" && node.getAttribute("name") == "android:windowLightStatusBar") {
                                    node.textContent = "true"
                                    hasLightStatusBar = true
                                }
                            }

                            if (!hasLightStatusBar) {
                                val styleItem = document.createElement("item")
                                styleItem.setAttribute("name", "android:windowLightStatusBar")
                                styleItem.textContent = "true"
                                themeNode.appendChild(styleItem)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }

                val isMaterialYouDark =
                    darkThemeBackgroundColorOption.value!!.startsWith("@android:color/system_")
                val isMaterialYouLight =
                    lightThemeBackgroundColor!!.startsWith("@android:color/system_")

                // YouTube applies CairoLightThemeUpdates/CairoDarkThemeUpdates to the context theme
                // before Cairo renders, so ?ytRedIndicator in Cairo drawables resolves
                // from the style - not from night mode resource qualifiers.
                if (isMaterialYouDark || isMaterialYouLight) {
                    fun createNotifDrawable(
                        resPath: String,
                        color: String,
                        shape: String,
                        hasCorners: Boolean = false,
                    ) {
                        val file = get("res").resolve(resPath)
                        file.parentFile?.mkdirs()
                        val cornersLine = if (hasCorners)
                            "\n    <corners android:radius=\"@dimen/new_content_count_radius\" />"
                        else ""
                        file.writeText(
                            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                    "<shape android:shape=\"$shape\"\n" +
                                    "  xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                                    "    <solid android:color=\"$color\" />$cornersLine\n" +
                                    "</shape>"
                        )
                    }

                    fun patchCountTextColor(resPath: String, color: String) {
                        val resDir = get("res")
                        val sourceFile =
                            listOf("layout-v31", "layout-v26", "layout").firstNotNullOfOrNull {
                                resDir.resolve("$it/new_content_count.xml")
                                    .takeIf { f -> f.exists() }
                            } ?: return
                        val targetFile = resDir.resolve(resPath)
                        targetFile.parentFile?.mkdirs()
                        val patchedXml = sourceFile.readText().replace(
                            Regex("""android:textColor="[^"]+""""),
                            """android:textColor="$color""""
                        )
                        targetFile.writeText(patchedXml)
                    }

                    if (isMaterialYouDark) {
                        createNotifDrawable(
                            "drawable/revanced_notif_dot_dark.xml",
                            "@android:color/system_accent1_100",
                            "oval"
                        )
                        createNotifDrawable(
                            "drawable/revanced_notif_count_dark.xml",
                            "@android:color/system_accent1_100",
                            "rectangle",
                            hasCorners = true
                        )
                        patchCountTextColor(
                            "layout-v31/new_content_count.xml",
                            "@android:color/system_neutral1_900"
                        )
                    }
                    if (isMaterialYouLight) {
                        createNotifDrawable(
                            "drawable/revanced_notif_dot_light.xml",
                            "@android:color/system_accent1_200",
                            "oval"
                        )
                        createNotifDrawable(
                            "drawable/revanced_notif_count_light.xml",
                            "@android:color/system_accent1_100",
                            "rectangle",
                            hasCorners = true
                        )
                        patchCountTextColor(
                            "layout-v31/new_content_count.xml",
                            "@android:color/system_neutral1_900"
                        )
                    }

                    document("res/values/styles.xml").use { doc ->
                        val resources =
                            doc.getElementsByTagName("resources").item(0) as? Element ?: return@use

                        resources.forEachChildElement { style ->
                            if (style.nodeName != "style") return@forEachChildElement

                            val overrides: Map<String, String> = when (style.getAttribute("name")) {
                                "PivotBar.Dark" -> if (isMaterialYouDark) mapOf(
                                    "dotBackground" to "@drawable/revanced_notif_dot_dark",
                                    "countBackground" to "@drawable/revanced_notif_count_dark"
                                ) else return@forEachChildElement

                                "PivotBar.Default" -> if (isMaterialYouLight) mapOf(
                                    "dotBackground" to "@drawable/revanced_notif_dot_light",
                                    "countBackground" to "@drawable/revanced_notif_count_light"
                                ) else return@forEachChildElement

                                "CairoLightThemeUpdates" -> if (isMaterialYouLight) mapOf(
                                    "ytRedIndicator" to "@android:color/system_accent1_200"
                                ) else return@forEachChildElement

                                "CairoDarkThemeUpdates" -> if (isMaterialYouDark) mapOf(
                                    "ytRedIndicator" to "@android:color/system_accent1_100"
                                ) else return@forEachChildElement

                                else -> return@forEachChildElement
                            }

                            overrides.forEach { (attrName, attrValue) ->
                                var found = false
                                style.forEachChildElement { item ->
                                    if (item.nodeName == "item" && item.getAttribute("name") == attrName) {
                                        item.textContent = attrValue
                                        found = true
                                    }
                                }
                                if (!found) {
                                    style.appendChild(doc.createElement("item").apply {
                                        setAttribute("name", attrName)
                                        textContent = attrValue
                                    })
                                }
                            }
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
                    THEME_DEFAULT_DARK_COLOR_NAMES + if (is_21_06_or_greater)
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
                            // 21.17+
                            "yt_sys_color_baseline_mobile_dark_default_base_background",
                            "yt_sys_color_baseline_mobile_dark_default_raised_background",
                        ) else emptySet()
                },
                getLightColorNames = {
                    THEME_DEFAULT_LIGHT_COLOR_NAMES + if (is_21_06_or_greater)
                        setOf(
                            "yt_sys_color_baseline_light_base_background",
                            "yt_sys_color_baseline_light_raised_background",
                            // 21.17+
                            "yt_sys_color_baseline_mobile_light_default_base_background",
                            "yt_sys_color_baseline_mobile_light_default_raised_background",
                        )
                    else emptySet()
                }
            ),
            themeResourcePatch,
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
                "20.51.39"
            ),
        )
    },

    executeBlock = {
        addResources("youtube", "layout.theme.themePatch")

        PreferenceScreen.GENERAL.addPreferences(
            SwitchPreference("revanced_gradient_loading_screen"),
        )

        val preferences = mutableSetOf(
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

        if (is_21_08_or_greater) {
            carbonColorThemeFeatureFlagMethodMatch.method.insertLiteralOverride(
                carbonColorThemeFeatureFlagMethodMatch[0],
                false
            )
        }

        // Lottie splash screen exists in earlier versions, but it may not be always on.
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
