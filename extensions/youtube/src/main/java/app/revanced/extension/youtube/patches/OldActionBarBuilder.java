package app.revanced.extension.youtube.patches;

import android.icu.text.CompactDecimalFormat;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.ResourceType;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.youtube.shared.ProtoMessage;

/**
 * Builds the elements of the old watch page layout (channel bar and action bar)
 * from the element of the new layout, which combines both.
 * <p>
 * An element is a model (the data) and the name of a template that draws it. The templates of the old
 * elements are bundled in the app, and all data of the old models is also in the new model:
 * buttons, counts, tokens and the channel. Only the shape of the old models is built here.
 */
final class OldActionBarBuilder {

    /**
     * Bundled templates of the old elements. The hash differs per app version.
     */
    @Nullable
    private static final String ACTION_BAR_TEMPLATE = bundledTemplate("video_action_bar.eml-js");
    @Nullable
    private static final String CHANNEL_BAR_TEMPLATE = bundledTemplate("compact_channel_bar.eml-js");

    // Element.
    private static final int ELEMENT_FIELD = 1;
    private static final int ELEMENT = 168777401;
    private static final int ELEMENT_TEMPLATES = 3;
    private static final int ELEMENT_TEMPLATE_NAME = 172035250;
    private static final int ELEMENT_MODEL = 5;
    private static final int ELEMENT_BINDINGS = 6;
    private static final int TEMPLATE_REFERENCE_FIELD = 2;
    private static final int TEMPLATE_REFERENCE = 183314536;
    private static final int TEMPLATE_REFERENCE_INSTANCE_ID = 6;

    // Bindings of the model to entities and resources.
    private static final int BINDINGS_ENTITIES = 188490103;
    private static final int BINDINGS_MODEL = 212323971;
    private static final int BINDINGS_THEME = 386334485;
    private static final int BINDINGS_CAPABILITIES = 386334486;

    // New model.
    private static final int NEW_MODEL = 413471385;
    private static final int NEW_CONFIG = 3;
    private static final int NEW_DOWNLOAD_ENTITY = 21510;
    private static final int NEW_BLOB_1 = 1488;
    private static final int NEW_BLOB_2 = 1374;
    private static final int NEW_PLAYER_STATE_ENTITY = 511119349;
    private static final int NEW_CHANNEL_ENTITY = 520048937;
    private static final int NEW_BAR = 1;
    private static final int NEW_BAR_CONTENT = 2185;
    private static final int NEW_BAR_BUTTON = 1;
    private static final int NEW_BAR_CHANNEL = 6;
    private static final int BUTTON_CONTENT = 10;
    private static final int OVERFLOW = 37098;
    private static final int OVERFLOW_FULL_BUTTON = 3;
    private static final int OVERFLOW_MENU_BUTTON = 2;

    // Button view models.
    private static final int LIKE_BUTTON = 484585818;
    private static final int DISLIKE_BUTTON = 484590786;
    private static final int SEGMENTED_LIKE_DISLIKE_BUTTON = 484586709;
    private static final int BUTTON_VIEW_MODEL = 461054335;
    private static final int SAVE_BUTTON = 484598023;
    private static final int DOWNLOAD_BUTTON = 33561776;
    /**
     * Menu entry with an icon, title and subtitle, used for buttons such as Thanks in the more actions menu.
     */
    private static final int LIST_ITEM = 489973921;
    private static final int LIST_ITEM_INTERACTION = 997;
    /**
     * Menu entry that is only shown in some player states, used for the Premium upsell ("Watch ad-free").
     */
    private static final int CONDITIONAL_MENU_ENTRY = 15469;
    /**
     * Premium upsell text of the download button.
     */
    private static final int DOWNLOAD_BUTTON_UPSELL = 34;
    private static final int TOGGLE_BUTTON = 1;
    private static final int TOGGLE_BUTTON_STATES = 479644279;
    private static final int TOGGLE_BUTTON_STATE_OFF = 1;
    private static final int TOGGLE_BUTTON_STATE_ON = 2;
    private static final int BUTTON_TITLE = 3;
    private static final int BUTTON_ACCESSIBILITY = 5;

    // Old models.
    private static final int ACTION_BAR_MODEL = 389155117;
    private static final int CHANNEL_BAR_MODEL = 391108665;
    private static final int CHANNEL = 1107;
    private static final int CHANNEL_DISPLAY_STYLE = 26;
    private static final int CHANNEL_INFO = 27;
    private static final int CHANNEL_INFO_CONTENT = 477514866;
    private static final int CHANNEL_INFO_STYLE = 5;

    // Like counts entity, the counts are numbers of likes with and without the like of the user.
    private static final int LIKE_COUNTS_LIKED = 12;
    private static final int LIKE_COUNTS_NOT_LIKED = 13;

    // Entity types of the entity keys.
    private static final int LIKE_STATUS_ENTITY = 508;
    private static final int LIKE_COUNT_ENTITY = 445;

    // Video metadata.
    private static final int METADATA_MODEL = 377581260;
    private static final int METADATA_CONTENT = 6;
    private static final int METADATA_DESCRIPTION = 1019;
    private static final int METADATA_LINE = 17;
    private static final int METADATA_LINE_CONTENT = 1017;
    private static final int METADATA_LINE_CHANNEL_HANDLE = 1;
    private static final int METADATA_LINE_LIKES = 27;
    /**
     * Accessibility texts and entity of the likes in the metadata line.
     */
    private static final int[] METADATA_LINE_LIKES_EXTRA = {31, 32, 33, 34};

    private static final String EXPERIMENTAL_ICON = "_experimental";
    private static final int MAX_DEPTH = 40;

    /**
     * Elements of the old layout, in display order.
     */
    /**
     * @return The template reference (name and hash) of a template bundled in the app, or null if not found.
     */
    @Nullable
    private static String bundledTemplate(String name) {
        try {
            String prefix = name + "_";
            String[] files = Utils.getContext().getAssets().list("mainapp_filegroup/_srs_resources_eml_bundle");
            if (files != null) {
                for (String file : files) {
                    if (file.startsWith(prefix)) return name + "|" + file.substring(prefix.length());
                }
            }
            Logger.printException(() -> "Bundled template not found: " + name);
        } catch (Exception ex) {
            Logger.printException(() -> "bundledTemplate failure", ex);
        }
        return null;
    }

    record Elements(byte[] channelBar, byte[] actionBar) {
    }

    /**
     * Like counts of the video metadata, used by the old action bar.
     * The video metadata is added to the list right before the action bar.
     */
    @Nullable
    private static volatile byte[] metadataLikeCounts;

    /**
     * Removes the likes from the metadata line under the video title, as they are shown in the action bar.
     * The likes are kept for the action bar.
     *
     * @param removeChannelHandle If the channel handle is removed too,
     *                            as the old layout shows the channel in the channel bar.
     * @return The metadata element, or null if it has an unexpected structure.
     */
    @Nullable
    static byte[] removeMetadataLikes(byte[] metadataElement, boolean removeChannelHandle) {
        ProtoMessage root = ProtoMessage.parse(metadataElement);
        ProtoMessage element = root == null ? null : root.getMessage(ELEMENT_FIELD, ELEMENT);
        ProtoMessage elementModel = element == null ? null : element.getMessage(ELEMENT_MODEL);
        ProtoMessage model = elementModel == null ? null : elementModel.getMessage(METADATA_MODEL);
        ProtoMessage content = model == null ? null : model.getMessage(METADATA_CONTENT);
        ProtoMessage description = content == null ? null : content.getMessage(METADATA_DESCRIPTION);
        ProtoMessage line = description == null ? null : description.getMessage(METADATA_LINE);
        ProtoMessage lineContent = line == null ? null : line.getMessage(METADATA_LINE_CONTENT);
        if (lineContent == null) return null;

        metadataLikeCounts = lineContent.getBytes(METADATA_LINE_LIKES);
        lineContent.remove(METADATA_LINE_LIKES);
        if (removeChannelHandle) lineContent.remove(METADATA_LINE_CHANNEL_HANDLE);
        for (int field : METADATA_LINE_LIKES_EXTRA) lineContent.remove(field);

        line.setMessage(METADATA_LINE_CONTENT, lineContent);
        description.setMessage(METADATA_LINE, line);
        content.setMessage(METADATA_DESCRIPTION, description);
        model.setMessage(METADATA_CONTENT, content);
        elementModel.setMessage(METADATA_MODEL, model);
        element.setMessage(ELEMENT_MODEL, elementModel);
        return root.setMessage(ELEMENT_FIELD, new ProtoMessage().addMessage(ELEMENT, element)).toByteArray();
    }

    /**
     * @return The old elements, or null if the new element has an unexpected structure.
     */
    @Nullable
    static Elements build(byte[] newElement) {
        if (ACTION_BAR_TEMPLATE == null || CHANNEL_BAR_TEMPLATE == null) return null;

        ProtoMessage root = ProtoMessage.parse(newElement);
        if (root == null) return null;
        ProtoMessage element = root.getMessage(ELEMENT_FIELD, ELEMENT);
        ProtoMessage model = root.getMessage(ELEMENT_FIELD, ELEMENT, ELEMENT_MODEL, NEW_MODEL);
        ProtoMessage bindings = element == null ? null : element.getMessage(ELEMENT_BINDINGS);
        ProtoMessage bar = model == null ? null : model.getMessage(NEW_BAR, NEW_BAR_CONTENT);
        String instanceId = root.getString(TEMPLATE_REFERENCE_FIELD, TEMPLATE_REFERENCE, TEMPLATE_REFERENCE_INSTANCE_ID);
        if (bindings == null || bar == null || instanceId == null) return null;

        String theme = bindings.getString(BINDINGS_THEME, 1, 1, 1);
        String capabilities = bindings.getString(BINDINGS_CAPABILITIES, 1, 1);
        if (theme == null || capabilities == null) return null;

        byte[] actionBar = buildActionBar(model, bar, bindings, theme, capabilities, instanceId);
        byte[] channelBar = buildChannelBar(model, bar, theme, capabilities, previousInstanceId(instanceId));
        if (actionBar == null || channelBar == null) return null;

        return new Elements(channelBar, actionBar);
    }

    /**
     * Entries of the more actions menu of the new action bar that are removed.
     */
    record MenuEntries(boolean thanks, boolean download, boolean report, boolean premiumUpsell) {
        boolean any() {
            return thanks || download || report || premiumUpsell;
        }
    }

    /**
     * Removes entries of the more actions menu of the new action bar.
     *
     * @return The new action bar without the entries, or null if nothing changed.
     */
    @Nullable
    static byte[] removeMenuEntries(byte[] newElement, MenuEntries remove) {
        if (!remove.any()) return null;
        return editNewActionBar(newElement, (model, bar) -> removeMenuEntries(bar, remove));
    }

    /**
     * Gets the like counts of the new action bar for {@link CompactActionBarCounts},
     * and marks the descriptions of the liked and disliked states, as the descriptions are the same in all states.
     *
     * @return The new action bar with the marked descriptions, or null if it has an unexpected structure.
     */
    @Nullable
    static byte[] addCountMarkers(byte[] newElement) {
        return editNewActionBar(newElement, OldActionBarBuilder::addCountMarkers);
    }

    private interface NewActionBarEditor {
        /**
         * @return If the bar changed.
         */
        boolean edit(ProtoMessage model, ProtoMessage bar);
    }

    /**
     * @return The new action bar edited by the editor, or null if nothing changed.
     */
    @Nullable
    private static byte[] editNewActionBar(byte[] newElement, NewActionBarEditor editor) {
        ProtoMessage root = ProtoMessage.parse(newElement);
        ProtoMessage element = root == null ? null : root.getMessage(ELEMENT_FIELD, ELEMENT);
        ProtoMessage elementModel = element == null ? null : element.getMessage(ELEMENT_MODEL);
        ProtoMessage model = elementModel == null ? null : elementModel.getMessage(NEW_MODEL);
        ProtoMessage barContainer = model == null ? null : model.getMessage(NEW_BAR);
        ProtoMessage bar = barContainer == null ? null : barContainer.getMessage(NEW_BAR_CONTENT);
        if (bar == null || !editor.edit(model, bar)) return null;

        barContainer.setMessage(NEW_BAR_CONTENT, bar);
        model.setMessage(NEW_BAR, barContainer);
        elementModel.setMessage(NEW_MODEL, model);
        element.setMessage(ELEMENT_MODEL, elementModel);
        return root.setMessage(ELEMENT_FIELD, new ProtoMessage().addMessage(ELEMENT, element)).toByteArray();
    }

    private static boolean addCountMarkers(ProtoMessage model, ProtoMessage bar) {
        ProtoMessage.Field likeField = null;
        ProtoMessage.Field dislikeField = null;
        for (ProtoMessage.Field barField : bar.fields()) {
            if (barField.number != NEW_BAR_BUTTON || !barField.isLengthDelimited()) continue;
            ProtoMessage item = ProtoMessage.parse(barField.value);
            ProtoMessage button = item == null ? null : item.getMessage(BUTTON_CONTENT);
            if (button == null) continue;

            if (button.has(LIKE_BUTTON)) likeField = barField;
            else if (button.has(DISLIKE_BUTTON)) dislikeField = barField;
        }
        String videoId = videoId(model);
        if (likeField == null || dislikeField == null || videoId == null) return false;

        ProtoMessage likeItem = ProtoMessage.parse(likeField.value);
        ProtoMessage like = likeItem.getMessage(BUTTON_CONTENT);
        ProtoMessage dislikeItem = ProtoMessage.parse(dislikeField.value);
        ProtoMessage dislike = dislikeItem.getMessage(BUTTON_CONTENT);
        String likeDescription = like.getString(LIKE_BUTTON, TOGGLE_BUTTON, TOGGLE_BUTTON_STATES,
                TOGGLE_BUTTON_STATE_OFF, BUTTON_VIEW_MODEL, BUTTON_ACCESSIBILITY);
        String dislikeDescription = dislike.getString(DISLIKE_BUTTON, TOGGLE_BUTTON, TOGGLE_BUTTON_STATES,
                TOGGLE_BUTTON_STATE_OFF, BUTTON_VIEW_MODEL, BUTTON_ACCESSIBILITY);
        if (likeDescription == null || dislikeDescription == null) return false;

        // The description has no like count if the video has no likes or the video owner hides the likes,
        // but the like counts of the video metadata are only missing if the likes are hidden.
        ProtoMessage counts = metadataLikeCounts(videoId);
        if (counts == null) {
            final long likes = parseNumber(likeDescription);
            if (likes >= 0) {
                counts = ProtoMessage.parse(likeCounts(videoId, likes, compactNumber(likes), likeDescription));
            }
        }
        String notLiked = null;
        String liked = null;
        if (counts != null) {
            final long notLikedCount = counts.getVarint(LIKE_COUNTS_NOT_LIKED, 0);
            notLiked = compactNumber(notLikedCount);
            // Videos without likes have no like of the user in the liked count.
            liked = compactNumber(Math.max(counts.getVarint(LIKE_COUNTS_LIKED, 0), notLikedCount + 1));
        }

        likeField.value = likeItem.setBytes(BUTTON_CONTENT, markToggledDescription(like, LIKE_BUTTON)).toByteArray();
        dislikeField.value = dislikeItem.setBytes(BUTTON_CONTENT,
                markToggledDescription(dislike, DISLIKE_BUTTON)).toByteArray();
        CompactActionBarCounts.setCounts(new CompactActionBarCounts.Counts(
                videoId, likeDescription, dislikeDescription, notLiked, liked));
        return true;
    }

    private static boolean removeMenuEntries(ProtoMessage bar, MenuEntries remove) {
        boolean changed = false;
        for (ProtoMessage.Field barField : bar.fields()) {
            if (barField.number != NEW_BAR_BUTTON || !barField.isLengthDelimited()) continue;
            ProtoMessage item = ProtoMessage.parse(barField.value);
            ProtoMessage button = item == null ? null : item.getMessage(BUTTON_CONTENT);
            ProtoMessage overflow = button == null ? null : button.getMessage(OVERFLOW);
            if (overflow == null) continue;

            ProtoMessage sanitized = new ProtoMessage();
            for (ProtoMessage.Field field : overflow.fields()) {
                ProtoMessage entry = field.number == OVERFLOW_MENU_BUTTON ? ProtoMessage.parse(field.value) : null;
                if (entry == null) {
                    sanitized.fields().add(field);
                    continue;
                }

                if (isRemovedMenuEntry(entry, remove)) {
                    changed = true;
                    continue;
                }
                if (remove.premiumUpsell() && entry.has(DOWNLOAD_BUTTON)) {
                    ProtoMessage download = entry.getMessage(DOWNLOAD_BUTTON);
                    if (download != null && download.has(DOWNLOAD_BUTTON_UPSELL)) {
                        entry.setMessage(DOWNLOAD_BUTTON, download.remove(DOWNLOAD_BUTTON_UPSELL));
                        changed = true;
                    }
                }
                sanitized.addMessage(OVERFLOW_MENU_BUTTON, entry);
            }

            button.setMessage(OVERFLOW, sanitized);
            barField.value = item.setMessage(BUTTON_CONTENT, button).toByteArray();
        }
        return changed;
    }

    private static boolean isRemovedMenuEntry(ProtoMessage entry, MenuEntries remove) {
        if (entry.has(CONDITIONAL_MENU_ENTRY)) return remove.premiumUpsell();
        if (entry.has(DOWNLOAD_BUTTON)) return remove.download();
        if (entry.has(LIST_ITEM)) {
            String icon = entry.getString(LIST_ITEM, 4, 1, 3, 2);
            return remove.thanks() && icon != null && icon.contains("dollar_sign_heart");
        }
        if (entry.has(BUTTON_VIEW_MODEL)) {
            String icon = entry.getString(BUTTON_VIEW_MODEL, 2);
            return remove.report() && icon != null && icon.contains("_flag_");
        }
        return false;
    }

    @Nullable
    private static byte[] buildActionBar(ProtoMessage model, ProtoMessage bar, ProtoMessage newBindings,
                                         String theme, String capabilities, String instanceId) {
        ProtoMessage like = null;
        ProtoMessage dislike = null;
        ProtoMessage overflow = null;
        for (ProtoMessage item : bar.getMessages(NEW_BAR_BUTTON)) {
            ProtoMessage button = item.getMessage(BUTTON_CONTENT);
            if (button == null) continue;
            if (button.has(LIKE_BUTTON)) like = button;
            else if (button.has(DISLIKE_BUTTON)) dislike = button;
            else if (button.has(OVERFLOW)) overflow = button.getMessage(OVERFLOW);
        }
        if (like == null || dislike == null || overflow == null) return null;

        String likeAccessibility = like.getString(LIKE_BUTTON, TOGGLE_BUTTON, TOGGLE_BUTTON_STATES,
                TOGGLE_BUTTON_STATE_OFF, BUTTON_VIEW_MODEL, BUTTON_ACCESSIBILITY);
        // The like count is missing if the video owner hides it.
        final long likes = parseNumber(likeAccessibility);
        String videoId = videoId(model);
        if (videoId == null) return null;

        List<ProtoMessage> otherButtons = new ArrayList<>();
        String dislikeTitle = null;
        String likeTitle = null;
        for (ProtoMessage button : overflow.getMessages(OVERFLOW_FULL_BUTTON)) {
            if (button.has(LIKE_BUTTON)) {
                likeTitle = button.getString(LIKE_BUTTON, TOGGLE_BUTTON, TOGGLE_BUTTON_STATES,
                        TOGGLE_BUTTON_STATE_OFF, BUTTON_VIEW_MODEL, BUTTON_TITLE);
            } else if (button.has(DISLIKE_BUTTON)) {
                dislikeTitle = button.getString(DISLIKE_BUTTON, TOGGLE_BUTTON, TOGGLE_BUTTON_STATES,
                        TOGGLE_BUTTON_STATE_OFF, BUTTON_VIEW_MODEL, BUTTON_TITLE);
            } else {
                otherButtons.add(button);
            }
        }
        for (ProtoMessage button : overflow.getMessages(OVERFLOW_MENU_BUTTON)) {
            if (button.has(BUTTON_VIEW_MODEL) || button.has(SAVE_BUTTON) || button.has(DOWNLOAD_BUTTON)) {
                otherButtons.add(button);
            } else if (button.has(LIST_ITEM)) {
                ProtoMessage converted = listItemButton(button.getMessage(LIST_ITEM));
                if (converted != null) otherButtons.add(converted);
            }
        }

        String count = likes < 0 ? null : compactNumber(likes);
        if (count == null && likeTitle == null) return null;
        ProtoMessage segmented = new ProtoMessage()
                .addBytes(1, oldStyle(setToggleTitle(like, LIKE_BUTTON, count == null ? likeTitle : count), 0))
                .addBytes(2, oldStyle(setToggleTitle(dislike, DISLIKE_BUTTON,
                        dislikeTitle == null ? "Dislike" : dislikeTitle), 0))
                .addVarint(5, 1);
        if (count != null) segmented.addBytes(7, likeCounts(videoId, likes, count, likeAccessibility));
        segmented.addMessage(9, new ProtoMessage()
                        .addString(1, "like_count_update_status_key")
                        .addString(2, "like_count_placeholder_values_key")
                        .addString(3, "like_count_update_delay_loop_id")
                        .addFloat(4, 5))
                .addString(10, entityKey(videoId, LIKE_STATUS_ENTITY))
                .addVarint(13, 1);

        ProtoMessage buttons = new ProtoMessage().addMessage(1, new ProtoMessage().addMessage(BUTTON_CONTENT,
                new ProtoMessage().addMessage(SEGMENTED_LIKE_DISLIKE_BUTTON, segmented)));
        for (ProtoMessage button : otherButtons) {
            if (button.has(SAVE_BUTTON)) {
                ProtoMessage save = button.getMessage(SAVE_BUTTON);
                if (save == null) continue;
                button.setMessage(SAVE_BUTTON, save.setVarint(4, 0));
            } else if (button.has(DOWNLOAD_BUTTON)) {
                ProtoMessage download = button.getMessage(DOWNLOAD_BUTTON);
                if (download == null) continue;
                button.setMessage(DOWNLOAD_BUTTON, download.remove(DOWNLOAD_BUTTON_UPSELL).setVarint(14, 4));
            }
            buttons.addMessage(1, new ProtoMessage().addBytes(BUTTON_CONTENT, oldStyle(button.toByteArray(), 0)));
        }

        ProtoMessage oldModel = new ProtoMessage();
        addIfPresent(oldModel, 3, model.getBytes(NEW_CONFIG));
        oldModel.addVarint(8, 4);
        addIfPresent(oldModel, 9, model.getBytes(NEW_DOWNLOAD_ENTITY));
        addIfPresent(oldModel, 11, model.getBytes(NEW_BLOB_1));
        addIfPresent(oldModel, 12, model.getBytes(NEW_BLOB_2));
        oldModel.addMessage(14, new ProtoMessage().addVarint(4, 0).addVarint(6, 0));
        addIfPresent(oldModel, 15, model.getBytes(NEW_PLAYER_STATE_ENTITY));
        oldModel.addVarint(16, 1);
        oldModel.addMessage(17, buttons);

        // Entities are bound to model fields, which have other numbers in the old model.
        ProtoMessage entities = new ProtoMessage();
        ProtoMessage newEntities = newBindings.getMessage(BINDINGS_ENTITIES);
        if (newEntities != null) {
            for (ProtoMessage entity : newEntities.getMessages(1)) {
                final long newField = entity.getVarint(2, -1);
                final int oldField = newField == NEW_DOWNLOAD_ENTITY ? 9
                        : newField == NEW_BLOB_2 ? 12
                        : newField == NEW_PLAYER_STATE_ENTITY ? 15 : -1;
                byte[] key = entity.getBytes(1);
                if (oldField < 0 || key == null) continue;
                entities.addMessage(1, new ProtoMessage().addBytes(1, key)
                        .addVarint(2, oldField).addVarint(3, oldField));
            }
        }
        entities.addVarint(2, ACTION_BAR_MODEL);

        ProtoMessage bindings = new ProtoMessage()
                .addMessage(BINDINGS_ENTITIES, entities)
                .addMessage(BINDINGS_MODEL, new ProtoMessage().addVarint(1, ACTION_BAR_MODEL).addVarint(2, 2))
                .addMessage(BINDINGS_THEME, themeBinding(theme, 1, ACTION_BAR_MODEL))
                .addMessage(BINDINGS_CAPABILITIES, capabilitiesBinding(capabilities, 10, ACTION_BAR_MODEL));

        return element(ACTION_BAR_TEMPLATE, ACTION_BAR_MODEL, oldModel, bindings, instanceId);
    }

    @Nullable
    private static byte[] buildChannelBar(ProtoMessage model, ProtoMessage bar, String theme,
                                          String capabilities, String instanceId) {
        ProtoMessage channelContainer = bar.getMessage(NEW_BAR_CHANNEL);
        ProtoMessage channel = channelContainer == null ? null : channelContainer.getMessage(CHANNEL);
        ProtoMessage info = channel == null ? null : channel.getMessage(CHANNEL_INFO);
        ProtoMessage infoContent = info == null ? null : info.getMessage(CHANNEL_INFO_CONTENT);
        if (infoContent == null) return null;

        // Display variants of the old channel bar, with channel name and subscriber count.
        info.setMessage(CHANNEL_INFO_CONTENT, infoContent.setVarint(CHANNEL_INFO_STYLE, 0));
        channel.setMessage(CHANNEL_INFO, info).setVarint(CHANNEL_DISPLAY_STYLE, 2);
        channelContainer.setMessage(CHANNEL, channel);

        ProtoMessage oldModel = new ProtoMessage();
        addIfPresent(oldModel, 2, model.getBytes(NEW_CONFIG));
        addIfPresent(oldModel, 3, model.getBytes(NEW_CHANNEL_ENTITY));
        oldModel.addMessage(4, new ProtoMessage()
                        .addMessage(4, new ProtoMessage().addFloat(1, 411).addFloat(2, 899))
                        .addVarint(5, 0)
                        .addVarint(6, 0))
                .addMessage(9, channelContainer)
                .addVarint(11, 1);

        ProtoMessage bindings = new ProtoMessage()
                .addMessage(BINDINGS_MODEL, new ProtoMessage().addVarint(1, CHANNEL_BAR_MODEL).addVarint(2, 4))
                .addMessage(BINDINGS_THEME, themeBinding(theme, 6, CHANNEL_BAR_MODEL))
                .addMessage(BINDINGS_CAPABILITIES, capabilitiesBinding(capabilities, 5, CHANNEL_BAR_MODEL));

        return element(CHANNEL_BAR_TEMPLATE, CHANNEL_BAR_MODEL, oldModel, bindings, instanceId);
    }

    private static byte[] element(String template, int modelType, ProtoMessage model,
                                  ProtoMessage bindings, String instanceId) {
        ProtoMessage element = new ProtoMessage()
                .addMessage(ELEMENT_TEMPLATES, new ProtoMessage().addMessage(ELEMENT_TEMPLATE_NAME,
                        new ProtoMessage().addString(1, template)))
                .addMessage(ELEMENT_MODEL, new ProtoMessage().addMessage(modelType, model))
                .addMessage(ELEMENT_BINDINGS, bindings);

        return new ProtoMessage()
                .addMessage(ELEMENT_FIELD, new ProtoMessage().addMessage(ELEMENT, element))
                .addMessage(TEMPLATE_REFERENCE_FIELD, new ProtoMessage().addMessage(TEMPLATE_REFERENCE,
                        new ProtoMessage().addString(1, template).addString(TEMPLATE_REFERENCE_INSTANCE_ID, instanceId)))
                .toByteArray();
    }

    private static ProtoMessage themeBinding(String theme, int slot, int modelType) {
        return new ProtoMessage()
                .addMessage(1, new ProtoMessage()
                        .addMessage(1, new ProtoMessage().addString(1, theme))
                        .addVarint(2, slot))
                .addVarint(2, modelType);
    }

    private static ProtoMessage capabilitiesBinding(String capabilities, int slot, int modelType) {
        return new ProtoMessage()
                .addMessage(1, new ProtoMessage().addString(1, capabilities).addVarint(2, slot))
                .addVarint(2, modelType);
    }

    /**
     * @return The like counts of the video metadata, which have translated texts,
     *         or like counts built from the like button if the metadata has none for this video.
     */
    private static byte[] likeCounts(String videoId, long likes, String count, String accessibility) {
        ProtoMessage metadataCounts = metadataLikeCounts(videoId);
        if (metadataCounts != null) {
            return metadataCounts.toByteArray();
        }

        return new ProtoMessage()
                .addString(1, entityKey(videoId, LIKE_COUNT_ENTITY))
                .addMessage(4, text(count))
                .addMessage(5, text(count))
                .addMessage(6, text(count))
                .addMessage(7, text(fullNumber(likes)))
                .addMessage(8, text(fullNumber(likes - 1)))
                .addMessage(9, text(fullNumber(likes - 1)))
                // TODO: Translations, the old model has English texts here.
                .addMessage(10, text("Likes"))
                .addMessage(11, text(accessibility))
                .addVarint(12, likes)
                .addVarint(13, likes - 1)
                .addVarint(14, likes - 1)
                .addVarint(15, 0)
                .addMessage(17, text(fullNumber(likes) + " likes"))
                .addMessage(18, text(fullNumber(likes - 1) + " likes"))
                .addMessage(23, text(count).addVarint(2, likes))
                .addMessage(24, text(fullNumber(likes)).addVarint(2, likes))
                .toByteArray();
    }

    /**
     * @return The like counts of the video metadata, or null if the metadata has none for this video.
     */
    @Nullable
    private static ProtoMessage metadataLikeCounts(String videoId) {
        ProtoMessage counts = ProtoMessage.parse(metadataLikeCounts);
        return counts != null && entityKey(videoId, LIKE_COUNT_ENTITY).equals(counts.getString(1)) ? counts : null;
    }

    private static ProtoMessage text(String text) {
        return new ProtoMessage().addString(1, text);
    }

    /**
     * @return A button of a menu entry, or null if the entry has no title, icon or command.
     */
    @Nullable
    private static ProtoMessage listItemButton(@Nullable ProtoMessage listItem) {
        if (listItem == null) return null;
        String title = listItem.getString(1, 1);
        String icon = listItem.getString(4, 1, 3, 2);
        byte[] command = listItem.getBytes(LIST_ITEM_INTERACTION, 3, 3);
        if (title == null || icon == null || command == null) return null;

        // Same fields as the other buttons of the old action bar. The style is applied later.
        return new ProtoMessage().addMessage(BUTTON_VIEW_MODEL, new ProtoMessage()
                .addString(2, icon)
                .addString(BUTTON_TITLE, title)
                .addBytes(4, command)
                .addString(BUTTON_ACCESSIBILITY, title)
                .addVarint(7, 9)
                .addVarint(10, 0)
                .addVarint(14, 1));
    }

    /**
     * @return The toggle button with the title in both states.
     */
    private static byte[] setToggleTitle(ProtoMessage button, int buttonType, String title) {
        ProtoMessage toggleButton = button.getMessage(buttonType);
        ProtoMessage toggle = toggleButton == null ? null : toggleButton.getMessage(TOGGLE_BUTTON);
        ProtoMessage states = toggle == null ? null : toggle.getMessage(TOGGLE_BUTTON_STATES);
        if (states == null) return button.toByteArray();

        for (int state : new int[]{TOGGLE_BUTTON_STATE_OFF, TOGGLE_BUTTON_STATE_ON}) {
            ProtoMessage wrapper = states.getMessage(state);
            ProtoMessage viewModel = wrapper == null ? null : wrapper.getMessage(BUTTON_VIEW_MODEL);
            if (viewModel == null) continue;
            states.setMessage(state, wrapper.setMessage(BUTTON_VIEW_MODEL, viewModel.setBytes(BUTTON_TITLE,
                    title.getBytes(StandardCharsets.UTF_8))));
        }
        toggleButton.setMessage(TOGGLE_BUTTON, toggle.setMessage(TOGGLE_BUTTON_STATES, states));
        return button.setMessage(buttonType, toggleButton).toByteArray();
    }

    /**
     * @return The toggle button with {@link CompactActionBarCounts#TOGGLED_MARKER}
     *         added to the description of the toggled state.
     */
    private static byte[] markToggledDescription(ProtoMessage button, int buttonType) {
        ProtoMessage toggleButton = button.getMessage(buttonType);
        ProtoMessage toggle = toggleButton == null ? null : toggleButton.getMessage(TOGGLE_BUTTON);
        ProtoMessage states = toggle == null ? null : toggle.getMessage(TOGGLE_BUTTON_STATES);
        ProtoMessage wrapper = states == null ? null : states.getMessage(TOGGLE_BUTTON_STATE_ON);
        ProtoMessage viewModel = wrapper == null ? null : wrapper.getMessage(BUTTON_VIEW_MODEL);
        String description = viewModel == null ? null : viewModel.getString(BUTTON_ACCESSIBILITY);
        if (description == null) return button.toByteArray();

        states.setMessage(TOGGLE_BUTTON_STATE_ON, wrapper.setMessage(BUTTON_VIEW_MODEL, viewModel.setBytes(
                BUTTON_ACCESSIBILITY, (description + CompactActionBarCounts.TOGGLED_MARKER)
                        .getBytes(StandardCharsets.UTF_8))));
        toggleButton.setMessage(TOGGLE_BUTTON, toggle.setMessage(TOGGLE_BUTTON_STATES, states));
        return button.setMessage(buttonType, toggleButton).toByteArray();
    }

    /**
     * Applies the style of the old buttons to all button view models of a message,
     * and uses the icons of the old buttons.
     */
    private static byte[] oldStyle(byte[] bytes, int depth) {
        ProtoMessage message = ProtoMessage.parse(bytes);
        if (message == null || depth > MAX_DEPTH) return bytes;

        boolean changed = false;
        for (ProtoMessage.Field field : message.fields()) {
            if (!field.isLengthDelimited()) continue;

            String icon = iconName(field.value);
            if (icon != null) {
                String oldIcon = oldIcon(icon);
                if (oldIcon != null) {
                    field.value = oldIcon.getBytes(StandardCharsets.US_ASCII);
                    changed = true;
                }
                continue;
            }

            byte[] value = oldStyle(field.value, depth + 1);
            if (field.number == BUTTON_VIEW_MODEL) {
                ProtoMessage viewModel = ProtoMessage.parse(value);
                if (viewModel != null) {
                    value = viewModel.setVarint(11, 5).setVarint(13, 2).remove(40).toByteArray();
                }
            }
            if (value != field.value) {
                field.value = value;
                changed = true;
            }
        }

        return changed ? message.toByteArray() : bytes;
    }

    @Nullable
    private static String iconName(byte[] value) {
        if (value.length < 4 || value[0] != 'y' || value[1] != 't' || value[2] != '_') return null;
        for (byte b : value) {
            if (b < 0x20 || b > 0x7E) return null;
        }
        return new String(value, StandardCharsets.US_ASCII);
    }

    /**
     * @return The icon of the old buttons, or null if there is none (such as for Hype)
     *         and the experimental icon is kept.
     */
    @Nullable
    private static String oldIcon(String icon) {
        if (!icon.contains(EXPERIMENTAL_ICON)) return null;

        String oldIcon = icon.replace(EXPERIMENTAL_ICON, "");
        return Utils.getResourceIdentifier(ResourceType.DRAWABLE, oldIcon) != 0 ? oldIcon : null;
    }

    @Nullable
    private static String videoId(ProtoMessage model) {
        String token = model.getString(NEW_DOWNLOAD_ENTITY, 1);
        if (token == null) return null;
        ProtoMessage key = ProtoMessage.parse(Base64.decode(Uri.decode(token), Base64.URL_SAFE));
        return key == null ? null : key.getString(2, 1);
    }

    /**
     * Key of an entity in the entity store of the app.
     */
    private static String entityKey(String videoId, int entityType) {
        byte[] key = new ProtoMessage().addString(2, videoId).addVarint(4, entityType).addVarint(5, 1).toByteArray();
        return Base64.encodeToString(key, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    /**
     * @return The number in a text, or -1 if there is none.
     */
    private static long parseNumber(@Nullable String text) {
        if (text == null) return -1;
        StringBuilder digits = new StringBuilder();
        boolean inNumber = false;
        for (int i = 0, length = text.length(); i < length; i++) {
            final char c = text.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
                inNumber = true;
            } else if (inNumber && !(c == '.' || c == ',' || Character.isSpaceChar(c) || c == '’')) {
                break;
            }
        }
        try {
            return digits.length() == 0 ? -1 : Long.parseLong(digits.toString());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static String compactNumber(long number) {
        return CompactDecimalFormat.getInstance(Locale.getDefault(),
                CompactDecimalFormat.CompactStyle.SHORT).format(number);
    }

    private static String fullNumber(long number) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(number);
    }

    private static String previousInstanceId(String instanceId) {
        try {
            return Long.toString(Long.parseLong(instanceId) - 1);
        } catch (NumberFormatException ex) {
            return instanceId + "0";
        }
    }

    private static void addIfPresent(ProtoMessage message, int number, @Nullable byte[] value) {
        if (value != null) message.addBytes(number, value);
    }
}
