package app.revanced.patches.youtube.interaction.swipecontrols

import app.revanced.patcher.accessFlags
import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.definingClass
import app.revanced.patcher.gettingFirstImmutableMethodDeclaratively
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.featureflag.FeatureFlag
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.swipeControlsHostActivityMethod by gettingFirstImmutableMethodDeclaratively {
    definingClass(EXTENSION_CLASS_DESCRIPTOR)
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameterTypes()
}

internal val BytecodePatchContext.swipeChangeVideoMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    instructions(
        FeatureFlag.SWIPE_TO_CHANGE_VIDEO_IN_FULLSCREEN(),
    )
}
