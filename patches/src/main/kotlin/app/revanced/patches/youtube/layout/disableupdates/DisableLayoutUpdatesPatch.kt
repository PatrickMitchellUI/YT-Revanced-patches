package app.revanced.patches.youtube.layout.disableupdates

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/DisableLayoutUpdatesPatch;"

@Suppress("unused")
val disableLayoutUpdatesPatch = bytecodePatch(
    name = "Disable layout updates",
    description = "Adds an option to disable server side layout updates and use an older UI.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
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
        addResources("youtube", "layout.disableLayoutUpdatesPatch")

        PreferenceScreen.GENERAL.addPreferences(
            SwitchPreference("revanced_disable_layout_updates"),
        )

        cronetHeaderMethodMatch.let {
            it.method.apply {
                val index = it[0]

                addInstructions(
                    index,
                    """
                        invoke-static { p1, p2 }, $EXTENSION_CLASS_DESCRIPTOR->disableLayoutUpdates(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p2
                    """,
                )
            }
        }
    }
}
