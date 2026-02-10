package app.revanced.patches.youtube.layout.disableupdates

import app.revanced.patcher.accessFlags
import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.custom
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.cronetHeaderMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("Ljava/lang/String;", "Ljava/lang/String;")
    instructions("Accept-Encoding"())
    // In YouTube 19.16.39 or earlier, there are two methods with almost the same structure.
    // Check the fields of the class to identify them correctly.
    custom { immutableClassDef.fields.any { it.type == "J" } }
}
