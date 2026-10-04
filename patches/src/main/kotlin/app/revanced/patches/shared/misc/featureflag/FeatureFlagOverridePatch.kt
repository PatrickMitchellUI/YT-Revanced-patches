package app.revanced.patches.shared.misc.featureflag

import app.revanced.patcher.classDef
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.debugging.experimentalBooleanFeatureFlagMethodMatch
import app.revanced.patches.shared.misc.debugging.experimentalFeatureFlagUtilMethod
import app.revanced.patches.shared.misc.debugging.featureFlagAccessorInvoke
import app.revanced.patches.shared.misc.extension.sharedExtensionPatch
import app.revanced.util.cloneMutable
import app.revanced.util.cloneMutableAndPreserveParameters

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/shared/patches/FeatureFlagOverridePatch;"

private lateinit var addBooleanFeatureFlagOverride: (flag: Long, smali: String) -> Unit

/**
 * Overrides a boolean feature flag everywhere the app reads it,
 * including call sites where the flag is read inline.
 *
 * @param extensionMethodDescriptor A static `(Z)Z` method that receives the original value
 * and returns the value to use.
 */
fun overrideBooleanFeatureFlag(flag: Long, extensionMethodDescriptor: String) =
    addBooleanFeatureFlagOverride(
        flag,
        """
            invoke-static { p0 }, $extensionMethodDescriptor
            move-result p0
        """,
    )

/**
 * Overrides a boolean feature flag everywhere the app reads it,
 * including call sites where the flag is read inline.
 */
fun overrideBooleanFeatureFlag(flag: Long, value: Boolean) =
    addBooleanFeatureFlagOverride(flag, "const/4 p0, ${if (value) "0x1" else "0x0"}")

/**
 * @see overrideBooleanFeatureFlag
 */
fun overrideBooleanFeatureFlag(flag: FeatureFlag, extensionMethodDescriptor: String) =
    overrideBooleanFeatureFlag(flag.id, extensionMethodDescriptor)

/**
 * @see overrideBooleanFeatureFlag
 */
fun overrideBooleanFeatureFlag(flag: FeatureFlag, value: Boolean) =
    overrideBooleanFeatureFlag(flag.id, value)

val featureFlagOverridePatch = bytecodePatch(
    description = "Hooks the boolean feature flag accessor to allow overriding feature flags.",
) {
    dependsOn(sharedExtensionPatch())

    apply {
        // Preserve the parameters to have free registers for the flag comparison.
        val overrideMethod = overrideBooleanFeatureFlagMethod.cloneMutableAndPreserveParameters()

        addBooleanFeatureFlagOverride = { flag, smali ->
            overrideMethod.addInstructionsWithLabels(
                0,
                """
                    const-wide v0, ${flag}L
                    cmp-long v0, p1, v0
                    if-nez v0, :other_flag
                    $smali
                    :other_flag
                    nop
                """,
            )
        }

        experimentalFeatureFlagUtilMethod.immutableClassDef.experimentalBooleanFeatureFlagMethodMatch.method.apply {
            // Not enough registers in the method. Clone the method and use the
            // original method as an intermediate to call extension code.
            val helperMethod = cloneMutable(name = "patch_getBooleanFeatureFlagOverride")
            classDef.methods.add(helperMethod)

            addInstructions(
                0,
                """
                    $featureFlagAccessorInvoke { p0, p1, p2, p3 }, $helperMethod
                    move-result p0
                    invoke-static { p0, p1, p2 }, $EXTENSION_CLASS_DESCRIPTOR->overrideBooleanFeatureFlag(ZJ)Z
                    move-result p0
                    return p0
                """,
            )
        }
    }
}
