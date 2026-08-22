package app.revanced.patches.shared.ad

import app.revanced.patcher.*
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.instructions
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.util.getting
import app.revanced.util.using
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.google.common.util.concurrent.Striped.custom

internal val BytecodePatchContext.interstitialsContainerMethodMatch by composingFirstMethod("overlay_controller_param") {
    returnType("V")
    instructions(
        ResourceType.ID("interstitials_container"),
    )
}

internal val BytecodePatchContext.lithoDialogBuilderMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("[B", "L")
    instructions(
        allOf(Opcode.INVOKE_VIRTUAL(), method("show")),
        ResourceType.STYLE("SlidingDialogAnimation"),
    )
}

internal val BytecodePatchContext.customDialogOnBackPressedMethod by getting {
    firstMethodDeclaratively {
        name("onBackPressed")
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returnType("V")
        parameterTypes()
    }
} using {
    firstImmutableMethodDeclaratively {
        returnType("V")
        parameterTypes("L")
        instructions(method { toString() == "Landroid/app/Dialog;->onBackPressed()V" })
        custom { immutableClassDef.superclass == "Landroid/app/Dialog;" }
    }
}

internal val BytecodePatchContext.fullscreenAdsCloseDialogMethod by gettingFirstMethodDeclaratively {
    definingClass(EXTENSION_CLASS_DESCRIPTOR)
    name("closeDialog")
    accessFlags(AccessFlags.PRIVATE, AccessFlags.STATIC)
}
