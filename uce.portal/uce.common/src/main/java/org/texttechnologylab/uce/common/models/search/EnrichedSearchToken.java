package org.texttechnologylab.uce.common.models.search;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnrichedSearchToken {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private String value;
    private EnrichedSearchTokenType type;
    private String metadata;
    private String badgeText;
    private String badgeTone;
    private List<EnrichedSearchToken> children;
    private LinkedHashMap<String, List<EnrichedSearchToken>> groupedChildren;
    private String groupedChildrenJson;

    public EnrichedSearchToken(String value, EnrichedSearchTokenType type) {
        this.value = value;
        this.type = type;
    }

    public EnrichedSearchToken() {
        this.type = EnrichedSearchTokenType.TOKEN;
    }

    public String getChildrenAsString(){
        if(getChildren() == null || getChildren().isEmpty()) return "";
        return "( " + String.join(" | ", getChildren().stream().map(c -> "'" + c.getValue() + "'").toList());
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public EnrichedSearchTokenType getType() {
        return type;
    }

    public void setType(EnrichedSearchTokenType type) {
        this.type = type;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getBadgeText() {
        return badgeText;
    }

    public void setBadgeText(String badgeText) {
        this.badgeText = badgeText;
    }

    public String getBadgeTone() {
        return badgeTone;
    }

    public void setBadgeTone(String badgeTone) {
        this.badgeTone = badgeTone;
    }

    public List<EnrichedSearchToken> getChildren() {
        return children;
    }

    public void setChildren(List<EnrichedSearchToken> children) {
        this.children = children;
    }

    public LinkedHashMap<String, List<EnrichedSearchToken>> getGroupedChildren() {
        return groupedChildren;
    }

    public void setGroupedChildren(LinkedHashMap<String, List<EnrichedSearchToken>> groupedChildren) {
        this.groupedChildren = groupedChildren;
        this.groupedChildrenJson = toGroupedChildrenJson(groupedChildren);
    }

    public boolean hasGroupedChildren() {
        return groupedChildren != null && !groupedChildren.isEmpty();
    }

    public String getGroupedChildrenJson() {
        return groupedChildrenJson == null ? "" : groupedChildrenJson;
    }

    private String toGroupedChildrenJson(Map<String, List<EnrichedSearchToken>> groupedChildren) {
        if (groupedChildren == null || groupedChildren.isEmpty()) return "";
        var flat = new LinkedHashMap<String, List<Map<String, String>>>();
        for (var entry : groupedChildren.entrySet()) {
            var values = new ArrayList<Map<String, String>>();
            if (entry.getValue() != null) {
                for (var token : entry.getValue()) {
                    if (token == null || token.getValue() == null || token.getValue().isBlank()) continue;
                    var child = new LinkedHashMap<String, String>();
                    child.put("value", token.getValue());
                    child.put("meta", token.getMetadata() == null ? "" : token.getMetadata());
                    child.put("badgeText", token.getBadgeText() == null ? "" : token.getBadgeText());
                    child.put("badgeTone", token.getBadgeTone() == null ? "" : token.getBadgeTone());
                    values.add(child);
                }
            }
            flat.put(entry.getKey(), values);
        }
        return GSON.toJson(flat);
    }
}
