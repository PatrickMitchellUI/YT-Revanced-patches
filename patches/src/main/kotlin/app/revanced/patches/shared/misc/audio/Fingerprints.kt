package app.revanced.patches.shared.misc.audio

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.featureflag.FeatureFlag
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.formatStreamModelToStringMethodMatch by composingFirstMethod {
    name("toString")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Ljava/lang/String;")
    instructions(
        "isDefaultAudioTrack="(String::contains),
        "audioTrackId="(String::contains),
    )
}

internal val BytecodePatchContext.selectAudioStreamMethodMatch by composingFirstMethod {
    instructions(FeatureFlag.LOCALIZED_DEFAULT_AUDIO_TRACK())
}
