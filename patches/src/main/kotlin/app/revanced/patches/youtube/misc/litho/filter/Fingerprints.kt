package app.revanced.patches.youtube.misc.litho.filter

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.featureflag.FeatureFlag

internal val BytecodePatchContext.lithoConverterBufferUpbFeatureFlagMethodMatch by composingFirstMethod {
    returnType("L")
    instructions(FeatureFlag.NATIVE_PROTOBUF_PARSING())
}
