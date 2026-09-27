package app.revanced.extension.shared.patches;

@SuppressWarnings("unused")
public final class FeatureFlagOverridePatch {

    /**
     * Injection point.
     * <p>
     * The flag overrides are added during patching.
     */
    public static boolean overrideBooleanFeatureFlag(boolean value, long flag) {
        return value;
    }
}
