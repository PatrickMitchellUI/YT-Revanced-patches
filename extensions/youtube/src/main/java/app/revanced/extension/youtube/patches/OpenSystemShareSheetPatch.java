package app.revanced.extension.youtube.patches;

import android.content.Intent;
import android.os.SystemClock;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.PlayerType;
import app.revanced.extension.youtube.shared.ShortsPlayerState;

import java.lang.ref.WeakReference;

/**
 * Replaces YouTube's in-app share sheet with the system share sheet.
 */
@SuppressWarnings("unused")
public final class OpenSystemShareSheetPatch {

    public static WeakReference<RecyclerView> flyoutMenuRecyclerView = new WeakReference<>(null);
    private static boolean waitUntilClosingDone;

    /**
     * Injection point.
     */
    public static void onFlyoutMenuCreate(final RecyclerView recyclerView) {
        flyoutMenuRecyclerView = new WeakReference<>(recyclerView);
    }

    /**
     * Injection point.
     */
    public static void openSystemShareSheet() {
        if (!Settings.OPEN_SYSTEM_SHARE_SHEET.get()) return;
        if (!(PlayerType.getCurrent().isMaximizedOrFullscreen() || ShortsPlayerState.isOpen())) return;

        final String prefixURL = (Settings.REPLACE_LINKS_WITH_SHORTENER.get()
                ? "https://youtu.be/"
                : "https://www.youtube.com/watch?v="
        );

        final String intentUrl = prefixURL + VideoInformation.getVideoId();

        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, intentUrl);
        final Intent chooserIntent = Intent.createChooser(shareIntent, "");
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        try {
            Utils.getContext().startActivity(chooserIntent);
        } catch (Exception ex) {
            Logger.printException(() -> "Can not open System Share panel: " + intentUrl, ex);
        }
    }

    public static void closeLithoAppShareSheet() {
        if (waitUntilClosingDone) {
            return;
        }
        waitUntilClosingDone = true;

        final RecyclerView shareSheetRecyclerView = flyoutMenuRecyclerView.get();
        if (shareSheetRecyclerView != null) {
            final View decorView = shareSheetRecyclerView.getRootView();

            if (decorView != null) {
                float clickX = decorView.getWidth() * 0.5f;
                float clickY = decorView.getHeight() * 0.25f;

                if (clickX <= 0 || clickY <= 0) {
                    clickX = clickY = 200.0f;
                }

                final long eventTime = SystemClock.uptimeMillis();

                for (int i = 0; i < 2; i++) {
                    final boolean firstIteration = i == 0;
                    final MotionEvent touchEvent = MotionEvent.obtain(
                            eventTime,
                            firstIteration ? eventTime : eventTime + 10,
                            firstIteration ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP,
                            clickX,
                            clickY,
                            0
                    );
                    decorView.dispatchTouchEvent(touchEvent);
                    touchEvent.recycle();
                }

                // Given the speed of Litho's elements renders, disable 'waitUntilClosingDone'
                // after a 500ms delay to ensure that only a single filtered
                // Litho object triggers the panel's closure.
                Utils.runOnMainThreadDelayed(
                        () -> waitUntilClosingDone = false,
                        500
                );
            }
        }
    }
}
