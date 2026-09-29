package app.revanced.patches.youtube.layout.feedspacing

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.firstClassDef
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/FeedSpacingPatch;"

@Suppress("unused")
val feedSpacingPatch = bytecodePatch(
    name = "Feed spacing",
    description = "Adds an option to make the space below videos the same in the home, subscriptions, " +
        "search results and watch page feeds.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        navigationBarHookPatch,
    )

    compatibleWith("com.google.android.youtube"("21.36.47"))

    apply {
        addResources("youtube", "layout.feedspacing.feedSpacingPatch")

        PreferenceScreen.FEED.addPreferences(
            SwitchPreference("revanced_feed_spacing"),
        )

        val getChildPositionMethod = feedItemDecorationGetItemOffsetsMethod.let { method ->
            val index = method.indexOfFirstInstructionOrThrow {
                val reference = methodReference
                opcode == Opcode.INVOKE_VIRTUAL && reference?.definingClass == RECYCLER_VIEW_CLASS_TYPE &&
                    reference.returnType == "I" && reference.parameterTypes.map { it.toString() } ==
                    listOf("Landroid/view/View;")
            }
            method.implementation!!.instructions.elementAt(index).methodReference!!
        }
        val invalidateItemDecorationsMethod = recyclerViewInvalidateItemDecorationsMethod

        firstClassDef(EXTENSION_CLASS_DESCRIPTOR).apply {
            fun replaceStub(name: String, parameters: List<String>, returnType: String, smali: String) {
                methods.removeIf { it.name == name }
                methods.add(
                    ImmutableMethod(
                        type,
                        name,
                        parameters.map { ImmutableMethodParameter(it, null, null) },
                        returnType,
                        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
                        null,
                        null,
                        MutableMethodImplementation(parameters.size + 1),
                    ).toMutable().apply { addInstructionsWithLabels(0, smali) },
                )
            }

            replaceStub(
                "getChildPosition",
                listOf("Landroid/view/ViewGroup;", "Landroid/view/View;"),
                "I",
                """
                    check-cast p0, $RECYCLER_VIEW_CLASS_TYPE
                    invoke-virtual { p0, p1 }, $getChildPositionMethod
                    move-result v0
                    return v0
                """,
            )

            replaceStub(
                "invalidateItemDecorations",
                listOf("Landroid/view/ViewGroup;"),
                "V",
                """
                    check-cast p0, $RECYCLER_VIEW_CLASS_TYPE
                    invoke-virtual { p0 }, $invalidateItemDecorationsMethod
                    return-void
                """,
            )
        }

        recyclerViewGetItemDecorInsetsMethod.apply {
            // After all decorations were added up, the insets are returned.
            val returnIndex = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_OBJECT)
            val insetsRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA

            addInstructionsAtControlFlowLabel(
                returnIndex - 1,
                "invoke-static { v$insetsRegister, p1, p0 }, $EXTENSION_CLASS_DESCRIPTOR->" +
                    "addItemOffsets(Landroid/graphics/Rect;Landroid/view/View;Landroid/view/ViewGroup;)V",
            )
        }
    }
}
