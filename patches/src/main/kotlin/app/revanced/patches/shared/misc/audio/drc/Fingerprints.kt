package app.revanced.patches.shared.misc.audio.drc

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.util.getting
import app.revanced.util.using
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * com.google.android.libraries.youtube.innertube.model.media.FormatStreamModel
 * Class names have been obfuscated in the latest YouTube or YouTube Music.
 */
internal val BytecodePatchContext.formatStreamModelConstructorMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    returnType("V")
    instructions(45374643L())
}

/**
 * Matched within the class of [formatStreamModelConstructorMethod].
 */
internal val BytecodePatchContext.compressionRatioMethodMatch by getting {
    firstMethodComposite {
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returnType("Lj$/util/Optional;")
        parameterTypes()

        var thisClass = ""
        custom {
            thisClass = definingClass
            true
        }

        instructions(
            allOf(Opcode.IGET_OBJECT(), field { definingClass == thisClass }),
            Opcode.IF_EQZ(),
            afterAtMost(3, allOf(Opcode.IGET(), field { type == "F" })),
            Opcode.NEG_FLOAT(),
            allOf(Opcode.INVOKE_STATIC(), method { toString() == "Ljava/lang/Math;->min(FF)F" }),
        )
    }
} using {
    firstImmutableMethodDeclaratively {
        accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
        returnType("V")
        instructions(45374643L())
    }
}

internal val BytecodePatchContext.volumeNormalizationConfigMethodMatch by composingFirstMethod {
    returnType("V")
    parameterTypes("F")
    instructions(45425391L())
}
