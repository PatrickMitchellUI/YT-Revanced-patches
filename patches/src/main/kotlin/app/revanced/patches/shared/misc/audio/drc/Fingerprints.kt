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

// YouTube 21.18 and lower, Music 9.18 and lower.
internal val BytecodePatchContext.volumeNormalizationConfigLegacyMethodMatch by composingFirstMethod {
    instructions(45425391L())
}

// YouTube 21.19+, Music 9.19+
internal val BytecodePatchContext.volumeNormalizationConfigMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes()
    instructions(45784793L())
}

// YouTube 21.19+, Music 9.20+. Removed again in later versions (e.g. YouTube 21.25), so matched optionally.
internal val BytecodePatchContext.optionalVolumeNormalizationConfigMethodMatch by composingFirstMethod {
    instructions(
        45785775L(),
        afterAtMost(2, Opcode.MOVE_RESULT()),
        method { toString() == "Lj$/util/Optional;->isEmpty()Z" },
        after(Opcode.MOVE_RESULT()),
        afterAtMost(5, 45783491L()),
    )
}
