package app.revanced.extension.youtube.patches;


import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.net.Uri;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class OverrideOpenInYouTubeMusicButtonPatch {

    private static final String YOUTUBE_MUSIC_PACKAGE_NAME = "com.google.android.apps.youtube.music";
    private static final String REVANCED_MUSIC_PACKAGE_NAME = "app.revanced.android.apps.youtube.music";
    private static final String REVANCED_EXTRA_FLAG = "ReVanced";

    private static Intent putExtraFlag(Intent intent) {
        String target = Settings.OVERRIDE_OPEN_IN_YOUTUBE_MUSIC_BUTTON_TARGET_PACKAGE_NAME.get().trim();
        if (target.isEmpty()) {
            target = REVANCED_MUSIC_PACKAGE_NAME;
        }

        PackageManager pm = Utils.getContext().getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(target);

        if (launchIntent == null) {
            intent.setData(Uri.parse("https://music.youtube.com/"));
            intent.setPackage(null);
            intent.setComponent(null);
            return intent;
        }

        Uri data = intent.getData();
        String uriString = data != null ? data.toString() : "";
        boolean isPlayStoreLink = uriString.contains("play.google.com") || uriString.startsWith("market://");

        intent.setAction(launchIntent.getAction());
        intent.setComponent(launchIntent.getComponent());
        intent.setPackage(target);
        intent.putExtra(REVANCED_EXTRA_FLAG, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (isPlayStoreLink || data == null) {
            intent.setData(null);
        } else {
            intent.setData(data);
        }

        return intent;
    }

    public static Intent overrideSetPackage(Intent intent, String packageName) {
        if (intent == null) return null;

        if (!Settings.OVERRIDE_OPEN_IN_YOUTUBE_MUSIC_BUTTON.get()) {
            return intent.setPackage(packageName);
        }

        if (intent.getBooleanExtra(REVANCED_EXTRA_FLAG, false)) {
            return intent;
        }

        if (YOUTUBE_MUSIC_PACKAGE_NAME.equals(packageName)) {
            return putExtraFlag(intent);
        }

        return intent.setPackage(packageName);
    }

    public static Intent overrideSetData(Intent intent, Uri uri) {
        if (intent == null) return null;
        if (uri == null) return intent.setData(null);

        if (!Settings.OVERRIDE_OPEN_IN_YOUTUBE_MUSIC_BUTTON.get()) {
            return intent.setData(uri);
        }

        if (intent.getBooleanExtra(REVANCED_EXTRA_FLAG, false)) {
            return intent;
        }

        String uriString = uri.toString();
        if (uriString.contains(YOUTUBE_MUSIC_PACKAGE_NAME) || uriString.contains("music.youtube.com")) {
            intent.setData(uri);
            return putExtraFlag(intent);
        }

        return intent.setData(uri);
    }

    public static Intent overrideSetComponent(Intent intent, ComponentName component) {
        if (intent == null) return null;

        if (!Settings.OVERRIDE_OPEN_IN_YOUTUBE_MUSIC_BUTTON.get()) {
            return intent.setComponent(component);
        }

        if (intent.getBooleanExtra(REVANCED_EXTRA_FLAG, false)) {
            return intent;
        }

        if (component != null && YOUTUBE_MUSIC_PACKAGE_NAME.equals(component.getPackageName())) {
            intent.setComponent(component);
            return putExtraFlag(intent);
        }

        return intent.setComponent(component);
    }
}
