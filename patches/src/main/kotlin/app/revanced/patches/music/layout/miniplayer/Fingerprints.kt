package app.revanced.patches.music.layout.miniplayer

import app.revanced.patcher.*
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.typeReference
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.util.getting
import app.revanced.util.using
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.miniPlayerConstructorMethodMatch by composingFirstMethod(
    "sharedToggleMenuItemMutations",
) {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    instructions(ResourceType.ID("music_playback_controls"))
}

internal val BytecodePatchContext.switchToggleColorMethodMatch by getting {
    firstMethodComposite {
        accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
        returnType("V")
        parameterTypes("L", "J")
        instructions(
            allOf(
                Opcode.INVOKE_VIRTUAL(),
                method { returnType.startsWith("L") && parameterTypes.isEmpty() },
            ),
            after(Opcode.MOVE_RESULT_OBJECT()),
            after(Opcode.CHECK_CAST()),
            afterAtMost(5, Opcode.GOTO()),
            allOf(Opcode.IGET(), field { type == "I" }),
            after(Opcode.INVOKE_VIRTUAL()),
        )
    }
} using { miniPlayerConstructorMethodMatch.method }

internal val BytecodePatchContext.minimizedPlayerMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L", "L")
    instructions("w_st"())
}

internal val BytecodePatchContext.interactionLoggingEnumMethod by composingFirstMethod {
    returnType("V")
    instructions(
        "INTERACTION_LOGGING_GESTURE_TYPE_SWIPE"(),
        Opcode.SPUT_OBJECT(),
    )
}

internal val BytecodePatchContext.musicActivityWidgetMethod by composingFirstMethod {
    definingClass("Lcom/google/android/apps/youtube/music/activities/MusicActivity;")
    name("onCreate")
    instructions(
        "widget_key"(),
        afterAtMost(5, Opcode.IGET_OBJECT()),
        afterAtMost(
            3,
            allOf(
                Opcode.INVOKE_INTERFACE(),
                method { parameterTypes.isEmpty() && returnType == "Ljava/lang/Object;" },
            ),
        ),
        afterAtMost(3, Opcode.CHECK_CAST()),
        afterAtMost(3, Opcode.SGET_OBJECT()),
        afterAtMost(3, Opcode.NEW_INSTANCE()),
        afterAtMost(3, 79500L()),
        afterAtMost(3, allOf(Opcode.INVOKE_STATIC(), method { parameterTypes == listOf("I") })),
        afterAtMost(
            3,
            allOf(
                Opcode.INVOKE_DIRECT(),
                method { name == "<init>" && parameterTypes.size == 1 },
            ),
        ),
        afterAtMost(
            3,
            allOf(
                Opcode.INVOKE_INTERFACE(),
                method { parameterTypes.size == 3 && returnType == "V" },
            ),
        ),
    )
}

// 9.03+
internal val BytecodePatchContext.coldStartMiniPlayerDefaultTextCallingMethodMatch by composingFirstMethod {
    val defaultTextMethod =
        contextOf<BytecodePatchContext>().firstImmutableMethodDeclaratively {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returnType("V")
            parameterTypes()
            instructions(ResourceType.STRING("mini_player_default_text"))
        }

    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("Ljava/lang/Object;")
    returnType("V")
    instructions(
        Opcode.IF_NE(),
        allOf(Opcode.INVOKE_VIRTUAL(), method { this == defaultTextMethod }),
    )
}

// 9.02 and lower.
internal val BytecodePatchContext.coldStartMiniPlayerDefaultTextLegacyMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Ljava/lang/Object;")
    instructions(
        Opcode.IF_NE(),
        afterAtMost(50, ResourceType.STRING("mini_player_default_text")),
    )
}

// 9.02+
internal val BytecodePatchContext.playerPageBehaviorMethod by gettingFirstMethodDeclaratively {
    definingClass("Lcom/google/android/apps/youtube/music/watchpage/ui/PlayerPageBehavior;")
    accessFlags(AccessFlags.FINAL)
    returnType("V")
    parameterTypes()
    instructions(
        Opcode.CONST_4(),
        after(Opcode.IPUT_BOOLEAN()),
        after(Opcode.RETURN_VOID()),
    )
}

internal fun BytecodePatchContext.getOnWatchWhileDismissedMethodMatch(musicActivityPeerDefiningClass: String) =
    firstMethodComposite {
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        parameterTypes()
        returnType("V")
        instructions(
            allOf(
                Opcode.IGET_OBJECT(),
                field {
                    definingClass == musicActivityPeerDefiningClass &&
                        type == "Ljava/util/concurrent/atomic/AtomicBoolean;"
                },
            ),
            afterAtMost(
                3,
                allOf(
                    Opcode.INVOKE_VIRTUAL(),
                    method {
                        definingClass == "Ljava/util/concurrent/atomic/AtomicBoolean;" &&
                            name == "set" &&
                            returnType == "V" &&
                            parameterTypes.size == 1 && parameterTypes.first().toString() == "Z"
                    },
                ),
            ),
        )
    }

internal val BytecodePatchContext.warmStartMiniplayerMethodMatch by composingFirstMethod {
    /**
     * Matches the watch-while layout's onFinishInflate() method.
     * definingClass uses a contains match, covering class renames across builds:
     *   <= 8.x: MppWatchWhileLayout
     *   >= 9.x: WatchWhileLayout
     */
    val watchWhileLayoutMethod =
        contextOf<BytecodePatchContext>().firstMethodComposite {
            definingClass("WatchWhileLayout;")
            name("onFinishInflate")
            returnType("V")
            instructions(
                // <= 8.x: MppPlayerPageBehavior
                // >= 9.x: PlayerPageBehavior
                allOf(
                    Opcode.IPUT_OBJECT(),
                    field { definingClass.endsWith("PlayerPageBehavior;") },
                ),
                afterAtMost(3, Opcode.NEW_INSTANCE()),
            )
        }

    val watchWhileLayoutDefiningClass = watchWhileLayoutMethod.method.definingClass
    val warmStartMiniplayerDefiningClass =
        watchWhileLayoutMethod.method
            .getInstruction(watchWhileLayoutMethod[-1])
            .typeReference!!
            .type

    definingClass(warmStartMiniplayerDefiningClass)
    parameterTypes("Landroid/view/View;", "I")
    instructions(
        allOf(
            Opcode.IGET_OBJECT(),
            field { definingClass == watchWhileLayoutDefiningClass },
        ),
        afterAtMost(
            5,
            allOf(
                Opcode.INVOKE_VIRTUAL(),
                method {
                    definingClass == "Lcom/google/android/material/bottomsheet/BottomSheetBehavior;" &&
                        returnType == "V" &&
                        parameterTypes.size == 1 && parameterTypes.first() == "Z"
                },
            ),
        ),
    )
}
