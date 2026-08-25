package app.revanced.patches.all.misc.links

import app.revanced.patcher.patch.resourcePatch
import app.revanced.util.asSequence
import app.revanced.util.getNode
import org.w3c.dom.Element

val disableAppLinksVerificationPatch =
    resourcePatch(
        name = "Disable app link verification",
        description = "Removes 'android:autoVerify' from all intents.",
        use = false,
    ) {
        apply {
            document("AndroidManifest.xml").use { document ->
                val manifestElement = document.getNode("manifest") as Element

                manifestElement.getElementsByTagName("intent-filter").asSequence().filterIsInstance<Element>().forEach {
                    it.removeAttribute("android:autoVerify")
                }
            }
        }
    }
