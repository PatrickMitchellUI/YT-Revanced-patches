package app.revanced.extension.youtube.patches;

import app.revanced.extension.shared.Utils;

public class VersionCheckPatch {
    private static boolean isVersionOrGreater(String version) {
        return Utils.getAppVersionName().compareTo(version) >= 0;
    }

    /**
     * The supported targets are 20.40.45 and 21.36.47, so this check tells them apart.
     */
    public static final boolean IS_21_36_OR_GREATER = isVersionOrGreater("21.36.00");
}
