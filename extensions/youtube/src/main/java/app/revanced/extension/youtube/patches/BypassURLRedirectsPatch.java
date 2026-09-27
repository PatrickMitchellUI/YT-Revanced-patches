package app.revanced.extension.youtube.patches;

import android.net.Uri;

import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.shared.Logger;

@SuppressWarnings("unused")
public class BypassURLRedirectsPatch {
    private static final String YOUTUBE_REDIRECT_PATH = "/redirect";

    /**
     * Convert the YouTube redirect URI string to the redirect query URI.
     *
     * @param uri The YouTube redirect URI string.
     * @return The redirect query URI.
     */
    public static Uri parseRedirectUri(String uri) {
        final var parsed = Uri.parse(uri);

        if (Settings.BYPASS_URL_REDIRECTS.get() && YOUTUBE_REDIRECT_PATH.equals(parsed.getPath())) {
            String redirect = parsed.getQueryParameter("q");
            if (redirect == null) {
                return parsed;
            }

            var query = Uri.parse(Uri.decode(redirect));

            Logger.printDebug(() -> "Bypassing YouTube redirect URI: " + query);

            return query;
        }

        return parsed;
    }
}
