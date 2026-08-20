package app.revanced.extension.shared.settings.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.preference.Preference;
import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.shared.settings.BaseSettings;

import static app.revanced.extension.shared.StringRef.str;

/**
 * A custom preference that clears the ReVanced debug log buffer when clicked.
 * Invokes the {@link Logger#clearLogBufferData} method.
 */
@SuppressWarnings("unused")
public class ClearLogBufferPreference extends Preference {

    {
        setOnPreferenceClickListener(pref -> {
            clearLogBuffer();
            return true;
        });
    }

    public ClearLogBufferPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    public ClearLogBufferPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public ClearLogBufferPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ClearLogBufferPreference(Context context) {
        super(context);
    }


    /**
     * Clears the internal log buffer and displays a toast with the result.
     */
    public static void clearLogBuffer() {
        if (!BaseSettings.DEBUG.get()) {
            Utils.showToastShort(str("revanced_debug_logs_disabled"));
            return;
        }

        // Show toast before clearing, otherwise toast log will still remain.
        Utils.showToastShort(str("revanced_debug_logs_clear_toast"));
        Logger.clearLogBufferData();
    }
}
