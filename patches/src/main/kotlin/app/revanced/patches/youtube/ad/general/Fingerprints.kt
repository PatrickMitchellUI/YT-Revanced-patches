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
                name == "add" && returnType == "Z" &&
                    parameterTypes.size == 1 && parameterTypes[0] == "Ljava/lang/Object;"
            },
        ),
        allOf(
            Opcode.INVOKE_VIRTUAL(),
            method {
                name == "add" && returnType == "Z" &&
                    parameterTypes.size == 1 && parameterTypes[0] == "Ljava/lang/Object;"
            },
        ),
        allOf(
            Opcode.INVOKE_VIRTUAL(),
            method { name == "size" && returnType == "I" && parameterTypes.isEmpty() },
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
        Opcode.RETURN_VOID,
    )
}

internal val BytecodePatchContext.playerOverlayTimelyShelfMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Ljava/lang/Object;")
    instructions(
        "player_overlay_timely_shelf"(),
        afterAtMost(5, method("equals")),
        after(Opcode.MOVE_RESULT()),
    )
}

internal val BytecodePatchContext.miniplayerPaidPromotionLabelMethodMatch by composingFirstMethod {
    instructions(
        ResourceType.ID("modern_miniplayer_subtitle_text"),
        after(Opcode.INVOKE_VIRTUAL()),
        after(Opcode.MOVE_RESULT_OBJECT()),
    )
}
