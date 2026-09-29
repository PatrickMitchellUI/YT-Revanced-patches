package app.revanced.extension.youtube.patches;

import static app.revanced.extension.shared.StringRef.str;

import android.net.Uri;
import android.util.Base64;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.PlayerType;
import app.revanced.extension.youtube.shared.ProtoMessage;
import kotlin.Unit;

/**
 * Non-Premium replacement for the "Play next in queue" and "Play last in queue" menu items.
 * <p>
 * YouTube hands these items to its cross-device queue, which the server only allows for Premium.
 * Instead, the queue is kept here and played as a temporary playlist (the same kind of list the
 * {@code watch_videos?video_ids=} URL creates). A video that is already playing gets the playlist
 * attached with YouTube's reload watch next command, which updates the watch page but keeps the player running.
 * <p>
 * Menus of feed videos offer both items with a queue command. Menus of videos on the watch page
 * (and on 20.x also feed menus) only offer "Play next in queue" with a Premium badge and an upsell command.
 * Those menus are rewritten to a regular item plus "Play last in queue". The same badged item in playlist panel menus is hidden,
 * as it would replace the playlist that is playing.
 */
@SuppressWarnings("unused")
public final class PlaybackQueuePatch {

    /** Command extension of the queue menu items. */
    private static final int QUEUE_COMMAND_FIELD = 1610;
    /** Command extension that reloads the watch next response of the current video. */
    private static final int RELOAD_WATCH_NEXT_COMMAND_FIELD = 1908;
    /** Command extension of a watch endpoint. */
    private static final int WATCH_ENDPOINT_FIELD = 48687757;
    /** Command extension of a browse endpoint, which the Premium upsell uses. */
    private static final int BROWSE_ENDPOINT_FIELD = 48687626;
    /** Command extension that opens a menu. */
    private static final int SHOW_MENU_COMMAND_FIELD = 98150882;
    /** Commands of the hidden queue items, which only work while casting. Field 1 is the video id. */
    private static final int[] INSERT_IN_REMOTE_QUEUE_COMMAND_FIELDS = {122330696, 76177954};
    /**
     * 20.x command of the hidden queue items. 1 = queue command while casting, 2 = watch command otherwise.
     */
    private static final int CAST_DEPENDENT_QUEUE_COMMAND_FIELD = 469942096;

    /** Menu item that runs a command. 1 = text, 2 = icon, 3 = command, 12 = badge. */
    private static final int MENU_SERVICE_ITEM_RENDERER_FIELD = 77258115;
    private static final int MENU_ITEM_TEXT_FIELD = 1;
    private static final int MENU_ITEM_ICON_FIELD = 2;
    private static final int MENU_ITEM_COMMAND_FIELD = 3;
    private static final int MENU_ITEM_BADGE_FIELD = 12;
    /** Menu item list entry. */
    private static final int MENU_ITEMS_FIELD = 1;

    private static final int ICON_QUEUE_PLAY_NEXT = 251;
    private static final int ICON_QUEUE_PLAY_LAST = 895;
    private static final int ICON_ADD_TO_QUEUE_TAIL = 781;

    private static final int QUEUE_OPERATION_PLAY_NEXT = 2;
    private static final int QUEUE_OPERATION_PLAY_LAST = 3;

    /**
     * Videos queued after the current video, in play order.
     */
    private static final List<String> queue = new ArrayList<>();

    /**
     * Video the queue was attached to, or the queued video that plays now.
     */
    @Nullable
    private static String currentVideoId;

    /**
     * Command routers that ran a command. Only some of them resolve the reload watch next command.
     */
    private static final Set<Object> routers = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * All command routers in creation order, used if no router that ran a command resolves the queue command.
     */
    private static final List<WeakReference<Object>> createdRouters = new ArrayList<>();

    static {
        PlayerType.getOnChange().addObserver(type -> {
            if (type == PlayerType.NONE) {
                synchronized (queue) {
                    if (!queue.isEmpty()) {
                        Logger.printDebug(() -> "Player closed, clearing queue: " + queue);
                        queue.clear();
                    }
                    currentVideoId = null;
                }
            }
            return Unit.INSTANCE;
        });
    }

    /**
     * Injection point.
     */
    public static void routerCreated(Object router) {
        synchronized (routers) {
            createdRouters.removeIf(reference -> reference.get() == null);
            createdRouters.add(new WeakReference<>(router));
        }
    }

    /**
     * Injection point.
     *
     * @return The command to run, which can be a rewritten copy,
     *         or null if the command was handled and YouTube must not run it.
     */
    @Nullable
    public static Object handleCommand(Object router, Object command) {
        try {
            synchronized (routers) {
                routers.add(router);
            }

            ProtoMessage message = ProtoMessage.parse(serializeMessage(command));
            if (message == null) return command;

            if (message.has(SHOW_MENU_COMMAND_FIELD)) {
                byte[] rewritten = rewriteMenu(message);
                return rewritten == null ? command : parseCommand(rewritten);
            }

            QueueOperation operation = QueueOperation.fromCommand(message);
            if (operation == null) return command;

            Logger.printDebug(() -> "Queue operation: " + operation);
            addToQueue(operation.videoId, operation.playNext);
            return null;
        } catch (Exception ex) {
            Logger.printException(() -> "handleCommand failure", ex);
            return command;
        }
    }

    /**
     * Injection point.
     *
     * @param menuItem Menu item renderer wrapper.
     * @return If the menu item must not be shown.
     */
    public static boolean hideMenuItem(Object menuItem) {
        try {
            ProtoMessage wrapper = ProtoMessage.parse(serializeMessage(menuItem));
            ProtoMessage item = wrapper == null ? null : wrapper.getMessage(MENU_SERVICE_ITEM_RENDERER_FIELD);
            if (item != null && isUpsellQueueItem(item)) {
                Logger.printDebug(() -> "Hiding Premium queue menu item");
                return true;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "hideMenuItem failure", ex);
        }
        return false;
    }

    /**
     * Injection point.
     */
    public static void newVideoLoaded(String videoId) {
        synchronized (queue) {
            if (queue.isEmpty()) return;

            final int index = queue.indexOf(videoId);
            if (index >= 0) {
                // Next queued video, or a later one picked from the playlist panel.
                queue.subList(0, index + 1).clear();
                Logger.printDebug(() -> "Playing queued video: " + videoId + " remaining: " + queue);
            } else if (!videoId.equals(currentVideoId)) {
                // Something else was opened, which replaces the playlist.
                Logger.printDebug(() -> "Queue cleared, opened: " + videoId);
                queue.clear();
            }
            currentVideoId = videoId;
        }
    }

    private record QueueOperation(String videoId, boolean playNext) {
        /**
         * @return The operation of a queue command, or null if the command is something else.
         */
        @Nullable
        static QueueOperation fromCommand(ProtoMessage command) {
            // Queue command: 1 = { 3 = url encoded base64 params }.
            String paramsString = command.getString(QUEUE_COMMAND_FIELD, 1, 3);
            if (paramsString == null) return null;

            // Params: 3 = operation { 1 = operation type, 2 = { 1 = { 1 = video id } } }.
            ProtoMessage params = ProtoMessage.parse(Base64.decode(Uri.decode(paramsString), Base64.URL_SAFE));
            ProtoMessage operation = params == null ? null : params.getMessage(3);
            if (operation == null) return null;

            final long type = operation.getVarint(1, -1);
            if (type != QUEUE_OPERATION_PLAY_NEXT && type != QUEUE_OPERATION_PLAY_LAST) return null;

            String videoId = operation.getString(2, 1, 1);
            if (videoId == null) return null;

            return new QueueOperation(videoId, type == QUEUE_OPERATION_PLAY_NEXT);
        }

        ProtoMessage toCommand() {
            ProtoMessage operation = new ProtoMessage()
                    .addVarint(1, playNext ? QUEUE_OPERATION_PLAY_NEXT : QUEUE_OPERATION_PLAY_LAST)
                    .addMessage(2, new ProtoMessage().addMessage(1, new ProtoMessage().addString(1, videoId)));
            byte[] params = new ProtoMessage().addMessage(3, operation).toByteArray();
            String paramsString = Uri.encode(Base64.encodeToString(params, Base64.URL_SAFE | Base64.NO_WRAP));

            return new ProtoMessage().addMessage(QUEUE_COMMAND_FIELD,
                    new ProtoMessage().addMessage(1, new ProtoMessage().addString(3, paramsString)));
        }
    }

    // region Menu

    /**
     * @return The rewritten show menu command, or null if nothing changed.
     */
    @Nullable
    private static byte[] rewriteMenu(ProtoMessage showMenuCommand) {
        byte[] menuBytes = showMenuCommand.getBytes(SHOW_MENU_COMMAND_FIELD);
        if (menuBytes == null) return null;
        rememberPlayLastLabel(menuBytes);

        byte[] rewrittenMenu = rewriteMenuItems(menuBytes, menuBytes, 0);
        if (rewrittenMenu == null) return null;

        showMenuCommand.setBytes(SHOW_MENU_COMMAND_FIELD, rewrittenMenu);
        return showMenuCommand.toByteArray();
    }

    /**
     * Replaces a badged upsell queue item with a queue item and adds "Play last in queue" after it.
     *
     * @return The rewritten message, or null if nothing changed.
     */
    @Nullable
    private static byte[] rewriteMenuItems(byte[] menu, byte[] bytes, int depth) {
        if (depth > 32) return null;
        ProtoMessage message = ProtoMessage.parse(bytes);
        if (message == null) return null;

        boolean changed = false;
        List<ProtoMessage.Field> fields = message.fields();
        for (int i = 0; i < fields.size(); i++) {
            ProtoMessage.Field field = fields.get(i);
            if (!field.isLengthDelimited()) continue;

            ProtoMessage entry = field.number == MENU_ITEMS_FIELD ? ProtoMessage.parse(field.value) : null;
            ProtoMessage item = entry == null ? null : entry.getMessage(MENU_SERVICE_ITEM_RENDERER_FIELD);
            if (item != null) {
                if (!isUpsellQueueItem(item)) continue;

                String videoId = findMenuVideoId(menu);
                if (videoId == null) {
                    Logger.printDebug(() -> "Premium queue item without a video id");
                    return null;
                }
                final boolean playNext = iconType(item) != ICON_QUEUE_PLAY_LAST;
                field.value = menuItemEntry(item, videoId, playNext, null);
                if (playNext && !hasQueueItem(menu, ICON_QUEUE_PLAY_LAST)) {
                    fields.add(i + 1, ProtoMessage.Field.ofBytes(field.number,
                            menuItemEntry(item, videoId, false, playLastLabel(menu))));
                    i++;
                }
                Logger.printDebug(() -> "Rewrote Premium queue menu item for: " + videoId);
                changed = true;
            } else {
                byte[] rewritten = rewriteMenuItems(menu, field.value, depth + 1);
                if (rewritten != null) {
                    field.value = rewritten;
                    changed = true;
                }
            }
        }

        return changed ? message.toByteArray() : null;
    }

    /**
     * @return A menu item list entry with a queue item that works without Premium.
     */
    private static byte[] menuItemEntry(ProtoMessage template, String videoId,
                                        boolean playNext, @Nullable String text) {
        ProtoMessage item = template.copy()
                .remove(MENU_ITEM_BADGE_FIELD)
                .setMessage(MENU_ITEM_COMMAND_FIELD, new QueueOperation(videoId, playNext).toCommand());
        if (text != null) {
            item.setMessage(MENU_ITEM_TEXT_FIELD,
                    new ProtoMessage().addMessage(1, new ProtoMessage().addString(1, text)));
            item.setMessage(MENU_ITEM_ICON_FIELD, new ProtoMessage().addVarint(1, ICON_QUEUE_PLAY_LAST));
        }
        return new ProtoMessage().addMessage(MENU_SERVICE_ITEM_RENDERER_FIELD, item).toByteArray();
    }

    private static long iconType(ProtoMessage item) {
        ProtoMessage icon = item.getMessage(MENU_ITEM_ICON_FIELD);
        return icon == null ? -1 : icon.getVarint(1, -1);
    }

    /**
     * @return If the item is a queue item that YouTube replaced with a Premium upsell.
     */
    private static boolean isUpsellQueueItem(ProtoMessage item) {
        final long icon = iconType(item);
        if (icon != ICON_QUEUE_PLAY_NEXT && icon != ICON_QUEUE_PLAY_LAST) return false;

        ProtoMessage command = item.getMessage(MENU_ITEM_COMMAND_FIELD);
        return command != null && command.has(BROWSE_ENDPOINT_FIELD);
    }

    private static boolean hasQueueItem(byte[] menu, int icon) {
        for (ProtoMessage entry : ProtoMessage.findMessagesWithField(menu, MENU_SERVICE_ITEM_RENDERER_FIELD)) {
            ProtoMessage item = entry.getMessage(MENU_SERVICE_ITEM_RENDERER_FIELD);
            if (item != null && iconType(item) == icon) return true;
        }
        return false;
    }

    /**
     * @return The video id of the menu, taken from its hidden queue items.
     */
    @Nullable
    private static String findMenuVideoId(byte[] menu) {
        for (int field : INSERT_IN_REMOTE_QUEUE_COMMAND_FIELDS) {
            for (ProtoMessage command : ProtoMessage.findMessagesWithField(menu, field)) {
                String videoId = command.getString(field, 1);
                if (videoId != null) return videoId;
            }
        }

        for (ProtoMessage command : ProtoMessage.findMessagesWithField(menu, CAST_DEPENDENT_QUEUE_COMMAND_FIELD)) {
            byte[] bytes = command.getBytes(CAST_DEPENDENT_QUEUE_COMMAND_FIELD);
            if (bytes == null) continue;

            for (ProtoMessage watch : ProtoMessage.findMessagesWithField(bytes, WATCH_ENDPOINT_FIELD)) {
                String videoId = watch.getString(WATCH_ENDPOINT_FIELD, 1);
                if (videoId != null) return videoId;
            }
        }
        return null;
    }

    /**
     * Saves the localized "Play last in queue" text of feed menus,
     * which is reused for the item added to watch page menus.
     */
    private static void rememberPlayLastLabel(byte[] menu) {
        for (ProtoMessage entry : ProtoMessage.findMessagesWithField(menu, MENU_SERVICE_ITEM_RENDERER_FIELD)) {
            ProtoMessage item = entry.getMessage(MENU_SERVICE_ITEM_RENDERER_FIELD);
            if (item == null) continue;

            ProtoMessage command = item.getMessage(MENU_ITEM_COMMAND_FIELD);
            QueueOperation operation = command == null ? null : QueueOperation.fromCommand(command);
            if (operation == null || operation.playNext) continue;

            String text = menuItemText(item);
            if (text != null && !text.equals(Settings.PLAYBACK_QUEUE_PLAY_LAST_LABEL.get())) {
                Logger.printDebug(() -> "Saving play last label: " + text);
                Settings.PLAYBACK_QUEUE_PLAY_LAST_LABEL.save(text);
            }
            return;
        }
    }

    /**
     * @return The saved feed menu text, the text of the hidden "Add to queue" item of the menu,
     *         or the ReVanced string.
     */
    private static String playLastLabel(byte[] menu) {
        String saved = Settings.PLAYBACK_QUEUE_PLAY_LAST_LABEL.get();
        if (!saved.isEmpty()) return saved;

        for (ProtoMessage entry : ProtoMessage.findMessagesWithField(menu, MENU_SERVICE_ITEM_RENDERER_FIELD)) {
            ProtoMessage item = entry.getMessage(MENU_SERVICE_ITEM_RENDERER_FIELD);
            if (item != null && iconType(item) == ICON_ADD_TO_QUEUE_TAIL) {
                String text = menuItemText(item);
                if (text != null) return text;
            }
        }

        return str("revanced_playback_queue_play_last");
    }

    /**
     * @return The text of a menu item, joined from its formatted string runs.
     */
    @Nullable
    private static String menuItemText(ProtoMessage item) {
        ProtoMessage text = item.getMessage(MENU_ITEM_TEXT_FIELD);
        if (text == null) return null;

        StringBuilder builder = new StringBuilder();
        for (ProtoMessage.Field run : text.fields()) {
            if (run.number != 1 || !run.isLengthDelimited()) continue;
            ProtoMessage runMessage = ProtoMessage.parse(run.value);
            String runText = runMessage == null ? null : runMessage.getString(1);
            if (runText != null) builder.append(runText);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    // endregion

    // region Queue

    private static void addToQueue(String videoId, boolean playNext) {
        final String playingVideoId;
        final List<String> playlist = new ArrayList<>();

        synchronized (queue) {
            PlayerType playerType = PlayerType.getCurrent();
            if (playerType.isNoneOrHidden() || playerType == PlayerType.INLINE_MINIMAL) {
                // Nothing, a Short or a muted feed preview is playing. Start the queued video.
                queue.clear();
                currentVideoId = null;
                playingVideoId = null;
            } else {
                playingVideoId = VideoInformation.getVideoId();
                if (videoId.equals(playingVideoId)) {
                    Logger.printDebug(() -> "Ignoring queue request for the playing video");
                    return;
                }
                if (!playingVideoId.equals(currentVideoId)) {
                    // The queue belongs to a video that is no longer playing.
                    queue.clear();
                }
                currentVideoId = playingVideoId;
                queue.remove(videoId);
                if (playNext) {
                    queue.add(0, videoId);
                } else {
                    queue.add(videoId);
                }
                playlist.add(playingVideoId);
                playlist.addAll(queue);
            }
        }

        final ProtoMessage command;
        if (playingVideoId == null) {
            command = watchCommand(videoId, Collections.singletonList(videoId));
        } else {
            command = new ProtoMessage().addMessage(RELOAD_WATCH_NEXT_COMMAND_FIELD,
                    new ProtoMessage().addMessage(2, watchCommand(playingVideoId, playlist)));
        }

        Utils.runOnMainThread(() -> {
            try {
                Object parsed = parseCommand(command.toByteArray());
                Object router = null;
                synchronized (routers) {
                    for (Object candidate : routers) {
                        if (canResolveCommand(candidate, parsed)) {
                            router = candidate;
                            break;
                        }
                    }
                    // Newest first, as older routers can belong to pages that are gone.
                    for (int i = createdRouters.size() - 1; router == null && i >= 0; i--) {
                        Object candidate = createdRouters.get(i).get();
                        if (candidate != null && canResolveCommand(candidate, parsed)) {
                            router = candidate;
                        }
                    }
                }
                if (router == null) {
                    Logger.printException(() -> "No command router resolves the queue command");
                    return;
                }

                runCommand(router, parsed);
                Utils.showToastShort(str(playNext
                        ? "revanced_playback_queue_toast_play_next"
                        : "revanced_playback_queue_toast_play_last"));
            } catch (Exception ex) {
                Logger.printException(() -> "addToQueue failure", ex);
            }
        });
    }

    /**
     * @return A command with a watch endpoint for the video and a temporary playlist of the video ids.
     */
    private static ProtoMessage watchCommand(String videoId, List<String> playlist) {
        ProtoMessage videoIds = new ProtoMessage();
        for (String id : playlist) {
            videoIds.addString(5, id);
        }
        // Without a title, the playlist is called "Untitled list".
        videoIds.addString(6, str("revanced_playback_queue_title"));
        String params = Base64.encodeToString(videoIds.toByteArray(), Base64.NO_WRAP);

        ProtoMessage watchEndpoint = new ProtoMessage()
                .addString(1, videoId)
                .addString(5, params);
        return new ProtoMessage().addMessage(WATCH_ENDPOINT_FIELD, watchEndpoint);
    }

    // endregion

    // Method bodies are replaced during patching.

    /**
     * @param message Any protobuf message.
     */
    private static byte[] serializeMessage(Object message) {
        throw new IllegalStateException();
    }

    private static Object parseCommand(byte[] bytes) {
        throw new IllegalStateException();
    }

    private static boolean canResolveCommand(Object router, Object command) {
        throw new IllegalStateException();
    }

    private static void runCommand(Object router, Object command) {
        throw new IllegalStateException();
    }
}
