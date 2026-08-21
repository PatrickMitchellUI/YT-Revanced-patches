package app.revanced.patches.shared.misc.spannablestring

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.classDef
import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.findFreeRegister
import app.revanced.util.fiveRegisterInstructionRegistersAsString
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/shared/patches/span/HookSpannableStringPatch;"

// Registers used in extension helperMethod.
private const val REGISTER_FILTER_CLASS = 0
private const val REGISTER_FILTER_COUNT = 1
private const val REGISTER_FILTER_ARRAY = 2

private lateinit var helperMethod: MutableMethod
private var addSpanFilterCount = 0

private var spannableStringIndex = -1
private var spannableStringRegister = -1
private var spannableStringContextRegister = -1
private lateinit var buildSpannableStringMethod: MutableMethod

val hookSpanPatch =
    bytecodePatch(description = "Hooks the spannable string builder.") {
        apply {
            buildSpannableStringMethodMatch.let { match ->
                match.method.apply {
                    buildSpannableStringMethod = this

                    spannableStringIndex = match[0]
                    spannableStringRegister = getInstruction<FiveRegisterInstruction>(spannableStringIndex).registerC
                    spannableStringContextRegister = findFreeRegister(spannableStringIndex, spannableStringRegister)

                    addInstructionsAtControlFlowLabel(
                        spannableStringIndex++,
                        "move-object/from16 v$spannableStringContextRegister, p0",
                    )

                    val setSpanIndex = match[1, 0]

                    replaceInstruction(
                        match[1, 0],
                        "invoke-static { ${fiveRegisterInstructionRegistersAsString(setSpanIndex)} }, " +
                            "$EXTENSION_CLASS_DESCRIPTOR->setSpan(Landroid/text/SpannableString;Ljava/lang/Object;III)V",
                    )
                }
            }

            hookSpannableString(
                classDescriptor = EXTENSION_CLASS_DESCRIPTOR,
                methodName = "setConversionContext",
            )

            getSpanTypeMethodMatch.let { match ->
                match.method.apply {
                    val index = match[0]
                    val instruction = getInstruction<TwoRegisterInstruction>(index)

                    replaceInstruction(
                        index,
                        "instance-of v${instruction.registerA}, v${instruction.registerB}, " +
                            customCharacterStyleMethod.classDef.type,
                    )
                }
            }

            // Remove dummy filter from extension static field and add the filters included during patching.
            spannableStringMethodMatch.let {
                it.method.apply {
                    // Add a helper method to avoid finding multiple free registers.
                    // This fixes an issue with extension compiled with Android Gradle Plugin 8.3.0+.
                    helperMethod =
                        ImmutableMethod(
                            definingClass,
                            "patch_getFilterArray",
                            listOf(),
                            "[Lapp/revanced/extension/shared/patches/span/SpannableStringFilter;",
                            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
                            null,
                            null,
                            MutableMethodImplementation(3),
                        ).toMutable().also(it.classDef.methods::add)

                    val insertIndex = it[0]
                    val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex,
                        """
                            invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->patch_getFilterArray()[Lapp/revanced/extension/shared/patches/span/SpanFilter;
                            move-result-object v$insertRegister
                        """,
                    )
                }
            }
        }

        afterDependents {
            helperMethod.apply {
                addInstruction(instructions.size, "return-object v$REGISTER_FILTER_ARRAY")

                addInstructions(
                    0,
                    """
                        const/16 v$REGISTER_FILTER_COUNT, $addSpanFilterCount
                        new-array v$REGISTER_FILTER_ARRAY, v$REGISTER_FILTER_COUNT, [Lapp/revanced/extension/shared/patches/span/SpanFilter;
                    """,
                )
            }
        }
    }

fun addSpannableStringFilter(classDescriptor: String) {
    helperMethod.addInstructions(
        0,
        """
            new-instance v$REGISTER_FILTER_CLASS, $classDescriptor
            invoke-direct { v$REGISTER_FILTER_CLASS }, $classDescriptor-><init>()V
            const/16 v$REGISTER_FILTER_COUNT, ${addSpanFilterCount++}
            aput-object v$REGISTER_FILTER_CLASS, v$REGISTER_FILTER_ARRAY, v$REGISTER_FILTER_COUNT
        """,
    )
}

internal fun hookSpannableString(
    classDescriptor: String,
    methodName: String = "onLithoTextLoaded",
    overrideSpan: Boolean = false,
) = buildSpannableStringMethod.apply {
    if (overrideSpan) {
        addInstructions(
            spannableStringIndex,
            """
                invoke-static { v$spannableStringContextRegister, v$spannableStringRegister }, $classDescriptor->$methodName(Ljava/lang/Object;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                move-result-object v$spannableStringRegister
            """,
        )
        spannableStringIndex += 2
    } else {
        addInstruction(
            spannableStringIndex++,
            "invoke-static { v$spannableStringContextRegister, v$spannableStringRegister }, $classDescriptor->$methodName(Ljava/lang/Object;Ljava/lang/CharSequence;)V",
        )
    }
}
