package app.revanced.extension.youtube.patches;

import static app.revanced.extension.shared.StringRef.str;
import static app.revanced.extension.youtube.patches.MiniplayerPatch.MiniplayerType.*;

import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class MiniplayerPatch {

    public enum MiniplayerType {
        /**
         * Disabled. When swiped down the miniplayer is immediately closed.
         */
        DISABLED,
        /**
         * Unmodified type, and same as un-patched.
         */
        DEFAULT
    }

    private static final int MINIPLAYER_SIZE;

    static {
        // YT appears to use the device screen dip width, plus an unknown fixed horizontal padding size.
        DisplayMetrics displayMetrics = Utils.getContext().getResources().getDisplayMetrics();
        final int deviceDipWidth = (int) (displayMetrics.widthPixels / displayMetrics.density);

        // YT seems to use a minimum height to calculate the minimum miniplayer width based on the video.
        // 170 seems to be the smallest that can be used and using less makes no difference.
        final int WIDTH_DIP_MIN = 170; // Seems to be the smallest that works.
        final int HORIZONTAL_PADDING_DIP = 15; // Estimated padding.
        // Round down to the nearest 5 pixels, to keep any error toasts easier to read.
        final int estimatedWidthDipMax = 5 * ((deviceDipWidth - HORIZONTAL_PADDING_DIP) / 5);
        // On some ultra-low-end devices the pixel width and density are the same number,
        // which causes the estimate to always give a value of 1.
        // Fix this by using a fixed size twice the minimum width.
        final int WIDTH_DIP_MAX = estimatedWidthDipMax <= WIDTH_DIP_MIN
                ? 2 * WIDTH_DIP_MIN
                : estimatedWidthDipMax;
        Logger.printDebug(() -> "Screen dip width: " + deviceDipWidth + " maxWidth: " + WIDTH_DIP_MAX);

        int dipWidth = Settings.MINIPLAYER_WIDTH_DIP.get();

        if (dipWidth < WIDTH_DIP_MIN || dipWidth > WIDTH_DIP_MAX) {
            Utils.showToastLong(str("revanced_miniplayer_width_dip_invalid_toast",
                    WIDTH_DIP_MIN, WIDTH_DIP_MAX));

            // Instead of resetting, clamp the size at the bounds.
            dipWidth = Math.max(WIDTH_DIP_MIN, Math.min(dipWidth, WIDTH_DIP_MAX));
            Settings.MINIPLAYER_WIDTH_DIP.save(dipWidth);
        }

        MINIPLAYER_SIZE = dipWidth;
    }

    private static final MiniplayerType CURRENT_TYPE = Settings.MINIPLAYER_TYPE.get();

    private static final boolean HIDE_OVERLAY_BUTTONS_ENABLED =
            CURRENT_TYPE == DEFAULT && Settings.MINIPLAYER_HIDE_OVERLAY_BUTTONS.get();

    private static final boolean HIDE_SUBTEXT_ENABLED =
            CURRENT_TYPE == DEFAULT && Settings.MINIPLAYER_HIDE_SUBTEXT.get();

    public static final class MiniplayerEnabledAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return Settings.MINIPLAYER_TYPE.get() == DEFAULT;
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(Settings.MINIPLAYER_TYPE);
        }
    }

    /**
     * Injection point.
     * <p>
     * Enables a handler that immediately closes the miniplayer when the video is minimized,
     * effectively disabling the miniplayer.
     */
    public static boolean getMiniplayerOnCloseHandler(boolean original) {
        return CURRENT_TYPE == DISABLED || original;
    }

    /**
     * Injection point.
     */
    public static int getMiniplayerDefaultSize(int original) {
        return CURRENT_TYPE == DEFAULT ? MINIPLAYER_SIZE : original;
    }

    /**
     * Injection point.
     */
    public static void hideMiniplayerOverlayButton(View view) {
        Utils.hideViewByRemovingFromParentUnderCondition(HIDE_OVERLAY_BUTTONS_ENABLED, view);
    }

    /**
     * Injection point.
     */
    public static void hideMiniplayerSubTexts(View view) {
        try {
            if (HIDE_SUBTEXT_ENABLED && view instanceof TextView) {
                Logger.printDebug(() -> "Hiding subtext view");
                Utils.hideViewByRemovingFromParentUnderCondition(true, view);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "hideMiniplayerSubTexts failure", ex);
        }
    }
}
