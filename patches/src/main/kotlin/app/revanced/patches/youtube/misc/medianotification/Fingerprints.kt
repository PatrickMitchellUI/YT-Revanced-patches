package app.revanced.patches.youtube.misc.medianotification

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

internal val BytecodePatchContext.mediaSessionSetPlaybackStateMethodMatch by composingFirstMethod {
    instructions(
        method("setPlaybackState"),
    )
}
