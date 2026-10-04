package app.revanced.patches.shared.misc.featureflag

import app.revanced.patcher.invoke

/**
 * Feature flags read by the app through the feature flag accessors.
 *
 * Flag ids are the same across app versions and are assigned in increasing order,
 * so newer flags have higher ids.
 *
 * @param id The flag id.
 * @param translationKey String resource key of a name to show for the flag, if one exists.
 * @param deprecated The flag is no longer present in the newest supported YouTube version.
 */
enum class FeatureFlag(
    val id: Long,
    val translationKey: String? = null,
    val deprecated: Boolean = false,
) {
    /** Current time of HLS livestreams. */
    HLS_LIVESTREAM_CURRENT_TIME_ENABLED(45355374L),

    /** Open the Shorts player on app startup if the app was closed in Shorts. */
    RESUME_SHORTS_ON_STARTUP(45358360L),

    /** Back gesture scrolls to the top and refreshes the feed instead of exiting. */
    BACK_TO_REFRESH(45359221L),

    /** Swipe up to enter fullscreen. */
    SWIPE_UP_TO_FULLSCREEN(45379021L),

    /** Seekbar thumbnails in fullscreen. */
    FULLSCREEN_SEEKBAR_THUMBNAILS(45398577L, deprecated = true),

    /** Translucent status bar. */
    TRANSLUCENT_STATUS_BAR(45400535L),

    /** Legacy double speed seek gesture (before 19.17). */
    DOUBLE_SPEED_SEEK_GESTURE_LEGACY(45411330L, deprecated = true),

    /** Gradient loading screen. */
    GRADIENT_LOADING_SCREEN(45412406L),

    /** Background playback of Shorts. */
    SHORTS_BACKGROUND_PLAYBACK(45415425L),

    /** Native (Upb) protobuf parsing of the Litho converter buffer. */
    NATIVE_PROTOBUF_PARSING(45419603L, deprecated = true),

    /** Cairo settings fragment. */
    CAIRO_SETTINGS_FRAGMENT(45532100L),

    /**
     * Purpose unclear. Read when an engagement panel is shown,
     * schedules an extra background task when on.
     */
    UNKNOWN_ENGAGEMENT_PANEL_FEATURE_ENABLED(45615449L),

    /** Translucent YouTube navigation buttons. */
    TRANSLUCENT_NAVIGATION_BUTTONS(45630927L),

    /** Swipe to change the video in fullscreen. */
    SWIPE_TO_CHANGE_VIDEO_IN_FULLSCREEN(45631116L, deprecated = true),

    /**
     * Native (Upb) code that handles Litho component names.
     * Breaks Litho filtering because component names become null.
     */
    NATIVE_LITHO_COMPONENT_NAMES(45631264L, deprecated = true),

    /** Translucent device back, home and recent buttons. */
    TRANSLUCENT_SYSTEM_NAVIGATION_BAR(45632194L),

    /** Picture-in-picture input consumer. Breaks the PiP buttons after locking the screen. */
    PIP_INPUT_CONSUMER_ENABLED(45638483L, deprecated = true),

    /** Different player bottom controls layout (youtube_video_exploder_controls_bottom_ui_container). */
    EXPLODER_BOTTOM_CONTROLS_LAYOUT(45643739L),

    /** Media fetch hot config stream config replacement. Interferes with spoofing. */
    MEDIA_FETCH_HOT_CONFIG_REPLACEMENT(45645570L),

    /** Shorts experimental UI renderer. Requires the experimental Shorts player. */
    SHORTS_EXPERIMENTAL_UI_RENDERER(45649743L),

    /** Handler that immediately closes the miniplayer. */
    MINIPLAYER_DISABLED(45657015L),

    /** Playback start parses the player response. Interferes with spoofing. */
    PARSE_PLAYBACK_RESPONSE_ON_START(45665455L),

    /** Open videos in fullscreen portrait. */
    OPEN_VIDEOS_FULLSCREEN_PORTRAIT(45666112L),

    /** Select the localized audio track as default. */
    LOCALIZED_DEFAULT_AUDIO_TRACK(45666189L, deprecated = true),

    /** New Litho text creation. */
    NEW_LITHO_TEXT_CREATION(45675738L),

    /** Auto-hide the navigation bar. */
    AUTO_HIDE_NAVIGATION_BAR(45677535L, deprecated = true),

    /** Experimental Shorts player. */
    SHORTS_EXPERIMENTAL_PLAYER(45677719L),

    /** Animated navigation bar tabs. */
    ANIMATED_NAVIGATION_TABS(45680008L),

    /** Media fetch hot config stream config replacement (alternative flag, YouTube 20.10 - 20.13). */
    MEDIA_FETCH_HOT_CONFIG_REPLACEMENT_ALTERNATIVE(45683169L, deprecated = true),

    /** Bold icons. */
    BOLD_ICONS(45685201L),

    /** Large player buttons in fullscreen. */
    LARGE_FULLSCREEN_BUTTONS(45686474L, deprecated = true),

    /** Cast button in the feed action bar. */
    SHOW_CAST_BUTTON_IN_FEED(45690090L),

    /** Cast button in the player. */
    SHOW_CAST_BUTTON_IN_PLAYER(45690091L),

    /** Large seekbar in fullscreen. */
    LARGE_FULLSCREEN_SEEKBAR(45691569L, deprecated = true),

    /**
     * Purpose unclear, read in ~100 places. Interferes with background playback of some videos.
     * 21.36 requires it, otherwise the player controls are not shown.
     */
    UNKNOWN_PLAYER_FEATURE_ENABLED(45698813L),

    /** Large player overlay buttons. */
    LARGE_OVERLAY_BUTTONS(45709810L),

    /** New advanced video quality menu style. Breaks opening the advanced quality menu. */
    NEW_ADVANCED_QUALITY_MENU_STYLE(45712556L),

    /** Stroke around player buttons. */
    PLAYER_BUTTON_STROKE(45713296L),

    /** Use server provided minimum and maximum playback speeds. */
    SERVER_SIDE_PLAYBACK_SPEED_LIMITS(45719140L),
    ;

    /**
     * A fingerprint instruction filter matching the flag id literal.
     */
    operator fun invoke(compare: Long.(Long) -> Boolean = Long::equals) = id(compare)
}
