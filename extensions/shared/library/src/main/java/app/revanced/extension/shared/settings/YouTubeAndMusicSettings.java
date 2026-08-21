package app.revanced.extension.shared.settings;

import static app.revanced.extension.shared.settings.Setting.parent;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class YouTubeAndMusicSettings extends BaseSettings {
    // Ads
    public static final BooleanSetting HIDE_FULLSCREEN_ADS = new BooleanSetting("revanced_hide_fullscreen_ads", TRUE);

    public static final BooleanSetting REMOVE_VIEWER_DISCRETION_DIALOG = new BooleanSetting("revanced_remove_viewer_discretion_dialog", FALSE, true);

    // Custom filter
    public static final BooleanSetting CUSTOM_FILTER = new BooleanSetting("revanced_custom_filter", FALSE);
    public static final StringSetting CUSTOM_FILTER_STRINGS = new StringSetting("revanced_custom_filter_strings", "", true, parent(CUSTOM_FILTER));

    // Miscellaneous
    public static final BooleanSetting DEBUG_PROTOCOLBUFFER = new BooleanSetting("revanced_debug_protocolbuffer", FALSE, false,
            "revanced_debug_protocolbuffer_user_dialog_message", parent(BaseSettings.DEBUG));
    public static final BooleanSetting DEBUG_SPANNABLE = new BooleanSetting("revanced_debug_spannable", FALSE, parent(DEBUG));
}
