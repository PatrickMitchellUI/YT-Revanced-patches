package app.revanced.patches.youtube.interaction.dialog

import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.invoke
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.getMethodReferenceAtInstructionIndex
import com.android.tools.smali.dexlib2.Opcode

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/RemoveViewerDiscretionDialogPatch;"

@Suppress("unused")
val removeViewerDiscretionDialogPatch = bytecodePatch(
    name = "Remove viewer discretion dialog",
    description = "Adds an option to remove the dialog that appears when opening a video that has been age-restricted " +
            "by accepting it automatically. This does not bypass the age restriction.",
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
            "20.47.62"
        ),
    )

    apply {
        addResources("youtube", "interaction.dialog.removeViewerDiscretionDialogPatch")

        PreferenceScreen.GENERAL.addPreferences(
            SwitchPreference("revanced_remove_viewer_discretion_dialog"),
        )

        allowControversialContentMethodMatch.let {
            val allowControversialContentMethod = it.getMethodReferenceAtInstructionIndex(2)

            allowControversialContentMethod.apply {
                findInstructionIndicesReversedOrThrow(Opcode.RETURN).forEach { index ->
                    addInstructionsWithLabels(
                        index,
                        """
                            invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideViewDiscretionDialog()Z
                            move-result v0
                            if-eqz v0, :show_controversial_content_confirmation_box
                            return v0
                            :show_controversial_content_confirmation_box
                            nop
                        """,
                    )
                }
            }

            allowControversialContentMethod.immutableClassDef.allowAdultContentMethod().addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideViewDiscretionDialog()Z
                    move-result v0
                    if-eqz v0, :show_adult_content_confirmation_box
                    sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                    return-object v0
                    :show_adult_content_confirmation_box
                    nop
                """,
            )
        }
    }
}
