package app.revanced.patches.youtube.misc.settings

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.licenseActivityOnCreateMethod by gettingFirstMethodDeclaratively {
    name("onCreate")
    definingClass("/LicenseActivity;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/os/Bundle;")
}

internal val BytecodePatchContext.setThemeMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    parameterTypes()
    instructions(ResourceType.STRING("app_theme_appearance_dark"))
}
