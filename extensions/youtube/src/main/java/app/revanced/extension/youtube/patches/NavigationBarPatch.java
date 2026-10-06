package app.revanced.extension.youtube.patches;

import static app.revanced.extension.shared.Utils.hideViewUnderCondition;
import static app.revanced.extension.youtube.patches.VersionCheckPatch.IS_21_36_OR_GREATER;
import static app.revanced.extension.youtube.shared.NavigationBar.NavigationButton;

import android.os.Build;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.EnumMap;
import java.util.Map;

import app.revanced.extension.shared.Utils;
import app.revanced.extension.youtube.shared.NavigationBar;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class NavigationBarPatch {

    private static final Map<NavigationButton, Boolean> shouldHideMap = new EnumMap<>(NavigationButton.class) {
        {
            put(NavigationButton.HOME, Settings.HIDE_HOME_BUTTON.get());
            put(NavigationButton.CREATE, Settings.HIDE_CREATE_BUTTON.get());
            put(NavigationButton.NOTIFICATIONS, Settings.HIDE_NOTIFICATIONS_BUTTON.get());
            put(NavigationButton.SHORTS, Settings.HIDE_SHORTS_BUTTON.get());
            put(NavigationButton.SUBSCRIPTIONS, Settings.HIDE_SUBSCRIPTIONS_BUTTON.get());
        }
    };

    private static final boolean SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON
            = Settings.SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON.get();

    private static final boolean DISABLE_TRANSLUCENT_STATUS_BAR
            = Settings.DISABLE_TRANSLUCENT_STATUS_BAR.get();

    private static final boolean DISABLE_TRANSLUCENT_NAVIGATION_BAR_LIGHT
            = Settings.DISABLE_TRANSLUCENT_NAVIGATION_BAR_LIGHT.get();

    private static final boolean DISABLE_TRANSLUCENT_NAVIGATION_BAR_DARK
            = Settings.DISABLE_TRANSLUCENT_NAVIGATION_BAR_DARK.get();

    private static final boolean NARROW_NAVIGATION_BUTTONS = Settings.NARROW_NAVIGATION_BUTTONS.get();

    private static final boolean DISABLE_AUTO_HIDE_NAVIGATION_BAR = Settings.DISABLE_AUTO_HIDE_NAVIGATION_BAR.get();

    /**
     * Injection point.
     */
    public static String switchCreateWithNotificationButton(String osName) {
        return SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON
                ? "Android Automotive"
                : osName;
    }

    /**
     * Injection point.
     */
    public static void navigationTabCreated(NavigationButton button, View tabView) {
        if (Boolean.TRUE.equals(shouldHideMap.get(button))) {
            tabView.setVisibility(View.GONE);
        }
    }

    private static boolean showNavigationButtonBadge(@Nullable NavigationButton button, boolean show) {
        if (button == NavigationButton.SUBSCRIPTIONS) {
            return show && !Settings.HIDE_SUBSCRIPTIONS_BUTTON_BADGE.get();
        }
        if (button == NavigationButton.NOTIFICATIONS) {
            return show && !Settings.HIDE_NOTIFICATIONS_BUTTON_BADGE.get();
        }
        return show;
    }

    /**
     * Injection point.
     * Called while a navigation tab is created, before its view is loaded.
     *
     * @param show If the new content dot or unread count is shown.
     */
    public static boolean showNavigationButtonBadge(boolean show) {
        return showNavigationButtonBadge(NavigationBar.getLastLoadedNavigationButton(), show);
    }

    /**
     * Injection point.
     * Called when the badge of a loaded navigation tab changes.
     *
     * @param show If the new content dot or unread count is shown.
     */
    public static boolean showNavigationButtonBadge(View tabView, boolean show) {
        return showNavigationButtonBadge(NavigationBar.getNavigationButton(tabView), show);
    }

    /**
     * Injection point.
     */
    public static void hideNavigationButtonLabels(TextView navigationLabelsView) {
        hideViewUnderCondition(Settings.HIDE_NAVIGATION_BUTTON_LABELS, navigationLabelsView);
    }

    /**
     * Injection point.
     */
    public static boolean useAnimatedNavigationButtons(boolean original) {
        return !IS_21_36_OR_GREATER && Settings.NAVIGATION_BAR_ANIMATIONS.get();
    }

    /**
     * Injection point.
     */
    public static boolean enableNarrowNavigationButton(boolean original) {
        return NARROW_NAVIGATION_BUTTONS || original;
    }

    /**
     * Injection point.
     */
    public static boolean disableAutoHidingNavigationBar() {
        return DISABLE_AUTO_HIDE_NAVIGATION_BAR;
    }

    /**
     * Injection point.
     *
     * @param show If the scroll shows (true) or hides (false) the navigation bar.
     */
    public static boolean showNavigationBarOnScroll(boolean show) {
        return DISABLE_AUTO_HIDE_NAVIGATION_BAR || show;
    }

    /**
     * Injection point.
     */
    public static boolean useTranslucentNavigationStatusBar(boolean original) {
        // Must check Android version, as forcing this on Android 11 or lower causes app hang and crash.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return original;
        }

        if (DISABLE_TRANSLUCENT_STATUS_BAR) {
            return false;
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static boolean useTranslucentNavigationButtons(boolean original) {
        // Feature requires Android 13+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return original;
        }

        if (!DISABLE_TRANSLUCENT_NAVIGATION_BAR_DARK && !DISABLE_TRANSLUCENT_NAVIGATION_BAR_LIGHT) {
            return original;
        }

        if (DISABLE_TRANSLUCENT_NAVIGATION_BAR_DARK && DISABLE_TRANSLUCENT_NAVIGATION_BAR_LIGHT) {
            return false;
        }

        return Utils.isDarkModeEnabled()
                ? !DISABLE_TRANSLUCENT_NAVIGATION_BAR_DARK
                : !DISABLE_TRANSLUCENT_NAVIGATION_BAR_LIGHT;
    }

    // Toolbar

    public static void hideCreateButton(String enumString, View view) {
        if (!Settings.HIDE_TOOLBAR_CREATE_BUTTON.get())
            return;

        hideViewUnderCondition(isCreateButton(enumString), view);
    }

    public static void hideNotificationButton(String enumString, View view) {
        if (!Settings.HIDE_TOOLBAR_NOTIFICATION_BUTTON.get())
            return;

        hideViewUnderCondition(isNotificationButton(enumString), view);
    }

    public static void hideMessagesButton(String enumString, View view) {
        if (!Settings.HIDE_TOOLBAR_MESSAGES_BUTTON.get())
            return;

        hideViewUnderCondition(isMessagesButton(enumString), view);
    }

    public static void hideSearchButton(String enumString, View view) {
        if (!Settings.HIDE_TOOLBAR_SEARCH_BUTTON.get())
            return;

        hideViewUnderCondition(isSearchButton(enumString), view);
    }

    public static void hideSearchButton(MenuItem menuItem, int original) {
        menuItem.setShowAsAction(
                Settings.HIDE_TOOLBAR_SEARCH_BUTTON.get()
                        ? MenuItem.SHOW_AS_ACTION_NEVER
                        : original
        );
    }

    private static boolean isCreateButton(String enumString) {
        return "CREATION_ENTRY".equals(enumString) // Create button for Phone layout.
                || "FAB_CAMERA".equals(enumString); // Create button for Tablet layout.
    }

    private static boolean isNotificationButton(String enumString) {
        return "TAB_ACTIVITY".equals(enumString) // Notification button.
                || "TAB_ACTIVITY_CAIRO".equals(enumString); // Notification button (New layout).
    }

    private static boolean isMessagesButton(String enumString) {
        return "MESSAGE_BUBBLE_OVERLAP".equals(enumString); // Messages inbox button.
    }

    private static boolean isSearchButton(String enumString) {
        return "SEARCH".equals(enumString) // Search button.
                || "SEARCH_CAIRO".equals(enumString) // Search button (New layout).
                || "SEARCH_BOLD".equals(enumString); // Search button (Shorts).
    }
}
