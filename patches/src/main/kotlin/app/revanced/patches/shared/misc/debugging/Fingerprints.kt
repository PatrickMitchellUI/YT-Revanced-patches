package app.revanced.patches.shared.misc.debugging

import app.revanced.patcher.ClassDefComposing
import app.revanced.patcher.gettingFirstImmutableMethodDeclaratively
import app.revanced.patcher.firstMethodDeclaratively
import app.revanced.patcher.accessFlags
import app.revanced.patcher.custom
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method

/**
 * The feature flag accessors are static methods with the config object as first parameter,
 * and instance methods of the config class since 21.x.
 */
internal fun Method.isFeatureFlagAccessor(valueTypes: List<String?>): Boolean {
    if (!AccessFlags.PUBLIC.isSet(accessFlags)) return false

    val types = parameterTypes.toList()
        .let { if (AccessFlags.STATIC.isSet(accessFlags)) it.drop(1) else it }

    if (types.firstOrNull() != "J") return false

    return valueTypes.any { valueType ->
        if (valueType == null) types.size == 1 else types.size == 2 && types[1] == valueType
    }
}

internal val Method.featureFlagAccessorInvoke
    get() = if (AccessFlags.STATIC.isSet(accessFlags)) "invoke-static" else "invoke-virtual"

internal val BytecodePatchContext.experimentalFeatureFlagUtilMethod by gettingFirstImmutableMethodDeclaratively(
    "Unable to parse proto typed experiment flag: "
) {
    returnType("L")
    custom { isFeatureFlagAccessor(listOf(null, "[B")) }
}

internal val ClassDef.experimentalBooleanFeatureFlagMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("Z")
    custom { isFeatureFlagAccessor(listOf("Z")) }
}

context(_: BytecodePatchContext)
internal fun ClassDef.getExperimentalDoubleFeatureFlagMethod() = firstMethodDeclaratively {
    returnType("D")
    custom { isFeatureFlagAccessor(listOf("D")) }
}

context(_: BytecodePatchContext)
internal fun ClassDef.getExperimentalLongFeatureFlagMethod() = firstMethodDeclaratively {
    returnType("J")
    custom { isFeatureFlagAccessor(listOf("J")) }
}

context(_: BytecodePatchContext)
internal fun ClassDef.getExperimentalStringFeatureFlagMethod() = firstMethodDeclaratively {
    returnType("Ljava/lang/String;")
    custom { isFeatureFlagAccessor(listOf("Ljava/lang/String;")) }
}
