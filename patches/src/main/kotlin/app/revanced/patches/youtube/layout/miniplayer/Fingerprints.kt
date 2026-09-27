@file:Suppress("SpellCheckingInspection")

package app.revanced.patches.youtube.layout.miniplayer

import app.revanced.patcher.*
import app.revanced.patcher.ClassDefComposing
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

internal const val MINIPLAYER_DISABLED_FEATURE_KEY = 45657015L

internal val BytecodePatchContext.miniplayerModernViewParentMethod by gettingFirstImmutableMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Ljava/lang/String;")
    parameterTypes()
    instructions(
        "player_overlay_modern_mini_player_controls"(),
    )
}

/**
 * Matches using the class found in [miniplayerModernViewParentMethod].
 */
internal val ClassDef.miniplayerModernCloseButtonMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    parameterTypes()
    instructions(
        ResourceType.ID("modern_miniplayer_close"),
        allOf(Opcode.CHECK_CAST(), type("Landroid/widget/ImageView;")),
    )
}

/**
 * Matches using the class found in [miniplayerModernViewParentMethod].
 */
internal val ClassDef.miniplayerModernExpandButtonMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    parameterTypes()
    instructions(
        ResourceType.ID("modern_miniplayer_expand"),
        allOf(Opcode.CHECK_CAST(), type("Landroid/widget/ImageView;")),
    )
}

/**
 * Matches using the class found in [miniplayerModernViewParentMethod].
 */
internal val ClassDef.miniplayerModernForwardButtonMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    parameterTypes()
    instructions(
        ResourceType.ID("modern_miniplayer_forward_button"),
        afterAtMost(5, Opcode.MOVE_RESULT_OBJECT()),
    )
}

/**
 * Matches using the class found in [miniplayerModernViewParentMethod].
 */
internal val ClassDef.miniplayerModernRewindButtonMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    parameterTypes()
    instructions(
        ResourceType.ID("modern_miniplayer_rewind_button"),
        afterAtMost(5, Opcode.MOVE_RESULT_OBJECT()),
    )
}

/**
 * Matches using the class found in [miniplayerModernViewParentMethod].
 */
internal val ClassDef.miniplayerModernActionButtonMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    parameterTypes()
    instructions(
        ResourceType.ID("modern_miniplayer_overlay_action_button"),
        afterAtMost(5, Opcode.MOVE_RESULT_OBJECT()),
    )
}

/**
 * Matches using the class found in [miniplayerModernViewParentMethod].
 */
internal val ClassDef.miniplayerModernSubtitleTextMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    parameterTypes()
    instructions(
        ResourceType.ID("modern_miniplayer_subtitle_text"),
        allOf(Opcode.CHECK_CAST(), type("Landroid/widget/TextView;")),
    )
}

internal val BytecodePatchContext.miniplayerMinimumSizeMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    instructions(
        ResourceType.DIMEN("miniplayer_max_size"),
        192L(), // Default miniplayer width constant.
        128L(), // Default miniplayer height constant.
    )
}
