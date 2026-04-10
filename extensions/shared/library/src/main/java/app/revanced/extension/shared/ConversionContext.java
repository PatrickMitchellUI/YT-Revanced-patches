package app.revanced.extension.shared;

public final class ConversionContext {
    public static final String ELEMENT_IDENTIFIER_COMPONENT = "ComponentType";
    public static final String ELEMENT_IDENTIFIER_CONTAINER = "Container";
    public static final String ELEMENT_IDENTIFIER_LAZILY = "LazilyConvertedElement";

    /**
     * Interface to use obfuscated methods.
     */
    public interface ContextInterface {
        // Methods implemented by patch.
        StringBuilder patch_getPathBuilder();

        String patch_getIdentifier();

        default boolean isHomeFeedOrRelatedVideo() {
            return toString().contains("horizontalCollectionSwipeProtector=null");
        }

        default boolean isSubscriptionOrLibrary() {
            return toString().contains("heightConstraint=null");
        }
    }
}
