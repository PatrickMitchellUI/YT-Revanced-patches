package app.revanced.extension.youtube.patches;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.shared.ui.Dim;
import app.revanced.extension.youtube.patches.ActionButtonStylePatch.ActionButtonStyle;
import app.revanced.extension.youtube.settings.Settings;

/**
 * Draws the like and dislike counts next to the like and dislike buttons of the new action bar.
 * <p>
 * The template of the new action bar draws icons only, so the counts are drawn here: the like and dislike
 * buttons are widened by the width of their count, and the buttons row grows to the left into the free space,
 * so the other buttons keep their position. If the free space is too small for the counts,
 * the counts are drawn below the icons instead.
 * The counts are drawn in the overlay of the buttons, so tapping a count taps its button.
 * <p>
 * Litho lays out the buttons again when the element changes (such as after liking),
 * so the bounds are checked and changed again before every frame.
 */
@SuppressWarnings("unused")
public final class CompactActionBarCounts {

    /**
     * Added to the description of the toggled state of the like and dislike buttons,
     * as the description is the same in both states. Not read by screen readers.
     */
    static final String TOGGLED_MARKER = "\u200B";

    /**
     * Like counts of the new action bar, from the element of the action bar.
     *
     * @param notLiked Like count without the like of the user, or null if the likes are hidden.
     * @param liked    Like count with the like of the user, or null if the likes are hidden.
     */
    record Counts(String videoId, String likeDescription, String dislikeDescription,
                  @Nullable String notLiked, @Nullable String liked) {
    }

    private static final int TEXT_SIZE_SP = 14;
    private static final int STACKED_TEXT_SIZE_SP = 12;
    /**
     * Minimum space above and below a count drawn below its icon.
     */
    private static final int STACKED_GAP_DP = 1;
    private static final int ICON_GAP_DP = 4;
    /**
     * Space after the count. The next button has padding around its icon too.
     */
    private static final int END_PADDING_DP = 2;
    /**
     * Minimum space kept free left of the buttons row.
     */
    private static final int MIN_FREE_SPACE_DP = 8;
    private static final long SEARCH_INTERVAL_MILLISECONDS = 500;
    private static final long DISLIKES_RETRY_MILLISECONDS = 500;
    /**
     * How long to wait for the dislikes of a video, as the votes may never be fetched
     * (no connection, Return YouTube Dislike patch not included).
     */
    private static final long DISLIKES_WAIT_MILLISECONDS = 10_000;

    @Nullable
    private static volatile Counts counts;

    private static WeakReference<View> likeButtonRef = new WeakReference<>(null);
    private static WeakReference<View> dislikeButtonRef = new WeakReference<>(null);
    private static long lastSearchTime;
    private static volatile long dislikesWaitEnd;

    /**
     * Bounds set by this class, to detect when Litho lays out a view again.
     * Values: left and right before the change, left and right after the change.
     */
    private static final Map<View, int[]> changedBounds = new WeakHashMap<>();
    private static final Map<View, CountDrawable> countDrawables = new WeakHashMap<>();

    private static final Paint paint = createPaint(TEXT_SIZE_SP);
    private static final Paint stackedPaint = createPaint(STACKED_TEXT_SIZE_SP);
    private static final float STACKED_TEXT_SIZE = stackedPaint.getTextSize();

    private static Paint createPaint(int textSizeSp) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSizeSp,
                Utils.getContext().getResources().getDisplayMetrics()));
        return paint;
    }

    static void setCounts(Counts newCounts) {
        Counts current = counts;
        if (current == null || !current.videoId.equals(newCounts.videoId)) {
            dislikesWaitEnd = System.currentTimeMillis() + DISLIKES_WAIT_MILLISECONDS;
        }
        counts = newCounts;
    }

    /**
     * Injection point.
     */
    public static void setMainActivity(Activity activity) {
        try {
            View decorView = activity.getWindow().getDecorView();
            decorView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    try {
                        update(decorView);
                    } catch (Exception ex) {
                        Logger.printException(() -> "onPreDraw failure", ex);
                    }
                    return true;
                }
            });
        } catch (Exception ex) {
            Logger.printException(() -> "setMainActivity failure", ex);
        }
    }

    private static boolean isEnabled() {
        return Settings.ACTION_BUTTON_STYLE.get() == ActionButtonStyle.DEFAULT
                && Settings.SHOW_COMPACT_LIKE_DISLIKE_COUNTS.get();
    }

    private static void update(View decorView) {
        Counts current = counts;
        if (current == null || !isEnabled()) {
            reset();
            return;
        }

        View like = likeButtonRef.get();
        View dislike = dislikeButtonRef.get();
        // Either button can be hidden.
        if (!isButton(like, current.likeDescription) && !isButton(dislike, current.dislikeDescription)) {
            reset();
            final long now = System.currentTimeMillis();
            if (now - lastSearchTime < SEARCH_INTERVAL_MILLISECONDS) return;
            lastSearchTime = now;

            like = findButton(decorView, current.likeDescription);
            dislike = findButton(decorView, current.dislikeDescription);
            if (like == null && dislike == null) return;
            likeButtonRef = new WeakReference<>(like);
            dislikeButtonRef = new WeakReference<>(dislike);
        } else {
            if (!isButton(like, current.likeDescription)) like = null;
            if (!isButton(dislike, current.dislikeDescription)) dislike = null;
        }

        layOut(current, like, dislike);
    }

    private static void layOut(Counts current, @Nullable View like, @Nullable View dislike) {
        // Button -> wrappers with the same bounds -> buttons row.
        View likeTop = like == null ? null : topOfButton(like);
        View dislikeTop = dislike == null ? null : topOfButton(dislike);
        View anyTop = likeTop != null ? likeTop : dislikeTop;
        if (anyTop == null || !(anyTop.getParent() instanceof ViewGroup row)
                || (likeTop != null && likeTop.getParent() != row)
                || (dislikeTop != null && dislikeTop.getParent() != row)
                || !(row.getParent() instanceof ViewGroup bar)
                || row.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            reset();
            return;
        }

        String likeText = null;
        if (like != null) {
            likeText = isToggled(like) ? current.liked : current.notLiked;
        }
        String dislikeText = null;
        if (dislike != null) {
            dislikeText = ReturnYouTubeDislikePatch.getDislikeText(current.videoId);
            if (dislikeText == null && Settings.RYD_ENABLED.get() && System.currentTimeMillis() < dislikesWaitEnd) {
                // The votes may not be fetched yet.
                dislike.postInvalidateDelayed(DISLIKES_RETRY_MILLISECONDS);
            }
        }

        int likeExtra = inlineExtra(like, likeText);
        int dislikeExtra = inlineExtra(dislike, dislikeText);

        // Use the free space left of the buttons row only.
        final int rowLeft = baseLeft(row);
        int freeSpaceStart = 0;
        for (int i = 0, count = bar.getChildCount(); i < count; i++) {
            View child = bar.getChildAt(i);
            if (child != row && child.getRight() <= rowLeft) {
                freeSpaceStart = Math.max(freeSpaceStart, contentRight(child));
            }
        }
        final int freeSpace = rowLeft - freeSpaceStart - Dim.dp(MIN_FREE_SPACE_DP);
        // Draw the counts below the icons if they do not fit next to them.
        final boolean stacked = likeExtra + dislikeExtra > freeSpace;
        if (stacked) {
            likeExtra = 0;
            dislikeExtra = 0;
        }
        setCount(like, likeText, stacked);
        setCount(dislike, dislikeText, stacked);

        // The row grows to the left. Widened buttons push the following children to the right,
        // so the children after the last widened button keep their position.
        setHorizontalBounds(row, rowLeft - likeExtra - dislikeExtra, baseRight(row));
        int offset = 0;
        for (int i = 0, count = row.getChildCount(); i < count; i++) {
            View child = row.getChildAt(i);
            final int left = baseLeft(child) + offset;
            final int right = baseRight(child) + offset;
            if (child == likeTop) {
                widen(child, like, left, right, likeExtra);
                offset += likeExtra;
            } else if (child == dislikeTop) {
                widen(child, dislike, left, right, dislikeExtra);
                offset += dislikeExtra;
            } else {
                setHorizontalBounds(child, left, right);
            }
        }
    }

    /**
     * @return The width the button needs in addition to show the count next to its icon.
     */
    private static int inlineExtra(@Nullable View button, @Nullable String text) {
        if (button == null || text == null) return 0;
        final float textEnd = iconRight(button) + Dim.dp(ICON_GAP_DP)
                + paint.measureText(text) + Dim.dp(END_PADDING_DP);
        return Math.max(0, (int) Math.ceil(textEnd) - baseWidth(button));
    }

    private static void setCount(@Nullable View button, @Nullable String text, boolean stacked) {
        if (button == null) return;
        CountDrawable drawable = countDrawable(button);
        ImageView icon = icon(button);
        if (text == null || !stacked || icon == null) {
            drawable.setInlineText(text, button, baseWidth(button));
            return;
        }

        // Center the count in the space below the icon, with a smaller text size if the space is too small.
        final int height = button.getHeight();
        final int space = height - icon.getBottom();
        stackedPaint.setTextSize(STACKED_TEXT_SIZE);
        Rect digitBounds = new Rect();
        stackedPaint.getTextBounds("0", 0, 1, digitBounds);
        final int maxDigitHeight = space - 2 * Dim.dp(STACKED_GAP_DP);
        if (digitBounds.height() > maxDigitHeight) {
            stackedPaint.setTextSize(STACKED_TEXT_SIZE * Math.max(0, maxDigitHeight) / digitBounds.height());
            stackedPaint.getTextBounds("0", 0, 1, digitBounds);
        }

        final int width = baseWidth(button);
        final float textWidth = stackedPaint.measureText(text);
        final float iconCenter = (icon.getLeft() + icon.getRight()) / 2f;
        final float textX = Math.max(0, Math.min(width - textWidth, iconCenter - textWidth / 2));
        drawable.setText(text, stackedPaint, textX,
                icon.getBottom() + (space + digitBounds.height()) / 2f, width, height);
    }

    @Nullable
    private static ImageView icon(View button) {
        if (button instanceof ViewGroup group) {
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                if (group.getChildAt(i) instanceof ImageView icon) return icon;
            }
        }
        return null;
    }

    private static int iconRight(View button) {
        if (button instanceof ViewGroup group) {
            int right = 0;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                right = Math.max(right, group.getChildAt(i).getRight());
            }
            if (right > 0) return right;
        }
        return button.getHeight();
    }

    /**
     * Widens the view and its single children down to the button.
     */
    private static void widen(View top, View button, int left, int right, int extra) {
        setHorizontalBounds(top, left, right + extra);
        View view = top;
        while (view != button && view instanceof ViewGroup group && group.getChildCount() == 1) {
            view = group.getChildAt(0);
            setHorizontalBounds(view, baseLeft(view), baseRight(view) + extra);
        }
    }

    /**
     * @return The topmost ancestor with the same horizontal bounds, which is a child of the buttons row.
     */
    private static View topOfButton(View button) {
        View view = button;
        while (view.getParent() instanceof ViewGroup parent && parent.getChildCount() == 1) {
            view = parent;
        }
        return view;
    }

    private static int contentRight(View view) {
        if (!(view instanceof ViewGroup group) || group.getChildCount() == 0) return view.getRight();

        int right = view.getLeft();
        for (int i = 0, count = group.getChildCount(); i < count; i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                right = Math.max(right, view.getLeft() + contentRight(child));
            }
        }
        return right;
    }

    private static boolean isToggled(View button) {
        CharSequence description = button.getContentDescription();
        return description != null && description.toString().endsWith(TOGGLED_MARKER);
    }

    private static boolean isButton(@Nullable View view, String description) {
        if (view == null || !view.isAttachedToWindow()) return false;
        CharSequence viewDescription = view.getContentDescription();
        return viewDescription != null && viewDescription.toString().replace(TOGGLED_MARKER, "").equals(description);
    }

    @Nullable
    private static View findButton(View view, String description) {
        if (view.getVisibility() != View.VISIBLE) return null;
        if (isButton(view, description)) return view;
        if (view instanceof ViewGroup group) {
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View found = findButton(group.getChildAt(i), description);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static CountDrawable countDrawable(View button) {
        CountDrawable drawable = countDrawables.get(button);
        if (drawable == null) {
            drawable = new CountDrawable();
            countDrawables.put(button, drawable);
            button.getOverlay().add(drawable);
        }
        return drawable;
    }

    /**
     * Removes the counts and restores the bounds set by Litho.
     */
    private static void reset() {
        if (countDrawables.isEmpty() && changedBounds.isEmpty()) return;

        for (Map.Entry<View, CountDrawable> entry : countDrawables.entrySet()) {
            entry.getKey().getOverlay().remove(entry.getValue());
        }
        countDrawables.clear();

        for (Map.Entry<View, int[]> entry : changedBounds.entrySet()) {
            View view = entry.getKey();
            int[] bounds = entry.getValue();
            if (view.getLeft() == bounds[2] && view.getRight() == bounds[3]) {
                view.setLeft(bounds[0]);
                view.setRight(bounds[1]);
            }
        }
        changedBounds.clear();
        likeButtonRef = new WeakReference<>(null);
        dislikeButtonRef = new WeakReference<>(null);
    }

    /**
     * @return The bounds set by Litho, before the change of this class.
     */
    @Nullable
    private static int[] originalBounds(View view) {
        int[] bounds = changedBounds.get(view);
        if (bounds == null) return null;
        if (view.getLeft() == bounds[2] && view.getRight() == bounds[3]) return bounds;

        // Litho laid out the view again.
        changedBounds.remove(view);
        return null;
    }

    private static int baseLeft(View view) {
        int[] bounds = originalBounds(view);
        return bounds == null ? view.getLeft() : bounds[0];
    }

    private static int baseRight(View view) {
        int[] bounds = originalBounds(view);
        return bounds == null ? view.getRight() : bounds[1];
    }

    private static int baseWidth(View view) {
        return baseRight(view) - baseLeft(view);
    }

    private static void setHorizontalBounds(View view, int left, int right) {
        final int originalLeft = baseLeft(view);
        final int originalRight = baseRight(view);
        if (view.getLeft() != left || view.getRight() != right) {
            view.setLeft(left);
            view.setRight(right);
            view.invalidate();
        }
        changedBounds.put(view, new int[]{originalLeft, originalRight, left, right});
    }

    /**
     * Count drawn right of or below the icon of a button.
     */
    private static final class CountDrawable extends Drawable {
        @Nullable
        private String text;
        private Paint textPaint = paint;
        private float textX;
        private float baseline;

        /**
         * Draws the count right of the icon, vertically centered.
         *
         * @param baseWidth Width of the button set by Litho.
         */
        void setInlineText(@Nullable String newText, View button, int baseWidth) {
            if (newText == null) {
                setText(null, paint, 0, 0, 0, 0);
                return;
            }

            final float x = iconRight(button) + Dim.dp(ICON_GAP_DP);
            final float textEnd = x + paint.measureText(newText) + Dim.dp(END_PADDING_DP);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            final int height = button.getHeight();
            setText(newText, paint, x, height / 2f - (metrics.ascent + metrics.descent) / 2,
                    Math.max(baseWidth, (int) Math.ceil(textEnd)), height);
        }

        void setText(@Nullable String newText, Paint newPaint, float x, float newBaseline, int width, int height) {
            if (!Objects.equals(text, newText)) {
                Logger.printDebug(() -> "Count changed from: " + text + " to: " + newText);
            }
            text = newText;
            textPaint = newPaint;
            textX = x;
            baseline = newBaseline;
            if (newText != null) setBounds(0, 0, width, height);
            invalidateSelf();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            String drawnText = text;
            if (drawnText == null) return;

            textPaint.setColor(Utils.getAppForegroundColor());
            canvas.drawText(drawnText, textX, baseline, textPaint);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
