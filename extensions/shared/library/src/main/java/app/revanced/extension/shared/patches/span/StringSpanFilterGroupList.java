package app.revanced.extension.shared.patches.span;

import app.revanced.extension.shared.StringTrieSearch;

public final class StringSpanFilterGroupList extends SpanFilterGroupList<String, StringSpanFilterGroup> {
    protected StringTrieSearch createSearchGraph() {
        return new StringTrieSearch();
    }
}
