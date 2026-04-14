package app.revanced.patches.youtube.misc.fix.pipchatbar

import app.revanced.patcher.definingClass
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.name
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import app.revanced.patches.youtube.shared.YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE

internal val BytecodePatchContext.pipModeChangedMethod by gettingFirstMethodDeclaratively {
    name("onPictureInPictureModeChanged")
    definingClass(YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE)
    returnType("V")
    parameterTypes("Z", "Landroid/content/res/Configuration;")
}
