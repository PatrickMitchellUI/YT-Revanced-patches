package app.revanced.patches.youtube.misc.links

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * 20.36 and lower.
 */
internal val BytecodePatchContext.abUriParserLegacyMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Ljava/lang/Object;")
    parameterTypes("Ljava/lang/Object;")
    instructions(
        "Found entityKey=`"(),
        "that does not contain a PlaylistVideoEntityId"(String::contains),
        method { toString() == "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;" },
    )
}

/**
 * 20.37+
 */
internal val BytecodePatchContext.abUriParserMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Ljava/lang/Object;")
    parameterTypes("Ljava/lang/Object;")
    instructions(
        // Method is a switch statement of unrelated code,
        // and there's no strings or anything unique to match to.
        method { toString() == "Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;" },
        method { toString() == "Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;" },
        method { toString() == "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;" },
        method { toString() == "Ljava/util/List;->get(I)Ljava/lang/Object;" },
    )
}

/**
 * 20.49+ ExternalIntents.launchBrowserIntentForUri(Context, Uri), which opens links in the browser.
 */
internal val BytecodePatchContext.launchBrowserIntentForUriMethod by gettingFirstMethodDeclaratively(
    "launchBrowserIntentForUri",
) {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("Z")
    parameterTypes("Landroid/content/Context;", "Landroid/net/Uri;")
}

/**
 * 20.49+ Resolves to the class that opens links in a custom tab.
 */
internal val BytecodePatchContext.customTabsIntentMethodMatch by composingFirstMethod {
    instructions(
        "android.support.customtabs.extra.SEND_TO_EXTERNAL_HANDLER"(),
    )
}

/**
 * Opens a link with a plain view intent when no custom tab app is found.
 * "Open links externally" always takes this path.
 */
internal val BytecodePatchContext.openUrlFallbackMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/content/Intent;", "Landroid/net/Uri;", "L", "Ljava/util/Map;")
    instructions(
        "original_click_tracking_params"(),
        method { name == "startActivity" && definingClass == "Landroid/app/Activity;" },
    )
}
