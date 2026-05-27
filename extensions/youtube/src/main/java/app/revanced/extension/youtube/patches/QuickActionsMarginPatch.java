package app.revanced.extension.youtube.patches;

import android.view.View;
import android.view.ViewGroup;

import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class QuickActionsMarginPatch {
    /**
     * Injection point.
     * Sets the top margin of the quick actions container view in dp units.
     */
    public static void setQuickActionsMargin(View view) {
        final int marginDp = Settings.QUICK_ACTIONS_TOP_MARGIN.get();
        if (marginDp == 0) {
            return;
        }
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams params) {
            params.topMargin = Math.round(marginDp * view.getResources().getDisplayMetrics().density);
            view.requestLayout();
        }
    }
}
