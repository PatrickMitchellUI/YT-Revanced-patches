package app.revanced.extension.youtube.patches;

import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.Utils;

@SuppressWarnings("unused")
public class PlayerControlsPatch {

    private static boolean fullscreenButtonVisibilityCallbacksExist() {
        return false; // Modified during patching if needed.
    }

    /**
     * Injection point.
     */
    public static void hideBottomGradientScrim(ImageView bottomGradientScrim) {
        if (bottomGradientScrim != null) {
            Utils.runOnMainThread(() -> {
                bottomGradientScrim.setImageAlpha(0);
                bottomGradientScrim.setVisibility(View.GONE);
            });
        }
    }

    /**
     * Injection point.
     */
    public static void setFullscreenCloseButton(View button) {
        if (!fullscreenButtonVisibilityCallbacksExist()) {
            return;
        }

        Logger.printDebug(() -> "Fullscreen button set");

        // Add a global listener, since the protected method
        // View#onVisibilityChanged() does not have any call backs.
        button.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            int lastVisibility = View.VISIBLE;

            @Override
            public void onGlobalLayout() {
                try {
                    final int visibility = button.getVisibility();
                    if (lastVisibility != visibility) {
                        lastVisibility = visibility;

                        Logger.printDebug(() -> "fullscreen button visibility: "
                                + (visibility == View.VISIBLE ? "VISIBLE" :
                                visibility == View.GONE ? "GONE" : "INVISIBLE"));

                        fullscreenButtonVisibilityChanged(visibility == View.VISIBLE);
                    }
                } catch (Exception ex) {
                    Logger.printDebug(() -> "OnGlobalLayoutListener failure", ex);
                }
            }
        });
    }

    // noinspection EmptyMethod
    private static void fullscreenButtonVisibilityChanged(boolean isVisible) {
        // Code added by patch.
    }
}
