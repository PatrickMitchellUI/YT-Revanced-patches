package app.revanced.extension.youtube.patches.span;


import android.text.SpannableString;
import app.revanced.extension.shared.patches.span.SpanFilter;
import app.revanced.extension.shared.patches.span.SpanType;
import app.revanced.extension.shared.patches.span.StringSpanFilterGroup;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public final class SanitizeVideoSubtitleFilter extends SpanFilter {

    public SanitizeVideoSubtitleFilter() {
        addCallbacks(
                new StringSpanFilterGroup(
                        Settings.SANITIZE_VIDEO_SUBTITLE,
                        "|video_subtitle."
                )
        );
    }

    @Override
    public boolean skip(String conversionContext, SpannableString spannableString, Object span,
                        int start, int end, int flags, boolean isWord, SpanType spanType, StringSpanFilterGroup matchedGroup) {
        if (isWord) {
            if (spanType == SpanType.IMAGE) {
                hideImageSpan(spannableString, start, end, flags);
                return true;
            } else if (spanType == SpanType.CUSTOM_CHARACTER_STYLE) {
                hideSpan(spannableString, start, end, flags);
                return true;
            }
        }
        return false;
    }
}
