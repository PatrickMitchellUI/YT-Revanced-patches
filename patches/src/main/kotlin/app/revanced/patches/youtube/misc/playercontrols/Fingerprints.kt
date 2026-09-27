package app.revanced.patches.youtube.misc.playercontrols

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

internal val BytecodePatchContext.playerControlsVisibilityEntityModelMethodMatch by composingFirstMethod {
    name("getPlayerControlsVisibility")
    accessFlags(AccessFlags.PUBLIC)
    returnType("L")
    parameterTypes()
    opcodes(
        Opcode.IGET,
        Opcode.INVOKE_STATIC,
    )
}

internal val BytecodePatchContext.youtubeControlsOverlayMethod by gettingFirstImmutableMethodDeclaratively {
    returnType("V")
    parameterTypes()
    instructions(
        method("setFocusableInTouchMode"),
        ResourceType.ID("inset_overlay_view_layout"),
        ResourceType.ID("scrim_overlay"),
    )
}

internal val ClassDef.motionEventMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("V")
    parameterTypes("Landroid/view/MotionEvent;")
    instructions(method("setTranslationY"))
}

internal val ClassDef.motionEventTranslationHelperMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("V")
    parameterTypes("Landroid/view/MotionEvent;")
    instructions(
        Opcode.INVOKE_INTERFACE(),
        afterAtMost(5, Opcode.INVOKE_VIRTUAL()), // Calls setTranslationY.
        after(Opcode.RETURN_VOID()),
    )
}

internal val BytecodePatchContext.playerControlsExtensionHookListenersExistMethod by gettingFirstMethodDeclaratively {
    name("fullscreenButtonVisibilityCallbacksExist")
    definingClass(EXTENSION_CLASS_DESCRIPTOR)
    accessFlags(AccessFlags.PRIVATE, AccessFlags.STATIC)
    returnType("Z")
    parameterTypes()
}

internal val BytecodePatchContext.playerControlsExtensionHookMethod by gettingFirstMethodDeclaratively {
    name("fullscreenButtonVisibilityChanged")
    definingClass(EXTENSION_CLASS_DESCRIPTOR)
    accessFlags(AccessFlags.PRIVATE, AccessFlags.STATIC)
    returnType("V")
    parameterTypes("Z")
}

internal val BytecodePatchContext.playerTopControlsInflateMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes()
    instructions(
        ResourceType.ID("controls_layout_stub"),
        method { name == "inflate" && definingClass == "Landroid/view/ViewStub;" },
        after(Opcode.MOVE_RESULT_OBJECT()),
    )
}

internal val BytecodePatchContext.playerBottomControlsInflateMethodMatch by composingFirstMethod {
    returnType("Ljava/lang/Object;")
    parameterTypes()
    instructions(
        ResourceType.ID("bottom_ui_container_stub"),
        method { name == "inflate" && definingClass == "Landroid/view/ViewStub;" },
        after(Opcode.MOVE_RESULT_OBJECT()),
    )
}

internal val BytecodePatchContext.overlayViewInflateMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/view/View;")
    instructions(
        ResourceType.ID("heatseeker_viewstub"),
        ResourceType.ID("fullscreen_button"),
        Opcode.CHECK_CAST(),
    )
}

/**
 * 21.36 binds the visible fullscreen button here.
 * The button of [overlayViewInflateMethodMatch] is in a container that is not laid out.
 */
internal val BytecodePatchContext.overlayFullscreenButtonMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/view/View;")
    instructions(
        ResourceType.ID("fullscreen_button"),
        after(method { name == "findViewById" }),
        after(Opcode.MOVE_RESULT_OBJECT()),
        after(
            allOf(
                Opcode.INVOKE_INTERFACE(),
                method { parameterTypes.size == 1 && parameterTypes[0] == "Landroid/view/View;" },
            ),
        ),
    )
}

/**
 * Resolves to the class found in [playerTopControlsInflateMethodMatch].
 */
context(_: BytecodePatchContext)
internal fun ClassDef.getControlsOverlayVisibilityMethod() = firstMethodDeclaratively {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Z", "Z")
}

/**
 * 21.36 creates the background of the fullscreen button with insets for portrait and fullscreen.
 */
internal val BytecodePatchContext.fullscreenButtonBackgroundMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Landroid/graphics/drawable/Drawable;")
    parameterTypes("Z")
    instructions(
        ResourceType.DRAWABLE("player_button_circle_background"),
        afterAtMost(2, method { name == "getDrawable" }),
        after(Opcode.MOVE_RESULT_OBJECT()),
        afterAtMost(2, allOf(Opcode.IGET(), field { definingClass == "Landroid/graphics/Rect;" })),
    )
}
