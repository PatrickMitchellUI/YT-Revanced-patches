package app.revanced.extension.shared.patches.litho;

import app.revanced.extension.shared.StringTrieSearch;

public class StringFilterGroupList extends FilterGroupList<String, StringFilterGroup> {
    protected StringTrieSearch createSearchGraph() {
        return new StringTrieSearch();
    }
}
