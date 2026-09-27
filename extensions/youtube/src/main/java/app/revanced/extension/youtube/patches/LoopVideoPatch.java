package app.revanced.extension.youtube.patches;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.videoplayer.LoopVideoButton;

@SuppressWarnings("unused")
public class LoopVideoPatch {

    /**
     * Implemented by the app class that controls the native loop of the current video.
     */
    public interface LoopVideoController {
        // Method is added during patching.
        void patch_setLoopVideo(boolean loop);
    }

    /**
     * Loop mode of the native loop, when the current video loops.
     */
    private static final int LOOP_MODE_SINGLE_VIDEO = 2;

    private static WeakReference<LoopVideoController> controllerRef = new WeakReference<>(null);

    private static volatile boolean nativeLoopEnabled;

    @Nullable
    private static volatile String lastVideoId;

    /**
     * Injection point.
     */
    public static boolean shouldLoopVideo(Enum<?> status) {
        boolean shouldLoop = status != null && "ENDED".equals(status.name())
                && Settings.LOOP_VIDEO.get();

        // Instead of calling a method to loop the video, just seek to 00:00.
        if (shouldLoop) VideoInformation.seekTo(0);
        return shouldLoop;
    }

    /**
     * Injection point.
     */
    public static void setLoopVideoController(LoopVideoController controller) {
        controllerRef = new WeakReference<>(Objects.requireNonNull(controller));
    }

    /**
     * Injection point.
     * <p>
     * Called when the native loop mode changes.
     */
    public static void setLoopMode(int mode) {
        final boolean enabled = mode == LOOP_MODE_SINGLE_VIDEO;
        if (nativeLoopEnabled == enabled) return;

        Logger.printDebug(() -> "Native loop changed to: " + enabled);
        nativeLoopEnabled = enabled;
        LoopVideoButton.updateIcon();
    }

    /**
     * Injection point.
     * <p>
     * The native loop is reset for every video. Loop the new video if enabled in the settings.
     */
    public static void newVideoStarted(String videoId) {
        if (videoId.equals(lastVideoId)) return;
        lastVideoId = videoId;

        if (Settings.LOOP_VIDEO.get() && controllerRef.get() != null) {
            Logger.printDebug(() -> "Looping new video: " + videoId);
            setLoopVideoEnabled(true);
        }
    }

    /**
     * @return If the current video loops.
     */
    public static boolean isLoopVideoEnabled() {
        return controllerRef.get() != null
                ? nativeLoopEnabled
                : Settings.LOOP_VIDEO.get();
    }

    /**
     * @return If the native loop is used, which shows its own toast when changed.
     */
    public static boolean usesNativeLoop() {
        return controllerRef.get() != null;
    }

    public static void setLoopVideoEnabled(boolean enabled) {
        LoopVideoController controller = controllerRef.get();
        if (controller == null) {
            Settings.LOOP_VIDEO.save(enabled);
        } else {
            controller.patch_setLoopVideo(enabled);
        }
    }
}
