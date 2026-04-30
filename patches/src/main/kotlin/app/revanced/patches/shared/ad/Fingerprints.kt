package app.revanced.patches.shared.ad

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

internal val BytecodePatchContext.lithoDialogBuilderMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("[B", "L")
    instructions(
        allOf(Opcode.INVOKE_VIRTUAL(), method("show")),
        ResourceType.STYLE("SlidingDialogAnimation")
    )
}

internal val BytecodePatchContext.customDialogOnBackPressedParentMethodMatch by gettingFirstMethodDeclaratively {
    returnType("V")
    parameterTypes("L")
    custom { immutableClassDef.superclass == "Landroid/app/Dialog;" }
    instructions(
        method { toString() == "Landroid/app/Dialog;->onBackPressed()V" },
    )
}

context(_: BytecodePatchContext)
internal fun ClassDef.customDialogOnBackPressedMethod() = firstMethodDeclaratively {
    name("onBackPressed")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes()
}

internal val BytecodePatchContext.fullscreenAdsCloseDialogMethod by gettingFirstMethodDeclaratively {
    definingClass(EXTENSION_CLASS_DESCRIPTOR)
    name("closeDialog")
    accessFlags(AccessFlags.PRIVATE, AccessFlags.STATIC)
}
