package app.revanced.patches.youtube.interaction.queue

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.firstClassDef
import app.revanced.patcher.firstImmutableClassDef
import app.revanced.patcher.firstMethod
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playservice.is_21_36_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.patches.youtube.video.videoid.hookBackgroundPlayVideoId
import app.revanced.patches.youtube.video.videoid.videoIdPatch
import app.revanced.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/PlaybackQueuePatch;"

private const val EXTENSION_REGISTRY_LITE = "Lcom/google/protobuf/ExtensionRegistryLite;"

@Suppress("unused")
val playbackQueuePatch = bytecodePatch(
    name = "Playback queue",
    description = "Makes \"Play next in queue\" and \"Play last in queue\" work without YouTube Premium.",
) {
    dependsOn(
        sharedExtensionPatch,
        addResourcesPatch,
        videoInformationPatch,
        videoIdPatch,
        versionCheckPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "20.40.45",
            "21.36.47"
        ),
    )

    apply {
        addResources("youtube", "interaction.queue.playbackQueuePatch")

        val routerMethod = commandRouterResolveMethod
        val routerType = routerMethod.definingClass
        val commandType = routerMethod.parameterTypes.first().toString()
        val routerClass = firstImmutableClassDef(routerType)

        // Returns the handler of a command, or a "no handler" singleton.
        val findHandlerMethod = routerClass.methods.single { method ->
            !AccessFlags.STATIC.isSet(method.accessFlags) &&
                method.parameterTypes.map { it.toString() } == listOf(commandType) &&
                method.returnType.startsWith("L")
        }
        val handlerType = findHandlerMethod.returnType
        val noHandlerField = firstImmutableClassDef(handlerType).fields.single { field ->
            AccessFlags.STATIC.isSet(field.accessFlags) && field.type == handlerType
        }

        // Command default instance, and parseFrom(default, bytes, registry) of the protobuf lite superclass.
        val commandDefaultInstanceField = firstImmutableClassDef(commandType).fields.first { field ->
            AccessFlags.STATIC.isSet(field.accessFlags) && field.type == commandType
        }
        var liteType = firstImmutableClassDef(commandType).superclass!!
        var parseFromMethod: Method? = null
        while (parseFromMethod == null) {
            val liteClass = firstImmutableClassDef(liteType)
            parseFromMethod = liteClass.methods.firstOrNull { method ->
                method.name == "parseFrom" && method.parameterTypes.map { it.toString() } ==
                    listOf(liteType, "[B", EXTENSION_REGISTRY_LITE)
            }
            if (parseFromMethod == null) liteType = liteClass.superclass!!
        }

        firstClassDef(EXTENSION_CLASS_DESCRIPTOR).apply {
            fun replaceStub(
                name: String,
                parameters: List<String>,
                returnType: String,
                registers: Int,
                smali: String,
            ) {
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
                        MutableMethodImplementation(registers),
                    ).toMutable().apply { addInstructionsWithLabels(0, smali) },
                )
            }

            replaceStub(
                "serializeMessage",
                listOf("Ljava/lang/Object;"),
                "[B",
                1,
                """
                    check-cast p0, Lcom/google/protobuf/MessageLite;
                    invoke-interface { p0 }, Lcom/google/protobuf/MessageLite;->toByteArray()[B
                    move-result-object p0
                    return-object p0
                """,
            )

            replaceStub(
                "parseCommand",
                listOf("[B"),
                "Ljava/lang/Object;",
                3,
                """
                    sget-object v0, $commandDefaultInstanceField
                    invoke-static { }, $EXTENSION_REGISTRY_LITE->getGeneratedRegistry()$EXTENSION_REGISTRY_LITE
                    move-result-object v1
                    invoke-static { v0, p0, v1 }, $parseFromMethod
                    move-result-object v0
                    return-object v0
                """,
            )

            replaceStub(
                "canResolveCommand",
                listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
                "Z",
                3,
                """
                    check-cast p0, $routerType
                    check-cast p1, $commandType
                    invoke-virtual { p0, p1 }, $findHandlerMethod
                    move-result-object v0
                    sget-object p0, $noHandlerField
                    if-eq v0, p0, :no_handler
                    const/4 v0, 0x1
                    return v0
                    :no_handler
                    const/4 v0, 0x0
                    return v0
                """,
            )

            replaceStub(
                "runCommand",
                listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
                "V",
                3,
                """
                    check-cast p0, $routerType
                    check-cast p1, $commandType
                    new-instance v0, Ljava/util/HashMap;
                    invoke-direct { v0 }, Ljava/util/HashMap;-><init>()V
                    invoke-virtual { p0, p1, v0 }, $routerMethod
                    return-void
                """,
            )
        }

        // Remember every router, as the routers that resolve the reload watch next command
        // do not always run a command before the queue is used.
        routerClass.methods.filter { AccessFlags.CONSTRUCTOR.isSet(it.accessFlags) }.forEach { constructor ->
            firstMethod(constructor).apply {
                findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
                    addInstruction(
                        index,
                        "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->routerCreated(Ljava/lang/Object;)V",
                    )
                }
            }
        }

        // Runs the handlers of a command. The resolve method calls it,
        // but 20.x also calls it directly, so hook it to see every command.
        val dispatchMethod = routerClass.methods.single { method ->
            method != routerMethod && !AccessFlags.STATIC.isSet(method.accessFlags) && method.returnType == "V" &&
                method.parameterTypes.map { it.toString() } == listOf(commandType, "Ljava/util/Map;")
        }

        firstMethod(dispatchMethod).addInstructionsWithLabels(
            0,
            """
                invoke-static { p0, p1 }, $EXTENSION_CLASS_DESCRIPTOR->handleCommand(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v0
                if-nez v0, :run_command
                return-void
                :run_command
                check-cast v0, $commandType
                move-object p1, v0
            """,
        )

        // Hide the Premium queue item of playlist panel menus. Menus are built by one of two methods.
        val menuItemRendererType = bottomSheetMenuItemMethod.parameterTypes.first().toString()

        // Instruction that returns an absent item, before the first return.
        val absentItemField = bottomSheetMenuItemMethod.implementation!!.instructions.toList().let { instructions ->
            val returnIndex = instructions.indexOfFirst { it.opcode == Opcode.RETURN_OBJECT }
            (instructions[returnIndex - 1] as ReferenceInstruction).reference
        }

        fun MutableMethod.hideMenuItemAtStart() = addInstructionsWithLabels(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->hideMenuItem(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :show_item
                sget-object v0, $absentItemField
                return-object v0
                :show_item
                nop
            """,
        )

        bottomSheetMenuItemMethod.hideMenuItemAtStart()

        // 20.x builds the other menu from the renderer, like the bottom sheet.
        if (!is_21_36_or_greater) {
            sheetMenuItemLegacyMethod.hideMenuItemAtStart()
        } else sheetMenuItemFromModelMethod.apply {
            val rendererIndex = implementation!!.instructions.indexOfFirst { instruction ->
                instruction.opcode == Opcode.IGET_OBJECT &&
                    ((instruction as ReferenceInstruction).reference as FieldReference).type == menuItemRendererType
            }
            val rendererRegister = getInstruction<TwoRegisterInstruction>(rendererIndex).registerA
            val freeRegister = if (rendererRegister == 0) 1 else 0

            addInstructionsWithLabels(
                rendererIndex + 1,
                """
                    invoke-static { v$rendererRegister }, $EXTENSION_CLASS_DESCRIPTOR->hideMenuItem(Ljava/lang/Object;)Z
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :show_item
                    invoke-static { }, Lj${'$'}/util/Optional;->empty()Lj${'$'}/util/Optional;
                    move-result-object v$freeRegister
                    return-object v$freeRegister
                    :show_item
                    nop
                """,
            )
        }

        hookBackgroundPlayVideoId("$EXTENSION_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")
    }
}
