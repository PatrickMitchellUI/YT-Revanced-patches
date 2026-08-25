package app.revanced.extension.shared.patches;

import android.net.Uri;
import android.text.TextUtils;
import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.privacy.LinkSanitizer;
import app.revanced.extension.shared.settings.BaseSettings;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YouTube and YouTube Music.
 */
@SuppressWarnings("unused")
public final class SanitizeSharingLinksPatch {

    private static final LinkSanitizer sanitizer = new LinkSanitizer(
            "si",
            "is", // New (localized?) tracking parameter.
            "feature" // Old tracking parameter name, and may be obsolete.
    );

    /**
     * Injection point.
     */
    public static String sanitize(String originalURL) {
        Matcher urlMatcher = Pattern.compile("https?://\\S+(?<![.!?,-])").matcher(originalURL);
        String url;
        if (urlMatcher.find()) {
            url = urlMatcher.group();
        } else {
            return originalURL;
        }

        String host = Uri.parse(url).getHost();
        if (host == null || (!host.equals("youtube.com") && !host.endsWith(".youtube.com") && !host.equals("youtu.be"))) {
            return originalURL;
        }

        boolean urlChangesApplied = false;

        if (BaseSettings.SANITIZE_SHARING_LINKS.get()) {
            url = sanitizer.returnSanitizedURLFromURI(url);
            urlChangesApplied = true;
        }

        if (BaseSettings.REPLACE_MUSIC_LINKS_WITH_YOUTUBE.get()) {
            url = url.replace("music.youtube.com", "youtube.com");
            urlChangesApplied = true;
        }

        if (BaseSettings.REPLACE_LINKS_WITH_SHORTENER.get()) {
            url = replaceWithShortenedUrl(url);
            urlChangesApplied = true;
        }

        // If no sanitize/replace option is enabled, return the original text unchanged so that
        // opening the system share sheet (and other passthrough cases) is not broken.
        return urlChangesApplied ? url : originalURL;
    }

    private static final String timeQueryTagName = "t";

    private static String replaceWithShortenedUrl(String url) {
        try {
            // Invite and post links are not videos and must not be turned into youtu.be links.
            if (url.contains("/sharing/invite/") || url.contains("/post/")) {
                return url;
            }
            Uri uri = Uri.parse(url);
            List<String> segments = uri.getPathSegments();
            int segmentsSize = segments.size();
            if (segmentsSize == 0) {
                return url;
            }
            String videoId = "";
            int currentGetVideoIDAttempts = 0;
            int maxGetVideoIDAttempts = 3;
            while (currentGetVideoIDAttempts <= maxGetVideoIDAttempts) {
                videoId = switch (currentGetVideoIDAttempts) {
                    case 0 -> uri.getQueryParameter("v");
                    case 1 -> uri.getQueryParameter("w");
                    case 2 -> uri.getQueryParameter("s");
                    default -> segments.size() > 1 ? segments.get(1) : "";
                };
                if (!TextUtils.isEmpty(videoId)) {
                    break;
                }
                currentGetVideoIDAttempts++;
            }
            Uri.Builder finalURL =
                    new Uri.Builder()
                    .scheme("https")
                    .authority("youtu.be");
            if (currentGetVideoIDAttempts < maxGetVideoIDAttempts + 1) {
                finalURL.appendPath(videoId);
                String timeQueryContent = uri.getQueryParameter(timeQueryTagName);
                if (!TextUtils.isEmpty(timeQueryContent)) {
                    finalURL.appendQueryParameter(timeQueryTagName, timeQueryContent);
                }
                return finalURL.build().toString();
            }
            return url;
        } catch (Exception ex) {
            Logger.printException(() -> "replaceWithShortenedUrl failure: " + url, ex);
            return url;
        }
    }
}
