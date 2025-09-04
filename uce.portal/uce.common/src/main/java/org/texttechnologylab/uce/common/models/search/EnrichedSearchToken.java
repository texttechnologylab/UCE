package org.texttechnologylab.uce.common.models.search;

import java.util.List;

public class EnrichedSearchToken {

    private String value;
    private EnrichedSearchTokenType type;
    private List<EnrichedSearchToken> children;

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

    public List<EnrichedSearchToken> getChildren() {
        return children;
    }

    public void setChildren(List<EnrichedSearchToken> children) {
        this.children = children;
    }
}
