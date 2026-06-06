package app.revanced.extension.music.patches;

import app.revanced.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class EnableSwipeToDismissMiniplayerPatch {

    /**
     * Injection point
     */
    public static boolean enableSwipeToDismissMiniplayer() {
        return Settings.ENABLE_SWIPE_TO_DISMISS_MINIPLAYER.get();
    }

    /**
     * Injection point
     */
    public static Object enableSwipeToDismissMiniplayer(Object object) {
        return Settings.ENABLE_SWIPE_TO_DISMISS_MINIPLAYER.get() ? null : object;
    }
}
