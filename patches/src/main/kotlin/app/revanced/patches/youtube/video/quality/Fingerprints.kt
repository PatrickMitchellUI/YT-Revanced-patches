package app.revanced.patches.youtube.video.quality

import app.revanced.patcher.CompositeMatch
import app.revanced.patcher.accessFlags
import app.revanced.patcher.after
import app.revanced.patcher.afterAtMost
import app.revanced.patcher.allOf
import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.custom
import app.revanced.patcher.definingClass
import app.revanced.patcher.field
import app.revanced.patcher.firstImmutableMethodDeclaratively
import app.revanced.patcher.firstMethodComposite
import app.revanced.patcher.firstMethodDeclaratively
import app.revanced.patcher.gettingFirstImmutableMethodDeclaratively
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.name
import app.revanced.patcher.opcodes
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import app.revanced.patcher.type
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.util.findFieldFromToString
import app.revanced.util.getting
import app.revanced.util.using
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.newAdvancedQualityMenuStyleFlyoutMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    instructions(45712556L())
}

internal val BytecodePatchContext.currentVideoFormatToStringMethod by gettingFirstImmutableMethodDeclaratively(
    "currentVideoFormat=",
) {
    name("toString")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Ljava/lang/String;")
    parameterTypes()
}

internal val BytecodePatchContext.defaultOverflowOverlayOnClickMethodMatch by composingFirstMethod {
    name("onClick")
    definingClass("Lcom/google/android/libraries/youtube/player/features/overlay/overflow/ui/DefaultOverflowOverlay;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/view/View;")

    var methodDefiningClass = ""
    custom {
        methodDefiningClass = definingClass
        true
    }

    instructions(
        Opcode.IF_NE(),
        afterAtMost(2, allOf(Opcode.IGET_OBJECT(), field { definingClass == methodDefiningClass })),
    )
}

internal val BytecodePatchContext.hidePremiumVideoQualityGetArrayMethod by gettingFirstMethodDeclaratively {
    name("apply")
    definingClass("Lapp/revanced/extension/youtube/patches/playback/quality/HidePremiumVideoQualityPatch")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Ljava/lang/Object;")
    parameterTypes("I")
    custom { AccessFlags.SYNTHETIC.isSet(immutableClassDef.accessFlags) }
}

internal const val FIXED_RESOLUTION_STRING = ", initialPlaybackVideoQualityFixedResolution="

internal fun BytecodePatchContext.getPlaybackStartParametersConstructorMethod(): CompositeMatch {
    val playbackStartParametersToStringMethod =
        firstMethodDeclaratively(
            FIXED_RESOLUTION_STRING,
        ) {
            name("toString")
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returnType("Ljava/lang/String;")
            parameterTypes()
        }

    val initialResolutionField =
        playbackStartParametersToStringMethod
            .findFieldFromToString(FIXED_RESOLUTION_STRING)

    // Inject a call to override initial video quality.
    return playbackStartParametersToStringMethod.immutableClassDef.firstMethodComposite {
        name("<init>")
        instructions(
            allOf(Opcode.IPUT_OBJECT(), field { this == initialResolutionField }),
        )
    }
}

internal val BytecodePatchContext.videoQualityItemOnClickMethod by getting {
    firstMethodDeclaratively {
        name("onItemClick")
        returnType("V")
        parameterTypes(
            "Landroid/widget/AdapterView;",
            "Landroid/view/View;",
            "I",
            "J",
        )
    }
} using {
    firstImmutableMethodDeclaratively("VIDEO_QUALITIES_MENU_BOTTOM_SHEET_FRAGMENT") {
        returnType("V")
    }
}

internal val BytecodePatchContext.videoQualityMenuOptionsMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.STATIC)
    returnType("[L")
    parameterTypes("Landroid/content/Context", "L", "L")
    instructions(
        Opcode.CONST_4(), // First instruction of method.
        after(Opcode.CONST_4()),
        after(Opcode.IF_EQZ()),
        after(Opcode.IGET_BOOLEAN()), // Use the quality menu, that contains the advanced menu.
        after(Opcode.IF_NEZ()),
        ResourceType.STRING("video_quality_quick_menu_advanced_menu_description"),
    )
}

internal val BytecodePatchContext.videoQualityMenuViewInflateMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    parameterTypes("L", "L", "L")
    instructions(
        Opcode.INVOKE_SUPER(),
        after(Opcode.CONST()),
        after(Opcode.CONST_4()),
        after(Opcode.INVOKE_VIRTUAL()),
        after(Opcode.MOVE_RESULT_OBJECT()),
        after(Opcode.CONST()),
        after(Opcode.INVOKE_VIRTUAL()),
        after(Opcode.MOVE_RESULT_OBJECT()),
        after(Opcode.CONST_16()),
        after(Opcode.INVOKE_VIRTUAL()),
        after(Opcode.CONST()),
        after(Opcode.INVOKE_VIRTUAL()),
        after(Opcode.MOVE_RESULT_OBJECT()),
        after(Opcode.CHECK_CAST()),
        ResourceType.LAYOUT("video_quality_bottom_sheet_list_fragment_title"),
    )
}

private const val STREAMING_DATA_CLASS_DESCRIPTOR =
    $$"Lcom/google/protos/youtube/api/innertube/StreamingDataOuterClass$StreamingData;"

internal fun BytecodePatchContext.getVideoStreamingDataConstructorMethod(): CompositeMatch {
    val videoStreamingDataToStringMethod =
        firstImmutableMethodDeclaratively(
            "VideoStreamingData(itags=",
        ) {
            name("toString")
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returnType("Ljava/lang/String;")
            parameterTypes()
        }

    val videoStreamingDataClassDef = videoStreamingDataToStringMethod.immutableClassDef

    return videoStreamingDataClassDef.firstMethodComposite {
        accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
        returnType("V")
        instructions(
            allOf(Opcode.IGET_OBJECT(), field { definingClass == STREAMING_DATA_CLASS_DESCRIPTOR }),
            allOf(
                Opcode.IGET_OBJECT(),
                field {
                    definingClass == videoStreamingDataClassDef.type && type == "Ljava/lang/String;"
                },
            ),
            allOf(Opcode.NEW_INSTANCE(), type("Ljava/util/ArrayList;")),
            allOf(Opcode.IGET_OBJECT(), field { definingClass == STREAMING_DATA_CLASS_DESCRIPTOR }),
        )
    }
}
