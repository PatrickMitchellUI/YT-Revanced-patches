package app.revanced.extension.youtube.patches.litho;

import app.revanced.extension.shared.patches.litho.BufferAsciiStrings;
import app.revanced.extension.shared.patches.litho.ConversionContext.ContextInterface;
import app.revanced.extension.shared.patches.litho.Filter;
import app.revanced.extension.shared.patches.litho.StringFilterGroup;
import app.revanced.extension.youtube.patches.OpenSystemShareSheetPatch;
import app.revanced.extension.youtube.settings.Settings;

/**
 * LithoFilter for {@link app.revanced.extension.youtube.patches.OpenSystemShareSheetPatch}.
 */
@SuppressWarnings("unused")
public final class OpenSystemShareSheetFilter extends Filter {


    public OpenSystemShareSheetFilter() {
        addPathCallbacks(new StringFilterGroup(
                Settings.OPEN_SYSTEM_SHARE_SHEET,
                "share_sheet_container."
        ));
    }

    @Override
    public boolean isFiltered(ContextInterface contextInterface,
                              String identifier,
                              String accessibility,
                              String path,
                              byte[] buffer,
                              BufferAsciiStrings asciiStrings,
                              StringFilterGroup matchedGroup,
                              FilterContentType contentType,
                              int contentIndex) {

        OpenSystemShareSheetPatch.closeLithoAppShareSheet();

        return false;
    }
}
