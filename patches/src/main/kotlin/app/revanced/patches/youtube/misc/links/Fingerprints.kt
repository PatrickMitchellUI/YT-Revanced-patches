package app.revanced.patches.youtube.misc.links

import app.revanced.patcher.accessFlags
import app.revanced.patcher.after
import app.revanced.patcher.allOf
import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.method
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.commentUriIntentMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L", "Ljava/util/Map;")
    "parentCsn"()
    "URL_KEY"()
    "navigation_endpoint"()
    "WEB_VIEW_BOTTOM_SHEET_TAG"()
    instructions(
        "android.intent.action.VIEW"(),
        after(
            allOf(
                Opcode.INVOKE_DIRECT(),
                after(method { toString() == "Landroid/content/Intent;-><init>(Ljava/lang/String;Landroid/net/Uri;)V" }),
            ),
        ),
    )
}

internal val BytecodePatchContext.descriptionUriIntentMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("Z")
    parameterTypes("Landroid/content/Context;", "Landroid/net/Uri;")
    "android.intent.action.VIEW"()
    "text/html"()
    "Activity not found to view uri"()
}
