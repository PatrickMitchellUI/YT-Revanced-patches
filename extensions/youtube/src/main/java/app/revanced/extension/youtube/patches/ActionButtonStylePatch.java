package app.revanced.extension.youtube.patches;

import static app.revanced.extension.shared.StringRef.str;

import android.util.Pair;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.ResourceType;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class ActionButtonStylePatch {

    public enum ActionButtonStyle {
        /**
         * Action buttons chosen by YouTube.
         */
        DEFAULT,
        /**
         * Action buttons with labels, and like and dislike counts.
         */
        OLD
    }

    /**
     * Available if the action button style is selected.
     */
    public static final class ActionButtonStyleAvailability implements Setting.Availability {
        private final ActionButtonStyle style;
        @Nullable
        private final BooleanSetting parent;

        public ActionButtonStyleAvailability(ActionButtonStyle style) {
            this(style, null);
        }

        /**
         * @param parent Setting that must also be enabled.
         */
        public ActionButtonStyleAvailability(ActionButtonStyle style, @Nullable BooleanSetting parent) {
            this.style = style;
            this.parent = parent;
        }

        @Override
        public boolean isAvailable() {
            return (parent == null || parent.get()) && isStyleSelected();
        }

        private boolean isStyleSelected() {
            return Settings.ACTION_BUTTON_STYLE.get() == style;
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return parent == null
                    ? List.of(Settings.ACTION_BUTTON_STYLE)
                    : List.of(Settings.ACTION_BUTTON_STYLE, parent);
        }

        @Nullable
        @Override
        public String getUnavailableMessage() {
            // The message is a resource of the action buttons patch, which may not be included.
            if (isStyleSelected() || Utils.getResourceIdentifier(ResourceType.STRING,
                    "revanced_action_button_style_unavailable") == 0) {
                return null;
            }
            return str("revanced_action_button_style_unavailable",
                    str("revanced_action_button_style_entry_" + style.ordinal()));
        }
    }

    /**
     * Template of the new watch page element that combines the channel bar and the action bar.
     */
    private static final byte[] NEW_ACTION_BAR_IDENTIFIER = bytes("compactify_video_action_bar.eml");

    /**
     * Template of the video metadata (title, views, date) above the action bar.
     */
    private static final byte[] METADATA_IDENTIFIER = bytes("video_metadata.eml");

    /**
     * Only the end of the element is searched for the template name.
     */
    private static final int NEW_ACTION_BAR_IDENTIFIER_SEARCH_LENGTH = 256;

    /**
     * Template of the old action bar, see {@link OldActionBarBuilder}.
     */
    private static final byte[] ACTION_BAR_IDENTIFIER = bytes("video_action_bar.eml-js|");

    /**
     * Only the start of the element is searched for the identifier.
     */
    private static final int ACTION_BAR_IDENTIFIER_SEARCH_LENGTH = 256;

    /**
     * Buttons of the old action bar that are removed before the action bar is parsed,
     * and the start of their icon name.
     */
    private static final List<Pair<BooleanSetting, byte[]>> ACTION_BAR_BUTTONS = List.of(
            new Pair<>(Settings.HIDE_ASK_BUTTON, bytes("yt_fill_spark_")),
            new Pair<>(Settings.HIDE_CLIP_BUTTON, bytes("yt_outline_scissors")),
            new Pair<>(Settings.HIDE_COMMENTS_BUTTON, bytes("yt_outline_message_bubble_right")),
            new Pair<>(Settings.HIDE_HYPE_BUTTON, bytes("yt_outline_star_shooting")),
            new Pair<>(Settings.HIDE_HYPE_BUTTON, bytes("yt_outline_experimental_hype")),
            new Pair<>(Settings.HIDE_HYPE_BUTTON, bytes("yt_outline_hype")),
            new Pair<>(Settings.HIDE_PROMOTE_BUTTON, bytes("yt_outline_megaphone")),
            new Pair<>(Settings.HIDE_REMIX_BUTTON, bytes("yt_outline_youtube_shorts_plus")),
            new Pair<>(Settings.HIDE_REPORT_BUTTON, bytes("yt_outline_flag")),
            new Pair<>(Settings.HIDE_SHARE_BUTTON, bytes("yt_outline_share")),
            new Pair<>(Settings.HIDE_SHOP_BUTTON, bytes("yt_outline_bag")),
            new Pair<>(Settings.HIDE_STOP_ADS_BUTTON, bytes("yt_outline_slash_circle_left")),
            new Pair<>(Settings.HIDE_THANKS_BUTTON, bytes("yt_outline_dollar_sign_heart"))
    );

    /**
     * Field of an action bar button that holds the button view model.
     */
    private static final int BUTTON_FIELD = 10;

    /**
     * Field of the button view model.
     */
    private static final int BUTTON_VIEW_MODEL_FIELD = 461054335;

    /**
     * Maximum number of buttons removed per icon, in case of unexpected data.
     */
    private static final int MAX_REMOVED_BUTTONS = 5;

    /**
     * A length delimited protobuf field.
     */
    private record Field(int start, int number, int contentStart, int contentEnd) {
    }

    /**
     * Injection point.
     * <p>
     * Called before a watch page item with a serialized element is added to the item list.
     * The new element that combines the channel bar and the action bar is replaced
     * by the old channel bar and action bar elements, built from its data.
     *
     * @param item The item with the element.
     * @param list Collects the items of the list.
     */
    public static void addOldActionBar(Object item, Object list) {
        try {
            byte[] element = getElementBytes(item);
            if (element == null) return;
            final int templateSearchStart = Math.max(0, element.length - NEW_ACTION_BAR_IDENTIFIER_SEARCH_LENGTH);

            if (Settings.ACTION_BUTTON_STYLE.get() != ActionButtonStyle.OLD) {
                if (indexOf(element, NEW_ACTION_BAR_IDENTIFIER, templateSearchStart, element.length) < 0) return;

                byte[] sanitized = OldActionBarBuilder.removeMenuEntries(element, new OldActionBarBuilder.MenuEntries(
                        Settings.HIDE_COMPACT_THANKS_BUTTON.get(),
                        Settings.HIDE_COMPACT_DOWNLOAD_BUTTON.get(),
                        Settings.HIDE_COMPACT_REPORT_BUTTON.get(),
                        Settings.HIDE_YOUTUBE_PREMIUM_PROMOTIONS.get()));
                if (sanitized != null) setElementBytes(item, sanitized);
                return;
            }

            if (indexOf(element, METADATA_IDENTIFIER, templateSearchStart, element.length) >= 0) {
                byte[] metadata = OldActionBarBuilder.buildOldMetadata(element);
                if (metadata != null) setElementBytes(item, metadata);
                return;
            }

            if (indexOf(element, NEW_ACTION_BAR_IDENTIFIER, templateSearchStart, element.length) < 0) return;

            OldActionBarBuilder.Elements elements = OldActionBarBuilder.build(element);
            if (elements == null) {
                Logger.printDebug(() -> "Unexpected action bar element, keeping the new action bar");
                return;
            }

            Object channelBar = copyItem(item);
            setElementBytes(channelBar, elements.channelBar());
            setElementBytes(item, removeActionBarButtons(elements.actionBar()));
            // The original item is added after this, so the channel bar is above the action bar.
            addToList(list, channelBar);
        } catch (Exception ex) {
            Logger.printException(() -> "addOldActionBar failure", ex);
        }
    }

    /**
     * Removes buttons from the serialized old action bar element,
     * as these buttons cannot be identified by a litho filter.
     */
    private static byte[] removeActionBarButtons(byte[] element) {
        try {
            if (element == null || indexOf(element, ACTION_BAR_IDENTIFIER, 0,
                    Math.min(element.length, ACTION_BAR_IDENTIFIER_SEARCH_LENGTH)) < 0) {
                return element;
            }

            for (Pair<BooleanSetting, byte[]> button : ACTION_BAR_BUTTONS) {
                if (button.first.get()) element = removeButtons(element, button.second);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "removeActionBarButtons failure", ex);
        }

        return element;
    }

    private static byte[] removeButtons(byte[] element, byte[] icon) {
        int searchStart = 0;

        for (int i = 0; i < MAX_REMOVED_BUTTONS; i++) {
            final int iconIndex = indexOf(element, icon, searchStart, element.length);
            if (iconIndex < 0) break;

            byte[] removed = removeButton(element, iconIndex);
            if (removed == null) {
                // The icon is used by something other than an action bar button.
                searchStart = iconIndex + icon.length;
                continue;
            }

            Logger.printDebug(() -> "Removed action bar button with icon: "
                    + new String(icon, StandardCharsets.US_ASCII));
            element = removed;
        }

        return element;
    }

    /**
     * Removes the button containing the icon name at {@code iconIndex}.
     *
     * @return The element without the button, or null if the structure is unexpected.
     */
    private static byte[] removeButton(byte[] data, int iconIndex) {
        List<Field> chain = new ArrayList<>();
        if (!findFieldChain(data, 0, data.length, iconIndex, chain)) return null;

        // Button list -> button -> button field -> view model -> icon name.
        final int buttonIndex = chain.size() - 4;
        if (buttonIndex < 1
                || chain.get(buttonIndex + 1).number != BUTTON_FIELD
                || chain.get(buttonIndex + 2).number != BUTTON_VIEW_MODEL_FIELD) {
            return null;
        }

        Field button = chain.get(buttonIndex);
        Field list = chain.get(buttonIndex - 1);
        ByteArrayOutputStream content = new ByteArrayOutputStream(list.contentEnd - list.contentStart);
        content.write(data, list.contentStart, button.start - list.contentStart);
        content.write(data, button.contentEnd, list.contentEnd - button.contentEnd);
        byte[] rewritten = content.toByteArray();

        // Update the lengths of all enclosing fields.
        for (int i = buttonIndex - 1; i >= 0; i--) {
            Field field = chain.get(i);
            final int parentStart = i == 0 ? 0 : chain.get(i - 1).contentStart;
            final int parentEnd = i == 0 ? data.length : chain.get(i - 1).contentEnd;

            ByteArrayOutputStream parent = new ByteArrayOutputStream(parentEnd - parentStart);
            parent.write(data, parentStart, field.start - parentStart);
            writeVarint(parent, ((long) field.number << 3) | 2);
            writeVarint(parent, rewritten.length);
            parent.write(rewritten, 0, rewritten.length);
            parent.write(data, field.contentEnd, parentEnd - field.contentEnd);
            rewritten = parent.toByteArray();
        }

        return rewritten;
    }

    /**
     * Finds the nested length delimited fields that contain {@code index}.
     *
     * @return If the bytes are a message with a field containing the index.
     */
    private static boolean findFieldChain(byte[] data, int start, int end, int index, List<Field> chain) {
        int position = start;

        while (position < end) {
            final int fieldStart = position;
            final long tag = readVarint(data, position, end);
            if (tag < 0) return false;
            position += varintSize(tag);

            switch ((int) (tag & 0x7)) {
                case 0 -> {
                    final long value = readVarint(data, position, end);
                    if (value < 0) return false;
                    position += varintSize(value);
                }
                case 1 -> position += 8;
                case 5 -> position += 4;
                case 2 -> {
                    final long length = readVarint(data, position, end);
                    if (length < 0) return false;
                    position += varintSize(length);

                    final int contentStart = position;
                    final long contentEnd = contentStart + length;
                    if (contentEnd > end) return false;
                    position = (int) contentEnd;

                    if (index < contentStart || index >= contentEnd) continue;

                    chain.add(new Field(fieldStart, (int) (tag >>> 3), contentStart, (int) contentEnd));
                    // A string is not a message, and ends the chain.
                    findFieldChain(data, contentStart, (int) contentEnd, index, chain);
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }

        return false;
    }

    private static long readVarint(byte[] data, int position, int end) {
        long result = 0;
        for (int shift = 0; shift < 64 && position < end; shift += 7) {
            final byte b = data[position++];
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
        }
        return -1;
    }

    private static int varintSize(long value) {
        int size = 1;
        while ((value >>>= 7) != 0) size++;
        return size;
    }

    private static void writeVarint(ByteArrayOutputStream out, long value) {
        while ((value & ~0x7FL) != 0) {
            out.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        out.write((int) value);
    }

    private static int indexOf(byte[] data, byte[] pattern, int start, int end) {
        final byte first = pattern[0];
        final int last = end - pattern.length;

        outer:
        for (int i = start; i <= last; i++) {
            if (data[i] != first) continue;
            for (int j = 1; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static byte[] bytes(String string) {
        return string.getBytes(StandardCharsets.US_ASCII);
    }

    // Method bodies are replaced during patching.

    private static byte[] getElementBytes(Object item) {
        throw new IllegalStateException();
    }

    private static void setElementBytes(Object item, byte[] element) {
        throw new IllegalStateException();
    }

    private static Object copyItem(Object item) {
        throw new IllegalStateException();
    }

    private static void addToList(Object list, Object item) {
        throw new IllegalStateException();
    }
}
