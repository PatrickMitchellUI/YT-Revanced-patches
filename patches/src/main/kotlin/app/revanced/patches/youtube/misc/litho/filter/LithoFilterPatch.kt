@file:Suppress("SpellCheckingInspection")

package app.revanced.patches.youtube.misc.litho.filter

import app.revanced.patches.shared.misc.litho.filter.lithoFilterPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playservice.*
import app.revanced.util.insertLiteralOverride

val lithoFilterPatch = lithoFilterPatch(
    insertLegacyProtobufHook = {},
    getExtractIdentifierFromBuffer = { true },
    executeBlock = {
        // region A/B test of new Litho native code.

        // Turn off a feature flag that enables native code of protobuf parsing (Upb protobuf).
        // Flag was removed in 21.36.
        if (!is_21_36_or_greater) {
            lithoConverterBufferUpbFeatureFlagMethodMatch.let {
                // 20.22 the flag is still enabled in one location, but what it does is not known.
                // Disable it anyway.
                it.method.insertLiteralOverride(
                    it[0],
                    false,
                )
            }
        }
        // endregion
    }
) {
    dependsOn(sharedExtensionPatch, versionCheckPatch)
}
