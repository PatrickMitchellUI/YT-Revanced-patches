package app.revanced.patches.youtube.layout.hide.general

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.classDef
import app.revanced.patcher.extensions.*
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.shared.layout.hide.general.hideLayoutComponentsPatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.*
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.revanced.patches.shared.misc.spannablestring.addSpannableStringFilter
import app.revanced.patches.shared.misc.spannablestring.hookSpanPatch
import app.revanced.patches.youtube.layout.hide.shelves.hideHorizontalShelvesPatch
import app.revanced.patches.youtube.misc.engagement.engagementPanelHookPatch
import app.revanced.patches.youtube.misc.litho.filter.lithoFilterPatch
import app.revanced.patches.youtube.misc.litho.node.hookTreeNodeElementPatch
import app.revanced.patches.youtube.misc.litho.node.hookTreeNodeResult
import app.revanced.patches.youtube.misc.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.misc.playservice.*
import app.revanced.patches.youtube.misc.protobuf.hookElement
import app.revanced.patches.youtube.misc.protobuf.hookProtobufElementParserPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.*
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/litho/LayoutComponentsFilter;"
private const val DESCRIPTION_COMPONENTS_FILTER_CLASS_NAME =
    "Lapp/revanced/extension/youtube/patches/litho/DescriptionComponentsFilter;"
private const val COMMENTS_FILTER_CLASS_NAME =
    "Lapp/revanced/extension/youtube/patches/litho/CommentsFilter;"
private const val CUSTOM_FILTER_CLASS_NAME =
    "Lapp/revanced/extension/shared/patches/litho/CustomFilter;"
private const val KEYWORD_FILTER_CLASS_NAME =
    "Lapp/revanced/extension/youtube/patches/litho/KeywordContentFilter;"

private const val SANITIZE_VIDEO_SUBTITLE_FILTER_CLASS_NAME =
    "Lapp/revanced/extension/youtube/patches/span/SanitizeVideoSubtitleFilter;"

val hideLayoutComponentsPatch =
    hideLayoutComponentsPatch(
        lithoFilterPatch = lithoFilterPatch,
        settingsPatch = settingsPatch,
        generalSettingsScreen = PreferenceScreen.GENERAL,
        additionalDependencies =
            setOf(
                navigationBarHookPatch,
                versionCheckPatch,
                engagementPanelHookPatch,
                resourceMappingPatch,
                hideHorizontalShelvesPatch,
                hookProtobufElementParserPatch,
                hookTreeNodeElementPatch,
                hookSpanPatch,
            ),
        filterClasses =
            setOf(
                LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR,
                DESCRIPTION_COMPONENTS_FILTER_CLASS_NAME,
                COMMENTS_FILTER_CLASS_NAME,
                KEYWORD_FILTER_CLASS_NAME,
                CUSTOM_FILTER_CLASS_NAME,
            ),
        compatibleWithPackages =
            arrayOf(
                "com.google.android.youtube" to
                    setOf(
                        "20.14.43",
                        "20.21.37",
                        "20.26.46",
                        "20.31.42",
                        "20.37.48",
                        "20.45.36",
                        "20.51.39",
                    ),
            ),
    ) {
        addResources("youtube", "layout.hide.general.hideLayoutComponentsPatch")

        PreferenceScreen.PLAYER.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_hide_description_components_screen",
                preferences =
                    setOf(
                        SwitchPreference("revanced_hide_ai_generated_video_summary_section", summaryKey = null),
                        SwitchPreference("revanced_hide_ask_section", summaryKey = null),
                        SwitchPreference("revanced_hide_attributes_section"),
                        SwitchPreference("revanced_hide_chapters_section", summaryKey = null),
                        SwitchPreference("revanced_hide_corrections_section", summaryKey = null),
                        SwitchPreference("revanced_hide_course_progress_section", summaryKey = null),
                        SwitchPreference("revanced_hide_explore_section"),
                        SwitchPreference("revanced_hide_explore_course_section", summaryKey = null),
                        SwitchPreference("revanced_hide_explore_podcast_section", summaryKey = null),
                        SwitchPreference("revanced_hide_featured_channels_section", summaryKey = null),
                        SwitchPreference("revanced_hide_featured_links_section", summaryKey = null),
                        SwitchPreference("revanced_hide_featured_places_section", summaryKey = null),
                        SwitchPreference("revanced_hide_featured_videos_section", summaryKey = null),
                        SwitchPreference("revanced_hide_gaming_section", summaryKey = null),
                        SwitchPreference("revanced_hide_how_this_was_made_section", summaryKey = null),
                        SwitchPreference("revanced_hide_hype_points", summaryKey = null),
                        SwitchPreference("revanced_hide_info_cards_section", summaryKey = null),
                        SwitchPreference("revanced_hide_key_concepts_section", summaryKey = null),
                        SwitchPreference("revanced_hide_music_section", summaryKey = null),
                        SwitchPreference("revanced_hide_quizzes_section", summaryKey = null),
                        SwitchPreference("revanced_hide_subscribe_button", summaryKey = null),
                        SwitchPreference("revanced_hide_transcript_section", summaryKey = null),
                        SwitchPreference("revanced_hide_video_details_section", summaryKey = null),
                    ),
            ),
            PreferenceScreenPreference(
                "revanced_comments_screen",
                preferences =
                    setOf(
                        PreferenceCategory(
                            titleKey = null,
                            sorting = Sorting.UNSORTED,
                            tag = "app.revanced.extension.shared.settings.preference.NoTitlePreferenceCategory",
                            preferences =
                                setOf(
                                    SwitchPreference(
                                        "revanced_hide_comments_carousel",
                                        tag =
                                            "app.revanced.extension.shared.settings.preference.BulletPointSwitchPreference",
                                    ),
                                    TextPreference(
                                        "revanced_hide_comments_carousel_filter_strings",
                                        inputType = InputType.TEXT_MULTI_LINE,
                                    ),
                                ),
                        ),
                        SwitchPreference("revanced_hide_comments_ai_chat_summary", summaryKey = null),
                        SwitchPreference("revanced_hide_comments_channel_guidelines", summaryKey = null),
                        SwitchPreference("revanced_hide_comments_by_members_header", summaryKey = null),
                        SwitchPreference("revanced_hide_comments_section", summaryKey = null),
                        SwitchPreference("revanced_hide_comments_section_in_home_feed", summaryKey = null),
                        SwitchPreference("revanced_hide_comments_community_guidelines", summaryKey = null),
                        SwitchPreference("revanced_hide_comments_create_a_short_button", summaryKey = null),
                        SwitchPreference("revanced_hide_comments_emoji_and_timestamp_buttons", summaryKey = null),
                        SwitchPreference("revanced_hide_comments_live_chat_donators_bar", summaryKey = null),
                        SwitchPreference("revanced_hide_comments_preview_comment"),
                        SwitchPreference("revanced_hide_comments_thanks_button", summaryKey = null),
                        SwitchPreference("revanced_sanitize_comments_category_bar"),
                    ),
                sorting = Sorting.UNSORTED,
            ),
            SwitchPreference("revanced_hide_channel_bar", summaryKey = null),
            SwitchPreference("revanced_hide_channel_watermark", summaryKey = null),
            SwitchPreference("revanced_hide_crowdfunding_box", summaryKey = null),
            SwitchPreference("revanced_hide_emergency_box", summaryKey = null),
            SwitchPreference("revanced_hide_info_panels"),
            SwitchPreference("revanced_hide_join_membership_button", summaryKey = null),
            SwitchPreference("revanced_hide_live_chat_replay_button"),
            SwitchPreference("revanced_hide_medical_panels", summaryKey = null),
            SwitchPreference("revanced_hide_snackbar", summaryKey = null),
            SwitchPreference("revanced_hide_subscribers_community_guidelines", summaryKey = null),
            SwitchPreference("revanced_hide_sync_button", summaryKey = null),
            SwitchPreference("revanced_hide_timed_reactions"),
            SwitchPreference("revanced_hide_video_title"),
            SwitchPreference("revanced_sanitize_video_subtitle"),
        )

        PreferenceScreen.FEED.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_hide_keyword_content_screen",
                sorting = Sorting.UNSORTED,
                preferences =
                    setOf(
                        SwitchPreference("revanced_hide_keyword_content_home", summaryKey = null),
                        SwitchPreference("revanced_hide_keyword_content_subscriptions", summaryKey = null),
                        SwitchPreference("revanced_hide_keyword_content_search", summaryKey = null),
                        SwitchPreference("revanced_hide_keyword_content_comments", summaryKey = null),
                        TextPreference(
                            "revanced_hide_keyword_content_phrases",
                            inputType = InputType.TEXT_MULTI_LINE,
                        ),
                        NonInteractivePreference(
                            key = "revanced_hide_keyword_content_about",
                            tag = "app.revanced.extension.shared.settings.preference.BulletPointPreference",
                        ),
                        NonInteractivePreference(
                            key = "revanced_hide_keyword_content_about_whole_words",
                            tag = "app.revanced.extension.youtube.settings.preference.HTMLPreference",
                        ),
                    ),
            ),
            PreferenceScreenPreference(
                key = "revanced_hide_filter_bar_screen",
                preferences =
                    setOf(
                        SwitchPreference("revanced_hide_filter_bar_feed_in_feed", summaryKey = null),
                        SwitchPreference("revanced_hide_filter_bar_feed_in_related_videos", summaryKey = null),
                        SwitchPreference("revanced_hide_filter_bar_feed_in_search", summaryKey = null),
                        SwitchPreference("revanced_hide_filter_bar_feed_in_history", summaryKey = null),
                    ),
            ),
            PreferenceScreenPreference(
                key = "revanced_channel_screen",
                preferences =
                    setOf(
                        PreferenceCategory(
                            titleKey = null,
                            sorting = Sorting.UNSORTED,
                            tag = "app.revanced.extension.shared.settings.preference.NoTitlePreferenceCategory",
                            preferences =
                                setOf(
                                    SwitchPreference("revanced_hide_channel_tab", summaryKey = null),
                                    TextPreference(
                                        "revanced_hide_channel_tab_filter_strings",
                                        inputType = InputType.TEXT_MULTI_LINE,
                                    ),
                                ),
                        ),
                        SwitchPreference("revanced_hide_community_button", summaryKey = null),
                        SwitchPreference("revanced_hide_join_button", summaryKey = null),
                        SwitchPreference("revanced_hide_links_preview"),
                        SwitchPreference("revanced_hide_members_shelf"),
                        SwitchPreference("revanced_hide_posts_shelf", summaryKey = null),
                        SwitchPreference("revanced_hide_store_button", summaryKey = null),
                        SwitchPreference("revanced_hide_subscribe_button_in_channel_page", summaryKey = null),
                    ),
            ),
            SwitchPreference("revanced_hide_album_cards"),
            SwitchPreference("revanced_hide_artist_cards"),
            SwitchPreference("revanced_hide_auto_dubbed_label", summaryKey = null),
            SwitchPreference("revanced_hide_community_posts", summaryKey = null),
            SwitchPreference("revanced_hide_compact_banner"),
            if (is_20_26_or_greater) {
                ListPreference("revanced_hide_expandable_card")
            } else {
                ListPreference(
                    key = "revanced_hide_expandable_card",
                    entriesKey = "revanced_hide_expandable_card_legacy_entries",
                    entryValuesKey = "revanced_hide_expandable_card_legacy_entry_values",
                )
            },
            PreferenceCategory(
                titleKey = null,
                sorting = Sorting.UNSORTED,
                tag = "app.revanced.extension.shared.settings.preference.NoTitlePreferenceCategory",
                preferences =
                    setOf(
                        SwitchPreference("revanced_hide_feed_flyout_menu", summaryKey = null),
                        TextPreference(
                            "revanced_hide_feed_flyout_menu_filter_strings",
                            inputType = InputType.TEXT_MULTI_LINE,
                        ),
                    ),
            ),
            PreferenceCategory(
                titleKey = null,
                sorting = Sorting.UNSORTED,
                tag = "app.revanced.extension.shared.settings.preference.NoTitlePreferenceCategory",
                preferences =
                    setOf(
                        SwitchPreference("revanced_hide_account_menu", summaryKey = null),
                        TextPreference(
                            "revanced_hide_account_menu_filter_strings",
                            inputType = InputType.TEXT_MULTI_LINE,
                        ),
                    ),
            ),
            SwitchPreference("revanced_hide_floating_microphone_button"),
            SwitchPreference("revanced_hide_horizontal_shelves"),
            SwitchPreference("revanced_hide_hyped_label", summaryKey = null),
            SwitchPreference("revanced_hide_image_shelf"),
            SwitchPreference("revanced_hide_latest_videos_button"),
            SwitchPreference("revanced_hide_mix_playlists", summaryKey = null),
            SwitchPreference("revanced_hide_movies_section", summaryKey = null),
            SwitchPreference("revanced_hide_notify_me_button"),
            SwitchPreference("revanced_hide_playables"),
            SwitchPreference("revanced_hide_search_term_thumbnails"),
            SwitchPreference("revanced_hide_show_more_button"),
            SwitchPreference("revanced_hide_surveys"),
            SwitchPreference("revanced_hide_ticket_shelf", summaryKey = null),
            SwitchPreference(
                "revanced_hide_upload_time",
                tag = "app.revanced.extension.shared.settings.preference.BulletPointSwitchPreference",
            ),
            SwitchPreference("revanced_hide_video_recommendation_labels"),
            SwitchPreference(
                "revanced_hide_view_count",
                tag = "app.revanced.extension.shared.settings.preference.BulletPointSwitchPreference",
            ),
            SwitchPreference("revanced_hide_visual_spacer", summaryKey = null),
            SwitchPreference("revanced_hide_youtube_doodles"),
        )

        if (is_20_21_or_greater) {
            PreferenceScreen.FEED.addPreferences(
                SwitchPreference("revanced_hide_you_may_like_section", summaryKey = null),
            )
        }

        addSpannableStringFilter(SANITIZE_VIDEO_SUBTITLE_FILTER_CLASS_NAME)

        hookTreeNodeResult("$COMMENTS_FILTER_CLASS_NAME->sanitizeCommentsCategoryBar")

        // region Hide mix playlists

        parseElementFromBufferMethodMatch.let {
            it.method.apply {
                val insertIndex = it[0]

                val byteArrayParameter = "p3"
                val returnEmptyComponentIndex = it[4]
                val returnEmptyComponentInstruction = getInstruction(returnEmptyComponentIndex)

                val returnEmptyComponentRegister =
                    (returnEmptyComponentInstruction as FiveRegisterInstruction).registerC
                val freeRegister = findFreeRegister(insertIndex, returnEmptyComponentRegister)

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    """
                    invoke-static { $byteArrayParameter }, $LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->filterMixPlaylists([B)Z
                    move-result v$freeRegister 
                    if-eqz v$freeRegister, :show
                    move-object v$returnEmptyComponentRegister, p1   # Required for 19.47
                    goto :return_empty_component
                    :show
                    nop
                """,
                    ExternalLabel("return_empty_component", returnEmptyComponentInstruction),
                )
            }
        }

        // endregion

        // region Hide watermark (legacy code for old versions of YouTube)

        showWatermarkMethod.apply {
            val index = instructions.size - 5

            removeInstruction(index)
            addInstructions(
                index,
                """
                invoke-static {}, $LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->showWatermark()Z
                move-result p2
            """,
            )
        }

        // endregion

        // region Hide Show more button

        val (textViewField, buttonContainerField) =
            hideShowMoreButtonSetViewMethodMatch.let {
                val textViewIndex = it[1]
                val buttonContainerIndex = it[3]

                Pair(
                    it.method.getInstruction<ReferenceInstruction>(textViewIndex).reference,
                    it.method.getInstruction<ReferenceInstruction>(buttonContainerIndex).reference,
                )
            }

        hideShowMoreButtonMethod.apply {
            val helperMethod =
                ImmutableMethod(
                    definingClass,
                    "patch_hideShowMoreButton",
                    listOf(),
                    "V",
                    AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                    null,
                    null,
                    MutableMethodImplementation(7),
                ).toMutable()
                    .apply {
                        addInstructions(
                            0,
                            """
                    move-object/from16 v0, p0
                    invoke-virtual { v0 }, $hideShowMoreButtonGetParentViewMethod
                    move-result-object v1
                    iget-object v2, v0, $buttonContainerField
                    iget-object v3, v0, $textViewField
                    invoke-static { v1, v2, v3 }, $LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->hideShowMoreButton(Landroid/view/View;Landroid/view/View;Landroid/widget/TextView;)V
                    return-void
                """,
                        )
                    }.also(classDef.methods::add)

            findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
                addInstruction(index, "invoke-direct/range { p0 .. p0 }, $helperMethod")
            }
        }

        // endregion

        // region Hide Subscribed channels bar

        // Tablet
        val constructorMatch =
            if (is_20_21_or_greater) {
                hideSubscribedChannelsBarConstructorMethodMatch
            } else {
                hideSubscribedChannelsBarConstructorLegacyMethodMatch
            }

        constructorMatch.method.injectHideViewCall(
            constructorMatch[1],
            LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR,
            "hideSubscribedChannelsBar",
        )

        // Phone (landscape mode)
        if (is_20_21_or_greater) {
            hideSubscribedChannelsBarLandscapeMethodMatch
        } else {
            hideSubscribedChannelsBarLandscapeLegacyMethodMatch
        }.let {
            it.method.apply {
                val index = it[-1]
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1,
                    """
                    invoke-static { v$register }, $LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->hideSubscribedChannelsBar(I)I
                    move-result v$register
                """,
                )
            }
        }

        // endregion

        // region Hide Album cards

        albumCardsMethodMatch.method.injectHideViewCall(
            albumCardsMethodMatch[-1],
            LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR,
            "hideAlbumCard",
        )

        // endregion

        // region Hide comment carousel

        hookElement("${COMMENTS_FILTER_CLASS_NAME}->onCommentsLoaded([B)[B")

        // endregion

        // region hide comments info button

        engagementPanelInformationButtonMethodMatch.let {
            it.method.apply {
                val checkCastIndex = it[1]
                val viewRegister = getInstruction<OneRegisterInstruction>(checkCastIndex).registerA

                addInstruction(
                    checkCastIndex + 1,
                    "invoke-static { v$viewRegister }, ${COMMENTS_FILTER_CLASS_NAME}->hideCommentsInfoButton(Landroid/view/View;)V",
                )
            }
        }

        //endregion

        // region Hide crowdfunding box

        crowdfundingBoxMethodMatch.method.injectHideViewCall(
            crowdfundingBoxMethodMatch[-1],
            LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR,
            "hideCrowdfundingBox",
        )

        // endregion

        // region Hide live chat donators bar

        liveChatDonatorsBarMethodMatch.method.injectHideViewCall(
            liveChatDonatorsBarMethodMatch[-1],
            COMMENTS_FILTER_CLASS_NAME,
            "hideLiveChatDonatorsBar",
        )

        // endregion

        // region Hide live chat emoji button

        thumbnailAndEmojiPickerContainerMethodMatch.method.injectHideViewCall(
            thumbnailAndEmojiPickerContainerMethodMatch[-1],
            COMMENTS_FILTER_CLASS_NAME,
            "hideLiveChatEmojiButton",
        )

        // endregion

        // region Hide live chat thanks button

        inlineExtraButtonsContainerMethodMatch.method.injectHideViewCall(
            inlineExtraButtonsContainerMethodMatch[-1],
            COMMENTS_FILTER_CLASS_NAME,
            "hideLiveChatThanksButton",
        )

        // endregion

        // region Hide account menu

        accountMenuMethodMatch.methodOrNull?.apply {
            val targetIndex = accountMenuMethodMatch[2]
            val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

            addInstruction(
                targetIndex + 1,
                "invoke-static { v${targetInstruction.registerC}, v${targetInstruction.registerD} }, " +
                    "$LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->hideAccountBottomItemModern(Landroid/view/View;Ljava/lang/CharSequence;)V",
            )
        }

        accountMenuLegacyMethodMatch.methodOrNull?.apply {
            val targetIndex = accountMenuLegacyMethodMatch[2]
            val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

            addInstruction(
                targetIndex + 1,
                "invoke-static { v${targetInstruction.registerC}, v${targetInstruction.registerD} }, " +
                    "$LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->hideAccountBottomItemLegacy(Landroid/view/View;Ljava/lang/CharSequence;)V",
            )
        }

        accountListMethodMatch.methodOrNull?.apply {
            val literalIndex = accountListMethodMatch[0]
            val targetIndex =
                indexOfFirstInstructionOrThrow(literalIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                        methodReference?.name == "setText"
                }
            val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

            addInstruction(
                targetIndex + 1,
                "invoke-static { v${targetInstruction.registerC}, v${targetInstruction.registerD} }, " +
                    "$LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->hideAccountTopItem(Landroid/view/View;Ljava/lang/CharSequence;)V",
            )
        }

        // endregion

        // region Hide Floating microphone

        val floatingMicrophoneButtonMethodMatch =
            if (is_21_11_or_greater) {
                showFloatingMicrophoneButtonMethodMatch
            } else {
                showFloatingMicrophoneButtonLegacyMethod
            }

        floatingMicrophoneButtonMethodMatch.let {
            it.method.apply {
                val index = it[-1]
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1,
                    """
                    invoke-static { v$register }, $LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->hideFloatingMicrophoneButton(Z)Z
                    move-result v$register
                """,
                )
            }
        }

        // endregion

        // region Hide latest videos button

        listOf(
            latestVideosContentPillMethodMatch,
            latestVideosBarMethodMatch,
        ).forEach { match ->
            match.method.injectHideViewCall(
                match[-1],
                LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR,
                "hideLatestVideosButton",
            )
        }

        // endregion

        // region Hide 'Yoodles'

        yoodlesImageViewMethod.apply {
            findInstructionIndicesReversedOrThrow {
                methodReference?.name == "setImageDrawable"
            }.forEach { insertIndex ->
                val drawableRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD
                val imageViewRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                replaceInstruction(
                    insertIndex,
                    "invoke-static { v$imageViewRegister, v$drawableRegister }, $LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->" +
                        "setDoodleDrawable(Landroid/widget/ImageView;Landroid/graphics/drawable/Drawable;)V",
                )
            }
        }

        // endregion

        // region Hide view count

        hideViewCountMethodMatch.method.apply {
            val startIndex = hideViewCountMethodMatch[0]
            var returnStringRegister = getInstruction<OneRegisterInstruction>(startIndex).registerA

            // Find the instruction where the text dimension is retrieved.
            val applyDimensionIndex =
                indexOfFirstInstructionReversedOrThrow {
                    val reference = methodReference
                    opcode == Opcode.INVOKE_STATIC &&
                        reference?.definingClass == "Landroid/util/TypedValue;" &&
                        reference.returnType == "F" &&
                        reference.name == "applyDimension" &&
                        reference.parameterTypes == listOf("I", "F", "Landroid/util/DisplayMetrics;")
                }

            // A float value is passed which is used to determine subtitle text size.
            val floatDimensionRegister =
                getInstruction<OneRegisterInstruction>(
                    applyDimensionIndex + 1,
                ).registerA

            addInstructions(
                applyDimensionIndex - 1,
                """
                invoke-static { v$returnStringRegister, v$floatDimensionRegister }, $LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->modifyFeedSubtitleSpan(Landroid/text/SpannableString;F)Landroid/text/SpannableString;
                move-result-object v$returnStringRegister
            """,
            )
        }

        // endregion

        // region Hide filter bar

        mutableMapOf(
            filterBarHeightMethodMatch to "hideInFeed",
            searchResultsChipBarMethodMatch to "hideInSearch",
            getRelatedChipCloudMethodMatch() to "hideInRelatedVideos",
        ).forEach { (match, methodName) ->
            match.method.apply {
                val moveIndex = match[-1]
                val sizeRegister = getInstruction<OneRegisterInstruction>(moveIndex).registerA

                addInstructions(
                    moveIndex + 1,
                    """
                    invoke-static { v$sizeRegister }, $LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->$methodName(I)I
                    move-result v$sizeRegister
                """,
                )
            }
        }

        getRelatedChipCloudMethodMatch().let {
            it.method.apply {
                insertLiteralOverride(
                    it[2],
                    "$LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->hideInRelatedVideos(Z)Z",
                )
            }
        }

        getRelatedChipCloudMethodMatch().let {
            it.method.apply {
                val viewIndex = it[1]
                val viewRegister = getInstruction<FiveRegisterInstruction>(viewIndex).registerC

                injectHideViewCall(
                    viewIndex,
                    viewRegister,
                    LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR,
                    "hideInRelatedVideos",
                )
            }
        }

        // endregion

        // region Hide You may like section

        if (is_20_21_or_greater) {
            val searchSuggestionEndpointField =
                searchSuggestionEndpointMethodMatch.let {
                    it.method.getInstruction(it[0]).fieldReference!!
                }

            val searchSuggestionEndpointClass = searchSuggestionEndpointField.definingClass

            searchBoxTypingStringMethodMatch.let {
                it.method.apply {
                    // A collection of search suggestions.
                    // This includes trending search (also known as 'You may like' section)
                    // and your search history.

                    val searchSuggestionCollectionField = getInstruction(it[0]).fieldReference!!
                    val typedStringField = getInstruction(it[2]).fieldReference!!

                    val helperMethod =
                        ImmutableMethod(
                            definingClass,
                            "patch_setSearchSuggestions",
                            listOf(
                                ImmutableMethodParameter(
                                    parameterTypes.first().toString(),
                                    null,
                                    null,
                                ),
                            ),
                            "V",
                            AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                            annotations,
                            null,
                            MutableMethodImplementation(7),
                        ).toMutable()
                            .apply {
                                addInstructionsWithLabels(
                                    0,
                                    """
                                move-object/from16 v0, p1
                                iget-object v1, v0, $typedStringField
                                
                                # Check if the setting is enabled and if the typed string is empty.
                                invoke-static { v1 }, ${LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR}->hideYouMayLikeSection(Ljava/lang/String;)Z
                                move-result v1
                                
                                # If the setting is disabled or the typed string is not empty, do nothing.
                                if-eqz v1, :ignore

                                ## Get a collection of search suggestions.
                                iget-object v1, v0, $searchSuggestionCollectionField
                                
                                # Iterate through the collection and check if the search suggestion is the search history.
                                invoke-interface { v1 }, Ljava/util/Collection;->iterator()Ljava/util/Iterator;
                                move-result-object v2
                                
                                :loop
                                invoke-interface { v2 }, Ljava/util/Iterator;->hasNext()Z
                                move-result v3
                                if-eqz v3, :exit
                                invoke-interface { v2 }, Ljava/util/Iterator;->next()Ljava/lang/Object;
                                move-result-object v3
                                instance-of v4, v3, $searchSuggestionEndpointClass
                                if-eqz v4, :loop
                                check-cast v3, $searchSuggestionEndpointClass

                                # Each search suggestion has a command endpoint.
                                # If the search suggestion is the search history, the command includes the keyword '/delete'.
                                iget-object v4, v3, $searchSuggestionEndpointField
                                invoke-static { v3, v4 }, ${LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR}->isSearchHistory(Ljava/lang/Object;Ljava/lang/String;)Z
                                move-result v3
                                
                                # If this search suggestion is the search history, do nothing.
                                if-nez v3, :loop
                                
                                # If this search suggestion is not the search history, remove it from the search suggestions collection.
                                invoke-interface { v2 }, Ljava/util/Iterator;->remove()V
                                goto :loop

                                # Save the updated collection to a field.
                                :exit
                                iput-object v1, v0, $searchSuggestionCollectionField

                                :ignore
                                return-void
                            """,
                                )
                            }.also(it.classDef.methods::add)

                    addInstruction(
                        0,
                        "invoke-direct/range { p0 .. p1 }, $helperMethod",
                    )
                }
            }
        }

        // endregion

        // region Hide flyout menu items

        bottomSheetMenuItemBuilderMethodMatch.let {
            it.method.apply {
                val index = it[1]
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1,
                    """
                        invoke-static { v$register }, ${LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR}->hideFlyoutMenu(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$register
                    """,
                )
            }
        }

        // TODO: Refactor when ReVanced Patcher supports matching multiple methods in sequence.
        arrayOf(
            getBottomSheetMenuItemBuilderWithToStringMethod(true),
            getBottomSheetMenuItemBuilderWithToStringMethod(false),
        ).forEach { method ->
            method?.apply {
                val moveResultIndex =
                    indexOfFirstInstructionOrThrow {
                        opcode == Opcode.INVOKE_STATIC && methodReference?.let { reference ->
                            reference.returnType == "Ljava/lang/CharSequence;" &&
                                reference.parameterTypes.size == 1
                        } == true
                    } + 1
                val register = getInstruction<OneRegisterInstruction>(moveResultIndex).registerA

                addInstructions(
                    moveResultIndex + 1,
                    """
                        invoke-static { v$register }, ${LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR}->hideFlyoutMenu(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$register
                    """,
                )
            }
        }

        contextualMenuItemBuilderMethodMatch.let {
            it.method.apply {
                val index = it[1]
                val targetInstruction = getInstruction<FiveRegisterInstruction>(index)

                addInstruction(
                    index + 1,
                    "invoke-static { v${targetInstruction.registerC}, v${targetInstruction.registerD} }, " +
                        "${LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR}->hideFlyoutMenu(Landroid/widget/TextView;Ljava/lang/CharSequence;)V",
                )
            }
        }

        // endregion

        // region Hide channel tab

        if (is_21_20_or_greater) {
            channelTabRendererModernMethodMatch.let { match ->
                match.method.apply {
                    val objectIndex = match[0]
                    val register = getInstruction<TwoRegisterInstruction>(objectIndex).registerA
                    val insertIndex = objectIndex + 1
                    val free = findFreeRegister(insertIndex, register)

                    addInstructionsWithLabels(
                        insertIndex,
                        """
                            invoke-static { v$register }, ${LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR}->hideChannelTab(Ljava/lang/String;)Z
                            move-result v$free
                            if-eqz v$free, :ignore
                            return-void
                            :ignore
                            nop
                        """,
                    )
                }
            }
        } else {
            channelTabRendererLegacyMethodMatch.let { match ->
                match.method.apply {
                    val iteratorIndex = match[2]
                    val iteratorRegister = getInstruction<FiveRegisterInstruction>(iteratorIndex).registerC
                    val objectIndex = match[0]
                    val objectInstruction = getInstruction<TwoRegisterInstruction>(objectIndex)
                    val objectReference = getInstruction<ReferenceInstruction>(objectIndex).reference

                    addInstructionsWithLabels(
                        objectIndex + 1,
                        """
                            invoke-static { v${objectInstruction.registerA} }, ${LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR}->hideChannelTab(Ljava/lang/String;)Z
                            move-result v${objectInstruction.registerA}
                            if-eqz v${objectInstruction.registerA}, :ignore
                            invoke-interface { v$iteratorRegister }, Ljava/util/Iterator;->remove()V
                            goto :next_iterator
                            :ignore
                            iget-object v${objectInstruction.registerA}, v${objectInstruction.registerB}, $objectReference
                        """,
                        ExternalLabel("next_iterator", getInstruction(iteratorIndex)),
                    )
                }
            }
        }

        // endregion

        // region Hide search term thumbnails

        createSearchSuggestionsMethodMatch.let { match ->
            match.method.apply {
                val insertIndex = match[2] - 1
                val freeRegister = findFreeRegister(insertIndex)
                val jumpIndex = match[-1]

                addInstructionsWithLabels(
                    insertIndex,
                    """
                        invoke-static { }, ${LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR}->hideSearchTermThumbnails()Z
                        move-result v$freeRegister
                        
                        if-nez v$freeRegister, :hidden
                    """,
                    ExternalLabel("hidden", getInstruction(jumpIndex)),
                )
            }
        }

        // endregion

        // region Hide snackbar

        lithoSnackbarLayoutMethodMatch.let {
            it.method.apply {
                val insertIndex = it[0]
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA
                addInstruction(
                    insertIndex,
                    "invoke-static { v$register }, $LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->hideLithoSnackBar(Landroid/widget/FrameLayout;)V",
                )
            }
        }

        bottomUIContainerMethod.addInstructionsWithLabels(
            0,
            """
            invoke-static {}, $LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->hideSnackbar()Z
            move-result v0
            if-eqz v0, :show
            return-void
            :show
            nop
        """,
        )

        listOf(
            quantumSnackbarMethodMatch,
            materialSnackbarMethodMatch,
            appSnackbarMethodMatch,
            youTubeSnackbarMethodMatch,
            mealbarMethodMatch,
        ).forEach {
            it.method.addInstruction(
                it[0] + 1,
                "invoke-static { p0 }, $LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->handleLegacySnackbar(Landroid/view/View;)V",
            )
        }

        // endregion

        // region hide sync button

        syncButtonMethodMatch.let {
            it.method.injectHideViewCall(
                it[-1],
                LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR,
                "hideSyncButton",
            )
        }

        // endregion
    }
