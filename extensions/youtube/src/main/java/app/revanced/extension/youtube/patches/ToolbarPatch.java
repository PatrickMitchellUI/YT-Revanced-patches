package app.revanced.extension.youtube.patches;

import android.view.View;
import android.widget.ImageView;

import app.revanced.extension.shared.Logger;

@SuppressWarnings("unused")
public class ToolbarPatch {

    /**
     * Injection point.
     */
    public static void hookToolbar(Enum<?> iconEnum, ImageView imageView) {
        if (iconEnum != null && imageView.getParent() instanceof View view) {
            String enumName = iconEnum.name();
            Logger.printDebug(() -> "enum: " + enumName);
            hookToolbar(enumName, view);
        }
    }

    /**
     * Injection point.
     */
    private static void hookToolbar(String enumString, View parentView) {
        // Code added by patch.
    }
}