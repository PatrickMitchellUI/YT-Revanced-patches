package app.revanced.patches.youtube.layout.shortsplayer

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags

// 21.20+
internal val BytecodePatchContext.shortsPlaybackIntentMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PROTECTED, AccessFlags.FINAL)
    returnType("V")
    parameterTypes(
        "L",
        "Ljava/util/Map;",
        "J",
        "Ljava/lang/String;",
    )
    instructions(
        // None of these strings are unique.
        "PLAYBACK_START_DESCRIPTOR_MUTATOR"(),
        "com.google.android.apps.youtube.app.endpoint.flags"(),
        "com.google.android.libraries.youtube.rendering.elements.sender_view"(),
    )
}

// 19.25 - 21.19
internal val BytecodePatchContext.shortsPlaybackIntentLegacyMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PROTECTED, AccessFlags.FINAL)
    returnType("V")
    parameterTypes(
        "L",
        "Ljava/util/Map;",
        "J",
        "Ljava/lang/String;",
    )
    instructions(
        // None of these strings are unique.
        "com.google.android.apps.youtube.app.endpoint.flags"(),
        "ReelWatchFragmentArgs"(),
        "reels_fragment_descriptor"(),
    )
}
