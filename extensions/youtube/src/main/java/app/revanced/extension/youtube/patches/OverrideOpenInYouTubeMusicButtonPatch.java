package app.revanced.extension.youtube.patches;


import android.content.Intent;
import android.content.pm.PackageManager;

import app.revanced.extension.shared.Utils;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class OverrideOpenInYouTubeMusicButtonPatch {

    private static final String YOUTUBE_MUSIC_PACKAGE_NAME = "com.google.android.apps.youtube.music";
    private static final String REVANCED_EXTRA_FLAG = "ReVanced";

    public static Intent overrideSetPackage(Intent intent, String packageName) {
        if (intent == null) return null;

        if (!Settings.OVERRIDE_OPEN_IN_YOUTUBE_MUSIC_BUTTON.get()) {
            return intent.setPackage(packageName);
        }

        if (intent.getBooleanExtra(REVANCED_EXTRA_FLAG, false)) {
            return intent;
        }

        if (YOUTUBE_MUSIC_PACKAGE_NAME.equals(packageName)) {
            String target = Settings.OVERRIDE_OPEN_IN_YOUTUBE_MUSIC_BUTTON_TARGET_PACKAGE_NAME.get().trim();

            if (Utils.isNotEmpty(target) && isAppInstalled(target)) {
                PackageManager pm = Utils.getContext().getPackageManager();
                Intent launchIntent = pm.getLaunchIntentForPackage(target);

                if (launchIntent != null) {
                    intent.setAction(launchIntent.getAction());
                    intent.setComponent(launchIntent.getComponent());
                    intent.setPackage(target);
                    intent.putExtra(REVANCED_EXTRA_FLAG, true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    return intent;
                }
            }
            return intent.setPackage(null);
        }

        return intent.setPackage(packageName);
    }


    private static boolean isAppInstalled(String packageName) {
        try {
            return Utils.getContext().getPackageManager().getLaunchIntentForPackage(packageName) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
