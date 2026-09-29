package app.revanced.extension.youtube.patches;

import android.graphics.Rect;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.Nullable;

import java.util.WeakHashMap;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.ResourceType;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.NavigationBar;
import app.revanced.extension.youtube.shared.NavigationBar.NavigationButton;

/**
 * Makes the space between the items of the home feed, the subscriptions feed, the search results
 * and the watch page feed the same.
 * <p>
 * The space comes from three sources that differ per feed and change with the server layout:
 * padding inside the items, the feed item decoration, and margins of the flow layout manager.
 * The subscriptions feed also loses the margins after switching tabs. Instead of adjusting each source,
 * the gap between the content of an item and the next item is measured after layout and corrected
 * with an extra bottom offset of the item. The frame with the wrong spacing is not drawn.
 */
@SuppressWarnings("unused")
public final class FeedSpacingPatch {

    /**
     * Space below an item. YouTube uses this in the search results.
     */
    private static final float ITEM_SPACING_DP = 24;

    /**
     * Frames in a row that can be skipped for a correction, in case a correction has no effect.
     */
    private static final int MAX_SKIPPED_FRAMES = 3;

    /**
     * Differences in spacing up to this are not corrected.
     */
    private static final int MEASURE_PRECISION = 2;

    private static final int RESULTS_LIST_ID = Utils.getResourceIdentifier(ResourceType.ID, "results");
    private static final int WATCH_LIST_ID = Utils.getResourceIdentifier(ResourceType.ID, "watch_list");
    private static final int ENGAGEMENT_PANEL_ID = Utils.getResourceIdentifier(ResourceType.ID, "engagement_panel");

    /**
     * Feed lists, and null for lists that are not changed.
     */
    private static final WeakHashMap<ViewGroup, FeedList> lists = new WeakHashMap<>();

    /**
     * Injection point. Called after the offsets of all item decorations of an item were added up.
     */
    public static void addItemOffsets(Rect outRect, View item, ViewGroup list) {
        try {
            FeedList feedList = getFeedList(list);
            if (feedList == null) return;

            final int position = getChildPosition(list, item);
            if (position >= 0) {
                outRect.bottom += feedList.offsets.get(position, feedList.defaultOffset);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "addItemOffsets failure", ex);
        }
    }

    @Nullable
    private static FeedList getFeedList(ViewGroup list) {
        if (lists.containsKey(list)) {
            return lists.get(list);
        }

        FeedList feedList = isFeedList(list) ? new FeedList(list) : null;
        lists.put(list, feedList);
        return feedList;
    }

    private static boolean isFeedList(ViewGroup list) {
        if (!Settings.FEED_SPACING.get()) return false;

        final int id = list.getId();
        if (id == WATCH_LIST_ID) return true;
        if (id != RESULTS_LIST_ID) return false;
        // The panels of the watch page (comments, description) use the same list id.
        if (isInEngagementPanel(list)) return false;
        if (NavigationBar.isSearchBarActive()) return true;

        // The list is created when its tab is opened.
        NavigationButton button = NavigationButton.getSelectedNavigationButton();
        return button == null || button == NavigationButton.HOME || button == NavigationButton.SUBSCRIPTIONS;
    }

    private static boolean isInEngagementPanel(View view) {
        for (var parent = view.getParent(); parent instanceof View parentView; parent = parent.getParent()) {
            if (parentView.getId() == ENGAGEMENT_PANEL_ID) return true;
        }
        return false;
    }

    private static final class FeedList implements ViewTreeObserver.OnPreDrawListener {
        private final ViewGroup list;
        private final int itemSpacing;

        /**
         * The watch page list starts with the video metadata (title, channel, action buttons, comments).
         * These items draw their spacing inside their content, and YouTube's spacing is kept for them.
         */
        private final boolean isWatchList;

        /**
         * Extra bottom offset of the items, by adapter position.
         */
        private final SparseIntArray offsets = new SparseIntArray();

        /**
         * Offset of items that were not measured yet. Most items of a feed need the same correction.
         */
        private int defaultOffset;

        private int skippedFrames;

        FeedList(ViewGroup list) {
            this.list = list;
            isWatchList = list.getId() == WATCH_LIST_ID;
            itemSpacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    ITEM_SPACING_DP, list.getResources().getDisplayMetrics());
            list.getViewTreeObserver().addOnPreDrawListener(this);
        }

        @Override
        public boolean onPreDraw() {
            try {
                if (!list.isAttachedToWindow()) {
                    list.getViewTreeObserver().removeOnPreDrawListener(this);
                    return true;
                }
                if (!list.isShown() || !correctSpacing()) {
                    skippedFrames = 0;
                    return true;
                }

                invalidateItemDecorations(list);
                // Draw the corrected layout instead, unless corrections keep failing.
                return ++skippedFrames > MAX_SKIPPED_FRAMES;
            } catch (Exception ex) {
                Logger.printException(() -> "onPreDraw failure", ex);
                return true;
            }
        }

        /**
         * @return If an offset changed.
         */
        private boolean correctSpacing() {
            final int listWidth = list.getWidth();
            final int listHeight = list.getHeight();

            boolean changed = false;
            View upper = null;
            int upperPosition = -1;
            int upperContentBottom = 0;

            for (int i = 0, count = list.getChildCount(); i < count; i++) {
                View item = list.getChildAt(i);
                final int position = getChildPosition(list, item);
                if (position < 0) continue;

                if (item.getHeight() == 0) {
                    // Hidden item. Keep it from adding space.
                    changed |= setOffset(position, 0);
                    continue;
                }

                // Only gaps on screen are corrected. Items outside of it are not laid out again,
                // so a correction would not change the gap there.
                if (upper != null && upper.getBottom() <= listHeight && item.getTop() >= 0 && item.getTop() < listHeight) {
                    final int gap = item.getTop() - (upper.getTop() + upperContentBottom);
                    final int correction = itemSpacing - gap;

                    if (Math.abs(correction) > MEASURE_PRECISION) {
                        // More space than the spacing is never needed, and less than the item never makes sense.
                        final int offset = Math.max(-upper.getHeight(), Math.min(itemSpacing,
                                offsets.get(upperPosition, defaultOffset) + correction));
                        changed |= setOffset(upperPosition, offset);
                        defaultOffset = offset;
                    }
                }

                hidePlaybackBackground(item);

                // Items next to each other are not supported.
                final int contentBottom = item.getWidth() == listWidth ? contentBottom(item) : -1;
                if (isWatchList && contentBottom > 0 && !isVideoItem(item, contentBottom)) {
                    changed |= setOffset(position, 0);
                    upper = null;
                } else if (contentBottom > 0) {
                    upper = item;
                    upperPosition = position;
                    upperContentBottom = contentBottom;
                } else {
                    upper = null;
                }
            }

            return changed;
        }

        /**
         * @return If the offset changed.
         */
        private boolean setOffset(int position, int offset) {
            final boolean changed = offsets.get(position, defaultOffset) != offset;
            offsets.put(position, offset);
            return changed;
        }
    }

    /**
     * Hides the tinted background of a video that plays in the feed. The background fills the space below
     * the video, so the space looked different from the other videos.
     * <p>
     * The background is a component host without views that is added to the item while the video plays.
     * It starts below the thumbnail, behind the content of the item, and ends at the bottom of the item.
     * Hosts are reused by Litho, so a hidden host is shown again when it is removed from the item.
     */
    private static void hidePlaybackBackground(View item) {
        if (!(item instanceof ViewGroup group) || group.getChildCount() < 2) return;

        View content = group.getChildAt(0);
        for (int i = 1, count = group.getChildCount(); i < count; i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE && child instanceof ViewGroup host && host.getChildCount() == 0
                    && child.getWidth() == item.getWidth()
                    && child.getTop() > content.getTop() && child.getTop() < content.getBottom()) {
                child.setVisibility(View.INVISIBLE);                child.addOnAttachStateChangeListener(SHOW_ON_DETACH);
            }
        }
    }

    private static final View.OnAttachStateChangeListener SHOW_ON_DETACH = new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View view) {
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            view.removeOnAttachStateChangeListener(this);
            view.setVisibility(View.VISIBLE);
        }
    };

    /**
     * @return If the item looks like a video of the watch page list: its content starts at the top
     *         of the item, and the space below the video is padding of the item.
     *         The video metadata has no padding, or padding on all sides.
     */
    private static boolean isVideoItem(View item, int contentBottom) {
        if (contentBottom >= item.getHeight()) return false;

        ViewGroup group = (ViewGroup) item;
        for (int i = 0, count = group.getChildCount(); i < count; i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE && child.getHeight() > 0 && child.getTop() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return The bottom of the content of the item relative to the item, or -1 if it is unknown.
     *         The content is the root component host of the item. Its bounds come from the layout,
     *         so they are known even if parts of the item are not mounted, and they include
     *         backgrounds that are drawn without views. The padding of the item is outside of it.
     */
    private static int contentBottom(View item) {
        if (!(item instanceof ViewGroup group)) return -1;

        int bottom = -1;
        for (int i = 0, count = group.getChildCount(); i < count; i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE && child.getHeight() > 0) {
                bottom = Math.max(bottom, child.getBottom());
            }
        }
        return bottom;
    }

    // Method bodies are replaced during patching.

    private static int getChildPosition(ViewGroup list, View item) {
        throw new IllegalStateException();
    }

    private static void invalidateItemDecorations(ViewGroup list) {
        throw new IllegalStateException();
    }
}
