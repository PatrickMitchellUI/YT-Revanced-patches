package app.revanced.patches.youtube.layout.buttons.action

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.firstClassDef
import app.revanced.patcher.firstMethod
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.shared.mainActivityOnCreateMethod
import app.revanced.util.removeFlags
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/ActionButtonStylePatch;"

private const val COUNTS_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/CompactActionBarCounts;"

internal val actionButtonStylePatch = bytecodePatch(
    description = "Adds an option to use the old action buttons, and hides old action buttons.",
) {
    dependsOn(
        sharedExtensionPatch,
    )

    apply {
        // The old action bar is built from the new element, see OldActionBarBuilder.
        val itemClass = firstClassDef(createElementItemMethod.returnType)
        val itemFactoryInterface = createElementItemMethod.let { method ->
            firstClassDef(method.definingClass).interfaces.single { interfaceType ->
                firstClassDef(interfaceType).methods.any { it.returnType == itemClass.type }
            }
        }
        val elementBytesField = itemClass.fields.single { it.type == "[B" }.apply {
            removeFlags(AccessFlags.FINAL)
        }
        val itemConstructor = itemClass.methods.single { method ->
            method.name == "<init>" && method.parameterTypes.size == 1
        }
        val rendererField = itemClass.fields.single { it.type == itemConstructor.parameterTypes.first().toString() }

        // Method that adds the item of an element renderer to the list. Its list parameter collects the items.
        var addItemMethod: Method? = null
        classDefs.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (method.returnType != "V" || method.parameterTypes.size != 2 ||
                    method.parameterTypes.first() != "Ljava/lang/Object;"
                ) return@forEach

                val instructions = method.implementation?.instructions?.toList() ?: return@forEach
                val createIndex = instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.INVOKE_INTERFACE &&
                        instruction.methodReference?.definingClass == itemFactoryInterface &&
                        instruction.methodReference?.returnType == itemClass.type
                }
                if (createIndex < 0) return@forEach
                val addInstruction = instructions.getOrNull(createIndex + 2)
                val addReference = addInstruction?.methodReference
                if (addInstruction?.opcode != Opcode.INVOKE_INTERFACE ||
                    addReference?.parameterTypes?.map { it.toString() } != listOf("Ljava/lang/Object;") ||
                    addReference.definingClass != method.parameterTypes[1].toString()
                ) return@forEach

                if (addItemMethod != null) throw PatchException("Found more than one add element item method")
                addItemMethod = method
            }
        }
        val addItem = firstMethod(addItemMethod ?: throw PatchException("Add element item method not found"))
        val addIndex = addItem.implementation!!.instructions.indexOfLast { it.opcode == Opcode.INVOKE_INTERFACE }
        val addInstruction = addItem.getInstruction<FiveRegisterInstruction>(addIndex)
        val addReference = addItem.getInstruction(addIndex).methodReference!!

        addItem.addInstructions(
            addIndex,
            "invoke-static { v${addInstruction.registerD}, v${addInstruction.registerC} }, " +
                "$EXTENSION_CLASS_DESCRIPTOR->addOldActionBar(Ljava/lang/Object;Ljava/lang/Object;)V",
        )

        // The counts of the new action bar are drawn on its views.
        mainActivityOnCreateMethod.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $COUNTS_CLASS_DESCRIPTOR->setMainActivity(Landroid/app/Activity;)V",
        )

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
                "getElementBytes",
                listOf("Ljava/lang/Object;"),
                "[B",
                """
                    check-cast p0, ${itemClass.type}
                    iget-object v0, p0, $elementBytesField
                    return-object v0
                """,
            )

            replaceStub(
                "setElementBytes",
                listOf("Ljava/lang/Object;", "[B"),
                "V",
                """
                    check-cast p0, ${itemClass.type}
                    iput-object p1, p0, $elementBytesField
                    return-void
                """,
            )

            replaceStub(
                "copyItem",
                listOf("Ljava/lang/Object;"),
                "Ljava/lang/Object;",
                """
                    check-cast p0, ${itemClass.type}
                    iget-object p0, p0, $rendererField
                    new-instance v0, ${itemClass.type}
                    invoke-direct { v0, p0 }, $itemConstructor
                    return-object v0
                """,
            )

            replaceStub(
                "addToList",
                listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
                "V",
                """
                    check-cast p0, ${addReference.definingClass}
                    invoke-interface { p0, p1 }, $addReference
                    return-void
                """,
            )
        }
    }
}
