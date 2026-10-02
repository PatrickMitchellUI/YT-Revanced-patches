package app.revanced.extension.youtube.patches;

import android.content.Intent;
import android.net.Uri;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class OpenLinksExternallyPatch {
    /**
     * Return the intent to open links with. If empty, the link will be opened with the default browser.
     *
     * @param originalIntent The original intent to open links with.
     * @return The intent to open links with. Empty means the link will be opened with the default browser.
     */
    public static String getIntent(String originalIntent) {
        if (Settings.EXTERNAL_BROWSER.get()) return "";

        return originalIntent;
    }

    /**
     * Injection point. Called before a link is opened with a plain view intent.
     * <p>
     * YouTube web pages the app can not show itself (such as "About these results") are resolved
     * to a YouTube app, which only passes them on to the browser. The html type is only handled by browsers.
     */
    public static void openInBrowser(Intent intent) {
        try {
            if (!Settings.EXTERNAL_BROWSER.get() || !Intent.ACTION_VIEW.equals(intent.getAction())) return;

            Uri uri = intent.getData();
            if (uri == null || intent.getType() != null) return;

            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!("https".equals(scheme) || "http".equals(scheme)) || host == null) return;
            if (!host.equals("youtube.com") && !host.endsWith(".youtube.com")) return;

            Logger.printDebug(() -> "Opening in the browser: " + uri);
            intent.setDataAndType(uri, "text/html");
        } catch (Exception ex) {
            Logger.printException(() -> "openInBrowser failure", ex);
        }
    }
}
