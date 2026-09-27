package app.revanced.patches.youtube.ad.general

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.fullScreenEngagementAdContainerMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes()
    instructions(
        ResourceType.ID("fullscreen_engagement_ad_container"),
        Opcode.IGET_BOOLEAN(),
        allOf(
            Opcode.INVOKE_VIRTUAL(),
            method {
                name == "add" && returnType == "Z"
                        && parameterTypes.size == 1 && parameterTypes[0] == "Ljava/lang/Object;"
            }
        ),
        allOf(
            Opcode.INVOKE_VIRTUAL(),
            method {
                name == "add" && returnType == "Z"
                        && parameterTypes.size == 1 && parameterTypes[0] == "Ljava/lang/Object;"
            }
        ),
        allOf(
            Opcode.INVOKE_VIRTUAL(),
            method { name == "size" && returnType == "I" && parameterTypes.isEmpty() }
        ),
    )
}

internal val BytecodePatchContext.getPremiumViewMethodMatch by composingFirstMethod {
    name("onMeasure")
    definingClass("Lcom/google/android/apps/youtube/app/red/presenter/CompactYpcOfferModuleView;")
    accessFlags(AccessFlags.PROTECTED, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("I", "I")
    opcodes(
        Opcode.ADD_INT_2ADDR,
        Opcode.ADD_INT_2ADDR,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    )
}

/**
 * Constructor of the compact link presenter, used for the entries in the You tab.
 */
internal val BytecodePatchContext.compactLinkPresenterConstructorMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    instructions(ResourceType.LAYOUT("compact_list_item"))
}

/**
 * 21.36+ Fills in a settings entry from the server data, or removes the entry if there is no data.
 * Parameters: preference key, server data, list of preferences to remove.
 */
internal val BytecodePatchContext.settingsServerEntryMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Ljava/lang/String;", "Lj$/util/Optional;", "Ljava/util/List;")
    instructions(
        method { definingClass == "Lj$/util/Optional;" && name == "ofNullable" },
        method { definingClass == "Lj$/util/Optional;" && name == "isEmpty" },
        method { definingClass == "Ljava/util/List;" && name == "add" },
    )
}

internal val BytecodePatchContext.lithoDialogBuilderMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("[B", "L")
    instructions(
        allOf(Opcode.INVOKE_VIRTUAL(), method("show")),
        ResourceType.STYLE("SlidingDialogAnimation")
    )
}


internal val BytecodePatchContext.playerOverlayTimelyShelfMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Ljava/lang/Object;")
    instructions(
        allOf(Opcode.IGET_OBJECT(), field { type == "Ljava/lang/String;" }),
        after("player_overlay_timely_shelf"()),
        after(method { name == "equals" && definingClass == "Ljava/lang/String;" }),
    )
}

