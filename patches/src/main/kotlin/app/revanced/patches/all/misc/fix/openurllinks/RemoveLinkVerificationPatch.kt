package app.revanced.patches.all.misc.fix.openurllinks

import app.revanced.patcher.patch.resourcePatch
import app.revanced.util.asSequence
import app.revanced.util.getNode
import org.w3c.dom.Element

/**
 * Removes `autoVerify` from all intents. This fixes 'open link with' that are impossible
 * to manually enable on some devices.
 */
val removeLinkVerification = resourcePatch {
    apply {
        document("AndroidManifest.xml").use { document ->
            val manifestElement = document.getNode("manifest") as Element
            val intentFilterNodes = manifestElement.getElementsByTagName("intent-filter")

            intentFilterNodes.asSequence().forEach { node ->
                val element = node as Element
                val attribute = "android:autoVerify"
                if (element.hasAttribute(attribute)) {
                    element.removeAttribute(attribute)
                }
            }
        }
    }
}
