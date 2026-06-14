package app.revanced.extension.youtube.patches;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class DisableFullscreenGesturesPatch {

    /**
     * Injection point.
     */
    public static boolean disableFullscreenGestures(String nextGestureType) {
        Logger.printDebug(() -> "The next player gesture will be: " + nextGestureType);
        return ("MAXIMIZED_PULLED_UP".equals(nextGestureType) &&
                Settings.DISABLE_FULLSCREEN_PULLED_UP_GESTURE.get()) ||
                ("MAXIMIZED_TO_FULLSCREEN_SLIDING".equals(nextGestureType)
                        && Settings.DISABLE_FULLSCREEN_SLIDING_GESTURE.get()) ||
                ("FULLSCREEN_DRAGGED_DOWN".equals(nextGestureType)
                        && Settings.DISABLE_FULLSCREEN_DRAGGED_DOWN_GESTURE.get());
    }
}
