package app.revanced.extension.youtube.patches;


import static app.revanced.extension.shared.patches.litho.ConversionContext.ELEMENT_IDENTIFIER_COMPONENT;
import static app.revanced.extension.shared.patches.litho.ConversionContext.ELEMENT_IDENTIFIER_LAZILY;

import java.util.List;

import app.revanced.extension.shared.patches.litho.ConversionContext.ContextInterface;
import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.Utils;

@SuppressWarnings("unused")
public class HookTreeNodeElementPatch {

    /**
     * Injection point.
     */
    public static void onTreeNodeResultLoaded(ContextInterface contextInterface, List<Object> treeNodeResultList) {
        try {
            if (treeNodeResultList == null || treeNodeResultList.isEmpty()) {
                return;
            }
            String firstElement = treeNodeResultList.get(0).toString();
            if (ELEMENT_IDENTIFIER_COMPONENT.equals(firstElement)) {
                String path = contextInterface.patch_getPathBuilder().toString();
                onComponentLoaded(path, treeNodeResultList);
            } else if (ELEMENT_IDENTIFIER_LAZILY.equals(firstElement)) {
                String identifier = contextInterface.patch_getIdentifier();
                if (Utils.isNotEmpty(identifier)) {
                    onLazilyConvertedElementLoaded(identifier, treeNodeResultList);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onTreeNodeResultLoaded failure", ex);
        }
    }

    private static void onComponentLoaded(String path, List<Object> treeNodeResultList) {
        // Code added by patch.
    }

    private static void onLazilyConvertedElementLoaded(String identifier, List<Object> treeNodeResultList) {
        // Code added by patch.
    }
}
