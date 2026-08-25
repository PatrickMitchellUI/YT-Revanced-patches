package app.revanced.patches.youtube.ad.general

import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.ad.hideFullscreenAdsPatch
import app.revanced.patches.shared.misc.litho.filter.addLithoFilter
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.layout.hide.shelves.hideHorizontalShelvesPatch
import app.revanced.patches.youtube.misc.contexthook.Endpoint
import app.revanced.patches.youtube.misc.contexthook.addOSNameHook
import app.revanced.patches.youtube.misc.contexthook.hookClientContextPatch
import app.revanced.patches.youtube.misc.engagement.addEngagementPanelIdHook
import app.revanced.patches.youtube.misc.engagement.engagementPanelHookPatch
import app.revanced.patches.youtube.misc.fix.backtoexitgesture.fixBackToExitGesturePatch
import app.revanced.patches.youtube.misc.fix.verticalscroll.verticalScrollPatch
import app.revanced.patches.youtube.misc.litho.filter.lithoFilterPatch
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.patches.youtube.misc.protobuf.hookElement
import app.revanced.patches.youtube.misc.protobuf.hookProtobufElementParserPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.forEachInstructionAsSequence
import app.revanced.util.injectHideViewCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/litho/AdsFilter;"

internal var adAttributionId = -1L
    private set

private val hideAdsResourcePatch =
    resourcePatch {
        dependsOn(
            lithoFilterPatch,
            settingsPatch,
            resourceMappingPatch,
            addResourcesPatch,
            hookClientContextPatch,
            engagementPanelHookPatch,
            hideHorizontalShelvesPatch,
        )

        apply {
            addResources("youtube", "ad.general.hideAdsResourcePatch")

            PreferenceScreen.ADS.addPreferences(
                SwitchPreference("revanced_hide_creator_store_shelf", summaryKey = null),
                SwitchPreference("revanced_hide_end_screen_store_banner", summaryKey = null),
                SwitchPreference("revanced_hide_general_ads", summaryKey = null),
                SwitchPreference("revanced_hide_merchandise_banners", summaryKey = null),
                SwitchPreference("revanced_hide_paid_promotion_label", summaryKey = null),
                SwitchPreference("revanced_hide_player_popup_ads", summaryKey = null),
                SwitchPreference("revanced_hide_self_sponsor_ads", summaryKey = null),
                SwitchPreference("revanced_hide_shopping_links", summaryKey = null),
                SwitchPreference("revanced_hide_youtube_premium_promotions", summaryKey = null),
            )

            addLithoFilter("Lapp/revanced/extension/youtube/patches/litho/AdsFilter;")
            addEngagementPanelIdHook("$EXTENSION_CLASS_DESCRIPTOR->hidePlayerPopupAds(Ljava/lang/String;)Z")

            adAttributionId = ResourceType.ID["ad_attribution"]
        }
    }

@Suppress("unused")
val hideAdsPatch =
    bytecodePatch(
        name = "Hide ads",
        description = "Adds options to remove general ads.",
    ) {
        dependsOn(
            hideAdsResourcePatch,
            hideFullscreenAdsPatch(PreferenceScreen.ADS),
            verticalScrollPatch,
            fixBackToExitGesturePatch,
            hookProtobufElementParserPatch,
            versionCheckPatch,
        )

        compatibleWith(
            "com.google.android.youtube"(
                "20.14.43",
                "20.21.37",
                "20.26.46",
                "20.31.42",
                "20.37.48",
                "20.45.36",
                "20.51.39",
            ),
        )

        apply {
            // Hide YouTube Premium promotions and YouTube Doodles.

            hookElement("$EXTENSION_CLASS_DESCRIPTOR->hideStatementBanner([B)[B")

            // Hide get premium

            getPremiumViewMethodMatch.method.apply {
                val startIndex = getPremiumViewMethodMatch[0]
                val measuredWidthRegister = getInstruction<TwoRegisterInstruction>(startIndex).registerA
                val measuredHeightInstruction = getInstruction<TwoRegisterInstruction>(startIndex + 1)

                val measuredHeightRegister = measuredHeightInstruction.registerA
                val tempRegister = measuredHeightInstruction.registerB

                addInstructionsWithLabels(
                    startIndex + 2,
                    """
                    # Override the internal measurement of the layout with zero values.
                    invoke-static {}, ${EXTENSION_CLASS_DESCRIPTOR}->hideGetPremiumView()Z
                    move-result v$tempRegister
                    if-eqz v$tempRegister, :allow
                    const/4 v$measuredWidthRegister, 0x0
                    const/4 v$measuredHeightRegister, 0x0
                    :allow
                    nop
                    # Layout width/height is then passed to a protected class method.
                """,
                )
            }

            // Hide player overlay view. This can be hidden with a regular litho filter
            // but an empty space remains.
            playerOverlayTimelyShelfMethodMatch.method.apply {
                val index = playerOverlayTimelyShelfMethodMatch[-1]
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructionsWithLabels(
                    index + 1,
                    """
                    invoke-static { v$register }, ${EXTENSION_CLASS_DESCRIPTOR}->allowAds(Z)Z
                    move-result v$register
                    if-nez v$register, :show
                    return-void
                    :show
                    nop
                """,
                )
            }

            // Hide end screen store banner.

            fullScreenEngagementAdContainerMethodMatch.let {
                it.method.apply {
                    val insertIndex = it[3]
                    val insertInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)
                    val listRegister = insertInstruction.registerC
                    val objectRegister = insertInstruction.registerD

                    replaceInstruction(
                        insertIndex,
                        "invoke-static { v$listRegister, v$objectRegister }, " +
                            "${EXTENSION_CLASS_DESCRIPTOR}->" +
                            "hideEndScreenStoreBanner(Ljava/util/List;Ljava/lang/Object;)V",
                    )
                }
            }

            // Hide ad views.

            forEachInstructionAsSequence({ _, method, instruction, index ->
                if (instruction.opcode != Opcode.CONST) return@forEachInstructionAsSequence null
                if (instruction.wideLiteral != adAttributionId) return@forEachInstructionAsSequence null

                val insertIndex = index + 1

                // Call to get the view with the id adAttribution.
                if (method.instructions.elementAt(insertIndex).opcode != Opcode.INVOKE_VIRTUAL) return@forEachInstructionAsSequence null
                val viewRegister = method.getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                return@forEachInstructionAsSequence insertIndex to viewRegister
            }) { method, (insertIndex, viewRegister) ->
                method.injectHideViewCall(
                    insertIndex,
                    viewRegister,
                    EXTENSION_CLASS_DESCRIPTOR,
                    "hideAdAttributionView",
                )
            }

            // Hide paid promotion label in miniplayer.

            miniplayerPaidPromotionLabelMethodMatch.let {
                it.method.apply {
                    val insertIndex = it[-1]
                    val viewRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    injectHideViewCall(
                        insertIndex + 1,
                        viewRegister,
                        EXTENSION_CLASS_DESCRIPTOR,
                        "hideMiniplayerPaidPromotionLabelView",
                    )
                }
            }

            setOf(
                Endpoint.BROWSE,
                Endpoint.SEARCH,
                Endpoint.NEXT,
            ).forEach { endpoint ->
                addOSNameHook(
                    endpoint,
                    "$EXTENSION_CLASS_DESCRIPTOR->hideAds(Ljava/lang/String;)Ljava/lang/String;",
                )
            }
        }
    }
