package app.revanced.extension.shared.patches;

import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.patches.litho.FilterGroup.ByteArrayFilterGroup;
import app.revanced.extension.shared.settings.YouTubeAndMusicSettings;

@SuppressWarnings("unused")
public class HideFullscreenAdsPatch {

    private static final ByteArrayFilterGroup fullscreenAd = new ByteArrayFilterGroup(
            null,
            "_interstitial"
    );

    /**
     * Injection point.
     * Called from a different place then the other filters.
     */
    // TODO: Extract this into a youtube-shared patch
    public static void closeFullscreenAd(Object customDialog, @Nullable byte[] buffer) {
        try {
            if (!YouTubeAndMusicSettings.HIDE_FULLSCREEN_ADS.get()) {
                return;
            }

            if (buffer == null) {
                Logger.printDebug(() -> "buffer is null");
                return;
            }

            if (customDialog instanceof Dialog dialog &&
                    fullscreenAd.check(buffer).isFiltered()) {
                Logger.printDebug(() -> "Closing fullscreen ad");

                Window window = dialog.getWindow();

                if (window != null) {
                    // Set the dialog size to 0 before closing
                    // If the dialog is not resized to 0, it will remain visible for about a second before closing
                    WindowManager.LayoutParams params = window.getAttributes();
                    params.height = 0;
                    params.width = 0;

                    // Change the size of dialog to 0
                    window.setAttributes(params);

                    // Disable dialog's background dim
                    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                    // Restore window flags
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);

                    // Restore decorView visibility
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                }

                // Dismiss dialog
                dialog.dismiss();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "closeFullscreenAd failure", ex);
        }
    }
}
