package app.revanced.patches.youtube.layout.sharesheet

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.litho.filter.addLithoFilter
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.litho.filter.lithoFilterPatch
import app.revanced.patches.youtube.misc.recyclerviewtree.hook.addRecyclerViewTreeHook
import app.revanced.patches.youtube.misc.recyclerviewtree.hook.recyclerViewTreeHookPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/OpenSystemShareSheetPatch;"

private const val FILTER_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/litho/OpenSystemShareSheetFilter;"

@Suppress("unused")
val openSystemShareSheetPatch =
    bytecodePatch(
        name = "Open system share sheet",
        description = "Adds an option to always open the system share sheet instead of the in-app share sheet.",
    ) {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            addResourcesPatch,
            lithoFilterPatch,
            recyclerViewTreeHookPatch,
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
            addResources("youtube", "misc.sharesheet.openSystemShareSheetPatch")

            PreferenceScreen.GENERAL.addPreferences(
                SwitchPreference("revanced_open_system_share_sheet"),
            )

            addRecyclerViewTreeHook(EXTENSION_CLASS_DESCRIPTOR)

            initializeShareSheetPanelContentMethod.addInstruction(
                0,
                "invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->openSystemShareSheet()V",
            )

            addLithoFilter(FILTER_CLASS_DESCRIPTOR)
        }
    }
