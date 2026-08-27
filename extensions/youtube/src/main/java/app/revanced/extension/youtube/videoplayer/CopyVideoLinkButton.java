package app.revanced.extension.youtube.videoplayer;

import android.os.Build;
import android.view.View;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.youtube.patches.VideoInformation;
import app.revanced.extension.youtube.settings.Settings;

import static app.revanced.extension.shared.StringRef.str;

@SuppressWarnings("unused")
public class CopyVideoLinkButton {
    @Nullable
    private static PlayerControlButton instance;

    /**
     * Injection point.
     */
    public static void initializeButton(View controlsView) {
        try {
            // Tap copies in the default mode and long press copies in the opposite mode.
            // The default mode is set by the 'Copy timestamp' setting.
            instance = new PlayerControlButton(
                    controlsView,
                    "revanced_copy_video_link_button",
                    null,
                    Settings.COPY_VIDEO_LINK_BUTTON::get,
                    view -> copyLink(Settings.COPY_VIDEO_LINK_WITH_TIMESTAMP_BUTTON.get()),
                    view -> {
                        copyLink(!Settings.COPY_VIDEO_LINK_WITH_TIMESTAMP_BUTTON.get());
                        return true;
                    }
            );
        } catch (Exception ex) {
            Logger.printException(() -> "initializeButton failure", ex);
        }
    }

    /**`
     * injection point.
     */
    public static void setVisibilityNegatedImmediate() {
        if (instance != null) instance.setVisibilityNegatedImmediate();
    }

    /**
     * injection point.
     */
    public static void setVisibilityImmediate(boolean visible) {
        if (instance != null) instance.setVisibilityImmediate(visible);
    }

    /**
     * injection point.
     */
    public static void setVisibility(boolean visible, boolean animated) {
        if (instance != null) instance.setVisibility(visible, animated);
    }

    public static void copyLink(boolean withTimestamp) {
        try {
            StringBuilder builder = new StringBuilder("https://youtu.be/");
            builder.append(VideoInformation.getVideoId());
            final long currentVideoTimeInSeconds = appendCurrentVideoTimeInSeconds(withTimestamp, builder);

            Utils.setClipboard(builder.toString());
            // Do not show a toast if using Android 13+ as it shows its own toast.
            // But if the user copied with a timestamp then show a toast.
            // Unfortunately this will show 2 toasts on Android 13+, but no way around this.
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2
                    || (withTimestamp && currentVideoTimeInSeconds > 0)) {
                Utils.showToastShort(withTimestamp && currentVideoTimeInSeconds > 0
                        ? str("revanced_share_copy_link_button_with_timestamp_success")
                        : str("revanced_share_copy_link_button_success"));
            }
        } catch (Exception e) {
            Logger.printException(() -> "Failed to generate video link", e);
        }
    }

    private static long appendCurrentVideoTimeInSeconds(boolean withTimestamp, StringBuilder builder) {
        final long currentVideoTimeInSeconds = VideoInformation.getVideoTime() / 1000;
        if (withTimestamp && currentVideoTimeInSeconds > 0) {
            final long hour = currentVideoTimeInSeconds / (60 * 60);
            final long minute = (currentVideoTimeInSeconds / 60) % 60;
            final long second = currentVideoTimeInSeconds % 60;
            builder.append("?t=");
            if (hour > 0) {
                builder.append(hour).append("h");
            }
            if (minute > 0) {
                builder.append(minute).append("m");
            }
            if (second > 0) {
                builder.append(second).append("s");
            }
        }
        return currentVideoTimeInSeconds;
    }
}