package app.revanced.patches.shared.misc.spannablestring

import app.revanced.patcher.accessFlags
import app.revanced.patcher.allOf
import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.definingClass
import app.revanced.patcher.field
import app.revanced.patcher.gettingFirstImmutableMethodDeclaratively
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.method
import app.revanced.patcher.name
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import app.revanced.patcher.type
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.buildSpannableStringMethodMatch by composingFirstMethod {
    returnType("Ljava/lang/CharSequence;")

    val methodParameters = listOf("Landroid/text/SpannableString;", "Ljava/lang/Object;", "I", "Z", "I")

    instructions(
        method { toString() == "Landroid/text/SpannableString;->valueOf(Ljava/lang/CharSequence;)Landroid/text/SpannableString;" },
        method {
            returnType == "V" && parameterTypes.size == methodParameters.size &&
                parameterTypes.zip(methodParameters).all { (a, b) -> a.startsWith(b) }
        },
        "Failed to set PB Style Run Extension in TextComponentSpec."(String::startsWith),
    )
    instructions(method("setSpan"))
}

internal val BytecodePatchContext.customCharacterStyleMethod by gettingFirstImmutableMethodDeclaratively {
    returnType("Landroid/graphics/Path;")
    parameterTypes("Landroid/text/Layout;")
}

internal val BytecodePatchContext.spannableStringMethodMatch by composingFirstMethod {
    definingClass(EXTENSION_CLASS_DESCRIPTOR)
    accessFlags(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR)
    instructions(
        allOf(
            Opcode.SPUT_OBJECT(),
            field { name == "filters" },
        ),
    )
}

internal val BytecodePatchContext.getSpanTypeMethodMatch by composingFirstMethod {
    definingClass(EXTENSION_CLASS_DESCRIPTOR)
    returnType { this != "Ljava/lang/String;" }
    name("getSpanType")
    instructions(
        type("Landroid/text/style/CharacterStyle;"),
    )
}
