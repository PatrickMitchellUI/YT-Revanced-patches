package app.revanced.patches.shared.misc.featureflag

import app.revanced.patcher.definingClass
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.name
import app.revanced.patcher.patch.BytecodePatchContext

internal val BytecodePatchContext.overrideBooleanFeatureFlagMethod by gettingFirstMethodDeclaratively {
    name("overrideBooleanFeatureFlag")
    definingClass(EXTENSION_CLASS_DESCRIPTOR)
}
