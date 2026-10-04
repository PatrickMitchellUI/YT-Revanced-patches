package app.revanced.patches.youtube.interaction.seekbar

import app.revanced.patcher.*
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.featureflag.FeatureFlag
import app.revanced.util.getting
import app.revanced.util.literal
import app.revanced.util.using
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

internal val BytecodePatchContext.swipingUpGestureParentMethod by gettingFirstImmutableMethodDeclaratively {
    returnType("Z")
    parameterTypes()
    instructions(
        FeatureFlag.SWIPE_UP_TO_FULLSCREEN(),
    )
}

/**
 * Resolves using the class found in [swipingUpGestureParentMethod].
 */
context(_: BytecodePatchContext)
internal fun ClassDef.getShowSwipingUpGuideMethod() = firstMethodDeclaratively {
    accessFlags(AccessFlags.FINAL)
    returnType("Z")
    parameterTypes()
    instructions(1L())
}

/**
 * Resolves using the class found in [swipingUpGestureParentMethod].
 */
context(_: BytecodePatchContext)
internal fun ClassDef.getAllowSwipingUpGestureMethod() = firstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L")
}

internal val BytecodePatchContext.disableFastForwardLegacyMethodMatch by composingFirstMethod {
    returnType("Z")
    parameterTypes()
    opcodes(Opcode.MOVE_RESULT)
    // Intent start flag only used in the subscription activity
    literal { FeatureFlag.DOUBLE_SPEED_SEEK_GESTURE_LEGACY.id }
}

internal val BytecodePatchContext.disableFastForwardGestureMethodMatch by composingFirstMethod {
    definingClass("/NextGenWatchLayout;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes()
    opcodes(
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
    )
    custom { instructions.count() > 30 }
}

internal val BytecodePatchContext.onTouchEventHandlerMethodMatch by composingFirstMethod {
    name("onTouchEvent")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.PUBLIC)
    returnType("Z")
    parameterTypes("L")
    opcodes(
        Opcode.INVOKE_VIRTUAL, // nMethodReference
        Opcode.RETURN,
        Opcode.IGET_OBJECT,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN,
        Opcode.INT_TO_FLOAT,
        Opcode.INT_TO_FLOAT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL, // oMethodReference
    )
}

internal val BytecodePatchContext.tapToSeekMethodMatch by composingFirstMethod {
    name("onTouchEvent")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes("Landroid/view/MotionEvent;")
    instructions(
        Int.MAX_VALUE.toLong()(),
        allOf(Opcode.NEW_INSTANCE(), type("Landroid/graphics/Point;")),
        after(method { toString() == "Landroid/graphics/Point;-><init>(II)V" }),
        after(method { toString() == "Lj$/util/Optional;->of(Ljava/lang/Object;)Lj$/util/Optional;" }),
        after(Opcode.MOVE_RESULT_OBJECT()),
        after(allOf(Opcode.IPUT_OBJECT(), field { type == "Lj$/util/Optional;" })),
        afterAtMost(10, Opcode.INVOKE_VIRTUAL()),
    )
}

internal val BytecodePatchContext.slideToSeekMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/view/View;", "F")
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.GOTO_16,
    )
    literal { 67108864 }
}

internal val BytecodePatchContext.fullscreenLargeSeekbarFeatureFlagMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes()
    instructions(FeatureFlag.LARGE_FULLSCREEN_SEEKBAR())
}

internal val BytecodePatchContext.videoStreamingDataAllowSeekingMethod by getting {
    firstMethodDeclaratively {
        returnType("Z")
        parameterTypes()
        instructions(
            8L(),
            after(Opcode.IF_EQ()),
            after(1L()) // Another method in the same class almost matches this but uses 0 here.
        )
    }
} using {
    firstImmutableMethodDeclaratively {
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returnType("Ljava/lang/String;")
        name("toString")
        instructions("VideoStreamingData(itags="())
    }
}

internal val BytecodePatchContext.formatStreamModelMaxDvrDurationMethodMatch by getting {
    firstMethodComposite {
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returnType("D")
        parameterTypes()
        instructions(
            Opcode.IGET_OBJECT(),
            after(allOf(Opcode.IGET_WIDE(), field { type == "D" })),
            after(Opcode.RETURN_WIDE()),
        )
    }
} using {
    firstImmutableMethodDeclaratively {
        returnType("Ljava/lang/String;")
        instructions("FormatStream(itag="())
    }
}