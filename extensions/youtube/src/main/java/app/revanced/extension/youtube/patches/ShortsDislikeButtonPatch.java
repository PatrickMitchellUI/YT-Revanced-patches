package app.revanced.extension.youtube.patches;

import static app.revanced.extension.shared.StringRef.str;

import android.net.Uri;
import android.util.Base64;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.ProtoMessage;

/**
 * Restores the Shorts dislike button, which YouTube no longer sends.
 * <p>
 * The Shorts action bar template still has a slot for a dislike button, so a dislike button model
 * is built from the like button model and added after it. Return YouTube Dislike then shows the dislikes
 * as the text of the button. The heart icons of the like button are replaced with thumbs up icons.
 */
@SuppressWarnings("unused")
public final class ShortsDislikeButtonPatch {
    private static final byte[] OVERLAY_IDENTIFIER = bytes("reel_player_overlay.e");
    /**
     * The identifier is found near the start of the element.
     */
    private static final int IDENTIFIER_SEARCH_LENGTH = 64;
    private static final byte[] LIKE_BUTTON_ID = bytes("id.reel_like_button");
    private static final byte[] DISLIKE_BUTTON_ID = bytes("id.reel_dislike_button");

    /** Wrapper of the action bar entries in 20.40, with the entry in field 1. */
    private static final int ACTION_BAR_BUTTON_FIELD = 1987;
    /** Action bar entries with these fields use the like and dislike button templates. */
    private static final int LIKE_BUTTON_FIELD = 484585818;
    private static final int DISLIKE_BUTTON_FIELD = 484590786;
    /**
     * Key of the like status entity, which sets the toggle state.
     * The like button has it in field 3, the dislike button in field 2.
     */
    private static final int LIKE_BUTTON_ENTITY_KEY_FIELD = 3;
    private static final int DISLIKE_BUTTON_ENTITY_KEY_FIELD = 2;
    /** Toggle button with the untoggled button in field 1 and the toggled button in field 2. */
    private static final int TOGGLE_BUTTON_FIELD = 479644279;
    private static final int BUTTON_FIELD = 461054335;
    private static final int BUTTON_ICON_FIELD = 2;
    private static final int BUTTON_TEXT_FIELD = 3;
    private static final int BUTTON_COMMAND_FIELD = 4;
    private static final int BUTTON_ACCESSIBILITY_TEXT_FIELD = 5;
    private static final int BUTTON_ACCESSIBILITY_ID_FIELD = 17;
    private static final int COMMAND_FIELD = 169495254;
    /** Like endpoint: 1 = status, 2 = target, 6 = like params, 7 = dislike params. */
    private static final int LIKE_ENDPOINT_FIELD = 63158558;
    private static final int LIKE_STATUS_DISLIKE = 1;

    private static final String THUMBS_UP_ICON = "yt_delhi_thumbs_up_not_filled_24dp";
    private static final String THUMBS_UP_FILLED_ICON = "yt_delhi_thumbs_up_filled_24dp";
    private static final String THUMBS_DOWN_ICON = "yt_delhi_thumbs_down_not_filled_24dp";
    private static final String THUMBS_DOWN_FILLED_ICON = "yt_delhi_thumbs_down_filled_24dp";

    private static byte[] bytes(String string) {
        return string.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Injection point.
     */
    public static byte[] onElementLoaded(byte[] bytes) {
        if (!Settings.RYD_ENABLED.get() || !Settings.RYD_SHORTS.get()
                || indexOf(bytes, OVERLAY_IDENTIFIER, Math.min(bytes.length, IDENTIFIER_SEARCH_LENGTH)) < 0
                || indexOf(bytes, LIKE_BUTTON_ID, bytes.length) < 0
                || indexOf(bytes, DISLIKE_BUTTON_ID, bytes.length) >= 0) {
            return bytes;
        }

        try {
            byte[] rewritten = addDislikeButton(bytes, 0);
            if (rewritten != null) {
                Logger.printDebug(() -> "Added Shorts dislike button");
                return rewritten;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to add Shorts dislike button", ex);
        }

        return bytes;
    }

    /**
     * @return The rewritten message, or null if nothing changed.
     */
    @Nullable
    private static byte[] addDislikeButton(byte[] bytes, int depth) {
        if (depth > 32) return null;
        ProtoMessage message = ProtoMessage.parse(bytes);
        if (message == null) return null;

        List<ProtoMessage.Field> fields = message.fields();
        for (int i = 0; i < fields.size(); i++) {
            ProtoMessage.Field field = fields.get(i);
            if (!field.isLengthDelimited()) continue;

            ProtoMessage entry = ProtoMessage.parse(field.value);
            // 20.40 wraps each action bar entry.
            ProtoMessage wrapper = entry == null ? null : entry.getMessage(ACTION_BAR_BUTTON_FIELD);
            ProtoMessage buttonEntry = wrapper == null ? entry : wrapper.getMessage(1);
            ProtoMessage likeButton = buttonEntry == null ? null : buttonEntry.getMessage(LIKE_BUTTON_FIELD);
            if (likeButton != null) {
                ProtoMessage dislikeButton = dislikeButton(likeButton);
                if (dislikeButton == null) return null;

                buttonEntry.setMessage(LIKE_BUTTON_FIELD, thumbsUpIcons(likeButton));
                field.value = actionBarEntry(entry, wrapper, buttonEntry);
                ProtoMessage dislikeEntry = new ProtoMessage().addMessage(DISLIKE_BUTTON_FIELD, dislikeButton);
                fields.add(i + 1, ProtoMessage.Field.ofBytes(field.number,
                        actionBarEntry(entry.copy(), wrapper == null ? null : wrapper.copy(), dislikeEntry)));
                return message.toByteArray();
            }

            byte[] rewritten = addDislikeButton(field.value, depth + 1);
            if (rewritten != null) {
                field.value = rewritten;
                return message.toByteArray();
            }
        }

        return null;
    }

    private static byte[] actionBarEntry(ProtoMessage entry, @Nullable ProtoMessage wrapper, ProtoMessage buttonEntry) {
        if (wrapper == null) return buttonEntry.toByteArray();
        wrapper.setMessage(1, buttonEntry);
        return entry.setMessage(ACTION_BAR_BUTTON_FIELD, wrapper).toByteArray();
    }

    /**
     * @return The like button with thumbs up icons instead of hearts.
     */
    private static ProtoMessage thumbsUpIcons(ProtoMessage likeButton) {
        return editButtons(likeButton, (button, toggled) ->
                button.setBytes(BUTTON_ICON_FIELD, bytes(toggled ? THUMBS_UP_FILLED_ICON : THUMBS_UP_ICON)));
    }

    /**
     * @return A dislike button built from the like button, or null if the like button is not as expected.
     */
    @Nullable
    private static ProtoMessage dislikeButton(ProtoMessage likeButton) {
        byte[] text = str("revanced_ryd_shorts_dislike_button").getBytes(StandardCharsets.UTF_8);
        final boolean[] valid = {true};

        ProtoMessage dislikeButton = editButtons(likeButton, (button, toggled) -> {
            button.setBytes(BUTTON_ICON_FIELD, bytes(toggled ? THUMBS_DOWN_FILLED_ICON : THUMBS_DOWN_ICON));
            button.setBytes(BUTTON_TEXT_FIELD, text);
            button.setBytes(BUTTON_ACCESSIBILITY_TEXT_FIELD, text);
            button.setBytes(BUTTON_ACCESSIBILITY_ID_FIELD,
                    toggled ? bytes("id.reel_dislike_toggled_button") : DISLIKE_BUTTON_ID);
            // The toggled like button removes the like, which also removes a dislike.
            if (!toggled) {
                byte[] command = dislikeCommand(button.getBytes(BUTTON_COMMAND_FIELD));
                if (command == null) {
                    valid[0] = false;
                } else {
                    button.setBytes(BUTTON_COMMAND_FIELD, command);
                }
            }
        });

        byte[] entityKey = dislikeButton.getBytes(LIKE_BUTTON_ENTITY_KEY_FIELD);
        if (entityKey == null) return null;
        dislikeButton.remove(LIKE_BUTTON_ENTITY_KEY_FIELD).addBytes(DISLIKE_BUTTON_ENTITY_KEY_FIELD, entityKey);

        return valid[0] ? dislikeButton : null;
    }

    /**
     * @return The like command changed to a dislike command, or null if it is not a like command.
     */
    @Nullable
    private static byte[] dislikeCommand(@Nullable byte[] likeCommandBytes) {
        ProtoMessage likeCommand = ProtoMessage.parse(likeCommandBytes);
        ProtoMessage command = likeCommand == null ? null : likeCommand.getMessage(COMMAND_FIELD);
        ProtoMessage endpoint = command == null ? null : command.getMessage(LIKE_ENDPOINT_FIELD);
        String likeParams = endpoint == null ? null : endpoint.getString(6);
        if (likeParams == null) return null;

        // Like params: 1 = target, 6 = timestamp. Dislike params: 1 = target, 4 = timestamp.
        ProtoMessage params = ProtoMessage.parse(Base64.decode(Uri.decode(likeParams), Base64.URL_SAFE));
        if (params == null) return null;
        byte[] target = params.getBytes(1);
        byte[] timestamp = params.getBytes(6);
        if (target == null || timestamp == null) return null;
        ProtoMessage dislikeParams = new ProtoMessage()
                .addBytes(1, target)
                .addVarint(2, 0)
                .addBytes(4, timestamp);

        endpoint.setVarint(1, LIKE_STATUS_DISLIKE)
                .remove(6)
                .addString(7, Uri.encode(Base64.encodeToString(dislikeParams.toByteArray(),
                        Base64.URL_SAFE | Base64.NO_WRAP)));
        command.setMessage(LIKE_ENDPOINT_FIELD, endpoint);
        likeCommand.setMessage(COMMAND_FIELD, command);
        return likeCommand.toByteArray();
    }

    private interface ButtonEditor {
        void edit(ProtoMessage button, boolean toggled);
    }

    /**
     * @return A copy of the like or dislike button with the buttons of both toggle states edited.
     */
    private static ProtoMessage editButtons(ProtoMessage likeButton, ButtonEditor editor) {
        ProtoMessage copy = likeButton.copy();
        ProtoMessage toggleEntry = copy.getMessage(1);
        ProtoMessage toggle = toggleEntry == null ? null : toggleEntry.getMessage(TOGGLE_BUTTON_FIELD);
        if (toggle == null) return copy;

        for (int state = 1; state <= 2; state++) {
            ProtoMessage buttonEntry = toggle.getMessage(state);
            ProtoMessage button = buttonEntry == null ? null : buttonEntry.getMessage(BUTTON_FIELD);
            if (button == null) continue;
            editor.edit(button, state == 2);
            buttonEntry.setMessage(BUTTON_FIELD, button);
            toggle.setMessage(state, buttonEntry);
        }
        toggleEntry.setMessage(TOGGLE_BUTTON_FIELD, toggle);
        copy.setMessage(1, toggleEntry);
        return copy;
    }

    private static int indexOf(byte[] array, byte[] pattern, int end) {
        outer:
        for (int i = 0; i <= end - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (array[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
