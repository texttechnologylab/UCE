package org.texttechnologylab.uce.common.models.search.promode;

import org.texttechnologylab.uce.common.models.search.EnrichedSearchToken;
import org.texttechnologylab.uce.common.models.search.EnrichedSearchTokenType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class EnrichmentBundle {
    private final String original;
    private EnrichedSearchTokenType tokenType;
    private final List<String> expandedValues;
    private LinkedHashMap<String, List<EnrichedSearchToken>> groupedChildren;

    public EnrichmentBundle(String original) {
        this.original = original;
        this.tokenType = EnrichedSearchTokenType.TOKEN;
        this.expandedValues = new ArrayList<>();
        this.groupedChildren = new LinkedHashMap<>();
    }

    public String getOriginal() {
        return original;
    }

    public EnrichedSearchTokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(EnrichedSearchTokenType tokenType) {
        this.tokenType = tokenType;
    }

    public List<String> getExpandedValues() {
        return expandedValues;
    }

    public LinkedHashMap<String, List<EnrichedSearchToken>> getGroupedChildren() {
        return groupedChildren;
    }

    public void setGroupedChildren(LinkedHashMap<String, List<EnrichedSearchToken>> groupedChildren) {
        this.groupedChildren = groupedChildren == null ? new LinkedHashMap<>() : groupedChildren;
    }

    public boolean isEnriched() {
        return !expandedValues.isEmpty();
    }
}
