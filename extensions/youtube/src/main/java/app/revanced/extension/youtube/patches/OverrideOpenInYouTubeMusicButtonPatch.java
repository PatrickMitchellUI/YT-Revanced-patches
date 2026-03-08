package app.revanced.extension.youtube.patches;


import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.Utils;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class OverrideOpenInYouTubeMusicButtonPatch {

    private static final String YOUTUBE_MUSIC_PACKAGE_NAME = "com.google.android.apps.youtube.music";

    private static final Boolean overrideButton = Settings.OVERRIDE_OPEN_IN_YOUTUBE_MUSIC_BUTTON.get();

    private static final String overridePackageName = getOverridePackageName();

    @SuppressWarnings("SameReturnValue")
    public static String getOverridePackageName() {
        return ""; // Value is replaced during patching.
    }

    /**
     * Replaces every {@link Intent#setPackage(String)} call of the app,
     * so the original call must always be made.
     */
    public static Intent overrideSetPackage(Intent intent, @Nullable String packageName) {
        if (overrideButton && YOUTUBE_MUSIC_PACKAGE_NAME.equals(packageName)) {
            packageName = getTargetPackageName();
        }

        return intent.setPackage(packageName);
    }

    /**
     * Replaces every {@link Intent#setData(Uri)} call of the app,
     * so the original call must always be made.
     */
    public static Intent overrideSetData(Intent intent, @Nullable Uri uri) {
        if (overrideButton && uri != null) {
            String uriString = uri.toString();
            if (uriString.contains(YOUTUBE_MUSIC_PACKAGE_NAME)
                    && ("market".equals(uri.getScheme()) || uriString.contains("play.google.com/store/apps"))) {
                return intent.setData(Uri.parse("https://music.youtube.com/"))
                        .setPackage(getTargetPackageName());
            }
        }

        return intent.setData(uri);
    }

    /**
     * @return The override package name if it is installed and enabled,
     *         otherwise null to let the system choose the app.
     */
    @Nullable
    private static String getTargetPackageName() {
        if (Utils.isNotEmpty(overridePackageName) && Utils.isPackageEnabled(overridePackageName)) {
            return overridePackageName;
        }

        return null;
    }
}
