package app.revanced.extension.youtube.patches;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.ResourceType;
import app.revanced.extension.shared.Utils;

@SuppressWarnings("unused")
public class PlayerControlsPatch {

    private static final int PLAYER_BUTTON_BACKGROUND_ID =
            Utils.getResourceIdentifier(ResourceType.DRAWABLE, "player_button_background");

    public static WeakReference<View> fullscreenButtonRef = new WeakReference<>(null);

    private static boolean fullscreenButtonVisibilityCallbacksExist() {
        return false; // Modified during patching if needed.
    }

    /**
     * Injection point.
     */
    public static void setFullscreenCloseButton(View button) {
        fullscreenButtonRef = new WeakReference<>(button);
        Logger.printDebug(() -> "Fullscreen button set");

        if (!fullscreenButtonVisibilityCallbacksExist()) {
            return;
        }

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

    /**
     * Injection point.
     * <p>
     * The background of the fullscreen button on 21.36 is a ripple with a filled circle,
     * which is replaced with the background of the other bottom buttons (only the press ripple).
     */
    public static Drawable getFullscreenButtonBackground(Drawable background) {
        if (PLAYER_BUTTON_BACKGROUND_ID == 0) return background;
        return Utils.getContext().getDrawable(PLAYER_BUTTON_BACKGROUND_ID);
    }

    /**
     * Injection point.
     * <p>
     * 21.36 draws the background of the fullscreen button inside these insets, which also become
     * the padding of the button. They are made for a 60 dp wide button with the icon 6 dp above the center.
     * Equal insets center the icon and its press ripple in the 48 dp button like the other bottom buttons,
     * which is moved up 6 dp by the patch.
     */
    public static Rect getFullscreenButtonBackgroundInsets(Rect insets) {
        final int inset = (insets.top + insets.bottom) / 2;
        return new Rect(inset, inset, inset, inset);
    }

    // noinspection EmptyMethod
    private static void fullscreenButtonVisibilityChanged(boolean isVisible) {
        // Code added during patching.
    }
}
