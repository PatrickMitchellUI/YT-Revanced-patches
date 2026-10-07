package app.revanced.extension.youtube.patches;

import app.revanced.com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.util.List;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.innertube.NextResponseOuterClass.Model;
import app.revanced.extension.shared.innertube.NextResponseOuterClass.NewElement;
import app.revanced.extension.shared.innertube.NextResponseOuterClass.ShareSheetData;
import app.revanced.extension.shared.innertube.NextResponseOuterClass.ShareTargetList;
import app.revanced.extension.youtube.settings.Settings;

/**
 * Hides buttons of the share sheet, which is opened by share buttons of the player, community posts and feed menus.
 * <p>
 * The buttons are rendered from the model of the share sheet element.
 * Hiding the rendered components keeps their space, so the buttons are removed from the model instead.
 */
@SuppressWarnings("unused")
public final class ShareSheetPatch {
    private static final byte[] SHARE_SHEET_IDENTIFIER = bytes("share_sheet_container.e");
    /**
     * The identifier is found near the start of the element.
     */
    private static final int IDENTIFIER_SEARCH_LENGTH = 64;

    /**
     * Each share target has an icon from "https://www.gstatic.com/youtube/img/messaging/192x192/[name]_[theme].png".
     * The icon name is the same in every app language.
     */
    private static final byte[] ICON_PATH = bytes("/192x192/");
    private static final byte[] QUICK_SHARE_ICON = bytes("quick_share_");
    private static final byte[] REMIX_ICON = bytes("remix_");
    /**
     * Icon of Create post and Repost on YouTube.
     */
    private static final byte[] COMMUNITY_POST_ICON = bytes("community_post_");
    private static final byte[] REPOST_ACTION = bytes("YOUTUBE_COMMUNITY_REPOST");

    private static byte[] bytes(String string) {
        return string.getBytes(StandardCharsets.US_ASCII);
    }

    private static boolean isAnyButtonHidden() {
        return Settings.HIDE_SHARE_SHEET_INVITE.get()
                || Settings.HIDE_SHARE_SHEET_CREATE_POST.get()
                || Settings.HIDE_SHARE_SHEET_QUICK_SHARE.get()
                || Settings.HIDE_SHARE_SHEET_REMIX.get()
                || Settings.HIDE_SHARE_SHEET_REPOST.get();
    }

    /**
     * Injection point.
     */
    public static byte[] onElementLoaded(byte[] bytes) {
        if (!isAnyButtonHidden()
                || indexOf(bytes, SHARE_SHEET_IDENTIFIER, 0, Math.min(bytes.length, IDENTIFIER_SEARCH_LENGTH)) < 0) {
            return bytes;
        }

        try {
            var element = NewElement.parseFrom(bytes).toBuilder();
            var type = element.getType().toBuilder();
            var componentType = type.getComponentType().toBuilder();
            var model = componentType.getModel().toBuilder();
            if (model.getDataCase() != Model.DataCase.SHARESHEETMODEL) {
                return bytes;
            }
            var shareSheetModel = model.getShareSheetModel().toBuilder();
            var data = shareSheetModel.getData().toBuilder();

            if (!removeButtons(data)) {
                return bytes;
            }

            shareSheetModel.setData(data);
            model.setShareSheetModel(shareSheetModel);
            componentType.setModel(model);
            type.setComponentType(componentType);
            element.setType(type);
            return element.build().toByteArray();
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to hide share sheet buttons", ex);
        }

        return bytes;
    }

    /**
     * @return If any button was removed.
     */
    private static boolean removeButtons(ShareSheetData.Builder data) {
        boolean modified = false;

        if (Settings.HIDE_SHARE_SHEET_INVITE.get() && data.hasOnPlatformSharing()) {
            var onPlatformSharing = data.getOnPlatformSharing().toBuilder();
            var segment = onPlatformSharing.getSegment().toBuilder();
            if (!segment.getInviteButton().isEmpty()) {
                segment.clearInviteButton();
                onPlatformSharing.setSegment(segment);
                data.setOnPlatformSharing(onPlatformSharing);
                modified = true;
            }
        }

        if (data.hasFirstPartyShareTargets()) {
            var segment = data.getFirstPartyShareTargets().toBuilder();
            ShareTargetList targetList = segment.getShareTargetList();
            List<ByteString> targets = targetList.getShareTargetsList();

            var newTargetList = ShareTargetList.newBuilder(targetList).clearShareTargets();
            for (ByteString target : targets) {
                if (isShareTargetHidden(target.toByteArray())) {
                    modified = true;
                } else {
                    newTargetList.addShareTargets(target);
                }
            }

            if (modified) {
                segment.setShareTargetList(newTargetList);
                data.setFirstPartyShareTargets(segment);
            }
        }

        return modified;
    }

    private static boolean isShareTargetHidden(byte[] target) {
        final int iconPathIndex = indexOf(target, ICON_PATH, 0, target.length);
        if (iconPathIndex < 0) {
            return false;
        }
        final int iconIndex = iconPathIndex + ICON_PATH.length;

        if (startsWith(target, iconIndex, QUICK_SHARE_ICON)) {
            return Settings.HIDE_SHARE_SHEET_QUICK_SHARE.get();
        }
        if (startsWith(target, iconIndex, REMIX_ICON)) {
            return Settings.HIDE_SHARE_SHEET_REMIX.get();
        }
        if (startsWith(target, iconIndex, COMMUNITY_POST_ICON)) {
            return indexOf(target, REPOST_ACTION, iconIndex, target.length) >= 0
                    ? Settings.HIDE_SHARE_SHEET_REPOST.get()
                    : Settings.HIDE_SHARE_SHEET_CREATE_POST.get();
        }

        return false;
    }

    private static boolean startsWith(byte[] array, int index, byte[] prefix) {
        if (index + prefix.length > array.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (array[index + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(byte[] array, byte[] pattern, int start, int end) {
        for (int i = start; i <= end - pattern.length; i++) {
            if (startsWith(array, i, pattern)) {
                return i;
            }
        }
        return -1;
    }
}
