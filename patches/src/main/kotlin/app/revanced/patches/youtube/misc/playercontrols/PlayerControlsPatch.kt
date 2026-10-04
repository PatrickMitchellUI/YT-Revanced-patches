package app.revanced.patches.youtube.misc.playercontrols

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.util.Document
import app.revanced.patches.shared.misc.featureflag.FeatureFlag
import app.revanced.patches.shared.misc.featureflag.featureFlagOverridePatch
import app.revanced.patches.shared.misc.featureflag.overrideBooleanFeatureFlag
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playservice.is_21_36_or_greater
import app.revanced.patches.youtube.misc.playservice.versionCheckPatch
import app.revanced.util.*
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Adds the controls of a layout file to the top of the YouTube player.
 * Only works with SponsorBlock.
 *
 * @param resourceDirectoryName The name of the directory containing the hosting resource.
 */
@Suppress("KDocUnresolvedReference")
internal lateinit var addTopControl: (String) -> Unit
    private set

/**
 * Adds the controls of a layout file to the bottom of the YouTube player, left of the fullscreen button.
 *
 * @param resourceDirectoryName The name of the directory containing the hosting resource.
 */
@Suppress("KDocUnresolvedReference")
lateinit var addBottomControl: (String) -> Unit
    private set

internal val playerControlsResourcePatch = resourcePatch {
    dependsOn(versionCheckPatch)

    // Id of the element the next bottom control is added to the left of.
    var bottomLastLeftOf = "@id/fullscreen_button"

    lateinit var bottomTargetDocument: Document

    apply {
        val targetResourceName = "youtube_controls_bottom_ui_container.xml"
        bottomTargetDocument = document("res/layout/$targetResourceName")

        val bottomTargetElement: Node = sequenceOf(
            "android.support.constraint.ConstraintLayout",
            "androidx.constraintlayout.widget.ConstraintLayout", // 21.36
        ).firstNotNullOfOrNull {
            bottomTargetDocument.getElementsByTagName(it).item(0)
        } ?: throw PatchException("Could not find the player controls container in $targetResourceName")

        var bottomInsertBeforeNode: Node = bottomTargetDocument.childNodes.findElementByAttributeValueOrThrow(
            "android:inflatedId",
            bottomLastLeftOf,
        )

        // region Bottom button positions.

        // All bottom buttons are 48 dp wide and tall with the icon in the center, moved up to the height
        // of the fullscreen icon of the unpatched app. The fullscreen button of 21.36 is 48 dp tall
        // and its padding is set by the background insets (see the extension).
        val bottomControlMargin = if (is_21_36_or_greater) "6.0dip" else "3.0dip"

        bottomTargetDocument.childNodes.findElementByAttributeValueOrThrow(
            "android:id",
            "@id/youtube_controls_fullscreen_button_stub",
        ).apply {
            setAttribute("android:layout_width", "48.0dip")
            setAttribute("android:layout_marginBottom", bottomControlMargin)
        }

        if (!is_21_36_or_greater) {
            document("res/layout/youtube_controls_fullscreen_button.xml").use { document ->
                (document.getElementsByTagName("com.google.android.libraries.youtube.common.ui.TouchImageView")
                    .item(0) as Element).apply {
                    setAttribute("android:paddingTop", "19.0dip")
                    setAttribute("android:paddingBottom", "19.0dip")
                }
            }
        }

        // endregion

        addTopControl = { resourceDirectoryName ->
            val resourceFileName = "host/layout/youtube_controls_layout.xml"
            val hostingResourceStream = inputStreamFromBundledResource(
                resourceDirectoryName,
                resourceFileName,
            ) ?: throw PatchException("Could not find $resourceFileName")

            val document = document("res/layout/youtube_controls_layout.xml")

            "RelativeLayout".copyXmlNode(
                document(hostingResourceStream),
                document,
            ).use {
                // The title ends at the SponsorBlock voting button of the host layout.
                document.childNodes.findElementByAttributeValueOrThrow(
                    "android:id",
                    "@id/player_video_heading",
                ).attributes.getNamedItem("android:layout_toStartOf").nodeValue = "@+id/revanced_sb_voting_button"
            }
        }

        addBottomControl = { resourceDirectoryName ->
            val resourceFileName = "host/layout/youtube_controls_bottom_ui_container.xml"
            val sourceDocument = document(
                inputStreamFromBundledResource(resourceDirectoryName, resourceFileName)
                    ?: throw PatchException("Could not find $resourceFileName"),
            )

            val sourceElements = sourceDocument.getElementsByTagName(
                "android.support.constraint.ConstraintLayout",
            ).item(0).childNodes

            for (index in sourceElements.length - 1 downTo 1) {
                val element = sourceElements.item(index).cloneNode(true)
                if (!element.hasAttributes()) continue

                element.attributes.getNamedItem("yt:layout_constraintRight_toLeftOf").nodeValue = bottomLastLeftOf
                bottomLastLeftOf = element.attributes.getNamedItem("android:id").nodeValue
                (element as Element).setAttribute("android:layout_marginBottom", bottomControlMargin)

                // Inserted in layout order to make debugging easier, the position comes from the constraints.
                bottomTargetDocument.adoptNode(element)
                bottomTargetElement.insertBefore(element, bottomInsertBeforeNode)
                bottomInsertBeforeNode = element
            }

            sourceDocument.close()
        }
    }

    afterDependents {
        arrayOf(
            "@id/bottom_end_container",
            "@id/multiview_button",
        ).forEach {
            bottomTargetDocument.childNodes.findElementByAttributeValue(
                "android:id",
                it,
            )?.setAttribute("yt:layout_constraintRight_toLeftOf", bottomLastLeftOf)
        }

        bottomTargetDocument.close()
    }
}

/**
 * Injects the code to initialize the controls.
 * @param descriptor The descriptor of the method which should be called.
 */
internal fun initializeTopControl(descriptor: String) {
    inflateTopControlMethod.addInstruction(
        inflateTopControlInsertIndex++,
        "invoke-static { v$inflateTopControlRegister }, $descriptor->initialize(Landroid/view/View;)V",
    )
}

/**
 * Injects the code to initialize the controls.
 * @param descriptor The descriptor of the method which should be called.
 */
fun initializeBottomControl(descriptor: String) {
    inflateBottomControlMethod.addInstruction(
        inflateBottomControlInsertIndex++,
        "invoke-static { v$inflateBottomControlRegister }, $descriptor->initializeButton(Landroid/view/View;)V",
    )
}

/**
 * Injects the code to change the visibility of controls.
 * @param descriptor The descriptor of the method which should be called.
 */
fun injectVisibilityCheckCall(descriptor: String) {
    if (!visibilityImmediateCallbacksExistModified) {
        visibilityImmediateCallbacksExistModified = true
        visibilityImmediateCallbacksExistMethod.returnEarly(true)
    }

    visibilityMethod.addInstruction(
        visibilityInsertIndex++,
        "invoke-static { p1 , p2 }, $descriptor->setVisibility(ZZ)V",
    )

    visibilityImmediateMethod.addInstruction(
        visibilityImmediateInsertIndex++,
        "invoke-static { p0 }, $descriptor->setVisibilityImmediate(Z)V",
    )

    // Uses the fade out duration of tapping the overlay to dismiss it.
    visibilityNegatedImmediateMethod.addInstruction(
        visibilityNegatedImmediateInsertIndex++,
        "invoke-static { }, $descriptor->setVisibilityNegatedImmediate()V",
    )
}

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/PlayerControlsPatch;"

private lateinit var inflateTopControlMethod: MutableMethod
private var inflateTopControlInsertIndex = -1
private var inflateTopControlRegister = -1

private lateinit var inflateBottomControlMethod: MutableMethod
private var inflateBottomControlInsertIndex = -1
private var inflateBottomControlRegister = -1

private lateinit var visibilityImmediateCallbacksExistMethod: MutableMethod
private var visibilityImmediateCallbacksExistModified = false

private lateinit var visibilityMethod: MutableMethod
private var visibilityInsertIndex = 0

private lateinit var visibilityImmediateMethod: MutableMethod
private var visibilityImmediateInsertIndex = 0

private lateinit var visibilityNegatedImmediateMethod: MutableMethod
private var visibilityNegatedImmediateInsertIndex = 0

val playerControlsPatch = bytecodePatch(
    description = "Manages the code for the player controls of the YouTube player.",
) {
    dependsOn(
        playerControlsResourcePatch,
        sharedExtensionPatch,
        resourceMappingPatch, // Used to find methods.
        playerControlsOverlayVisibilityPatch,
        versionCheckPatch,
        featureFlagOverridePatch,
    )

    apply {
        // region Control inflation.

        playerBottomControlsInflateMethodMatch.let {
            inflateBottomControlMethod = it.method
            inflateBottomControlRegister = it.method.getInstruction<OneRegisterInstruction>(it[-1]).registerA
            inflateBottomControlInsertIndex = it[-1] + 1
        }

        playerTopControlsInflateMethodMatch.let {
            inflateTopControlMethod = it.method
            inflateTopControlRegister = it.method.getInstruction<OneRegisterInstruction>(it[-1]).registerA
            inflateTopControlInsertIndex = it[-1] + 1
        }

        // endregion

        // region Visibility.

        visibilityMethod =
            playerTopControlsInflateMethodMatch.immutableClassDef.getControlsOverlayVisibilityMethod()

        // The ReVanced buttons follow the visibility of the fullscreen button.
        val fullscreenButtonMatch =
            if (is_21_36_or_greater) overlayFullscreenButtonMethodMatch else overlayViewInflateMethodMatch
        fullscreenButtonMatch.let {
            val index = if (is_21_36_or_greater) it[2] else it[-1]
            val register = it.method.getInstruction<OneRegisterInstruction>(index).registerA

            it.method.addInstruction(
                index + 1,
                "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->setFullscreenCloseButton(Landroid/view/View;)V",
            )
        }

        visibilityImmediateCallbacksExistMethod = playerControlsExtensionHookListenersExistMethod
        visibilityImmediateMethod = playerControlsExtensionHookMethod

        youtubeControlsOverlayMethod.immutableClassDef.let {
            val match = if (is_21_36_or_greater) it.motionEventTranslationHelperMethodMatch else it.motionEventMethodMatch
            visibilityNegatedImmediateMethod = match.method
            visibilityNegatedImmediateInsertIndex = match[if (is_21_36_or_greater) 1 else 0] + 1
        }

        // endregion

        // region Fullscreen button background.

        if (is_21_36_or_greater) {
            fullscreenButtonBackgroundMethodMatch.let {
                val backgroundRegister = it.method.getInstruction<OneRegisterInstruction>(it[2]).registerA
                val rectRegister = it.method.getInstruction<TwoRegisterInstruction>(it[-1]).registerB

                it.method.addInstructions(
                    it[2] + 1,
                    """
                        invoke-static { v$backgroundRegister }, $EXTENSION_CLASS_DESCRIPTOR->getFullscreenButtonBackground(Landroid/graphics/drawable/Drawable;)Landroid/graphics/drawable/Drawable;
                        move-result-object v$backgroundRegister
                        invoke-static { v$rectRegister }, $EXTENSION_CLASS_DESCRIPTOR->getFullscreenButtonBackgroundInsets(Landroid/graphics/Rect;)Landroid/graphics/Rect;
                        move-result-object v$rectRegister
                    """,
                )
            }
        }

        // endregion

        // region Layout A/B tests.

        // Different bottom controls layout (youtube_video_exploder_controls_bottom_ui_container.xml).
        overrideBooleanFeatureFlag(FeatureFlag.EXPLODER_BOTTOM_CONTROLS_LAYOUT, false)

        // Button styles that do not match the ReVanced buttons.
        overrideBooleanFeatureFlag(FeatureFlag.LARGE_FULLSCREEN_BUTTONS, false)
        overrideBooleanFeatureFlag(FeatureFlag.LARGE_OVERLAY_BUTTONS, false)
        overrideBooleanFeatureFlag(FeatureFlag.PLAYER_BUTTON_STROKE, false)

        // endregion
    }
}
