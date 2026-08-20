package app.revanced.patches.youtube.layout.hide.infocards

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.util.getting
import app.revanced.util.using
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.infoCardsIncognitoMethod by getting {
    firstMethodDeclaratively {
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returnType("Ljava/lang/Boolean;")
        parameterTypes("L", "J")
        instructions("vibrator"())
    }
} using {
    firstImmutableMethodDeclaratively {
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returnType("Ljava/lang/String;")
        instructions(
            "player_overlay_info_card_teaser"(),
        )
    }
}

internal val BytecodePatchContext.infoCardsMethodCallMethodMatch by
    composingFirstMethod("Missing ControlsOverlayPresenter for InfoCards to work.") {
        instructions(
            Opcode.INVOKE_VIRTUAL(),
            after(Opcode.IGET_OBJECT()),
            after(Opcode.INVOKE_INTERFACE()),
            ResourceType.ID("info_cards_drawer_header"),
        )
    }
