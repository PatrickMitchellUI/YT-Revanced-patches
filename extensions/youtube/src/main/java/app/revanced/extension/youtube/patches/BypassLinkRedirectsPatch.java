package app.revanced.extension.youtube.patches;

import android.net.Uri;
import app.revanced.extension.shared.Logger;
import app.revanced.extension.youtube.settings.Settings;

import java.util.Objects;

@SuppressWarnings("unused")
public class BypassLinkRedirectsPatch {

    /**
     * Convert the YouTube redirect URI to the redirect query URI.
     *
     * @param uri The YouTube redirect URI.
     * @return The redirect query URI.
     */
    public static Uri parseRedirectUri(Uri uri) {
        if (Settings.BYPASS_LINK_REDIRECTS.get() && Objects.equals(uri.getPath(), "/redirect")) {
            var query = Uri.parse(Uri.decode(uri.getQueryParameter("q")));
            Logger.printDebug(() -> "Bypassing YouTube redirect URI: " + query);
            return query;
        }
        return uri;
    }
}
