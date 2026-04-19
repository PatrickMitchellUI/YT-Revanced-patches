package app.revanced.extension.music.patches;

import app.revanced.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class HideAdsPatch {

    /**
     * Injection point
     */
    public static boolean hideGetPremiumLabel() {
        return Settings.HIDE_GET_PREMIUM_LABEL.get();
    }
}
