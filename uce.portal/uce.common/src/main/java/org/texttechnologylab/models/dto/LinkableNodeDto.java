package org.texttechnologylab.models.dto;

import org.texttechnologylab.models.Linkable;
import org.texttechnologylab.models.corpus.links.Link;
import org.texttechnologylab.models.viewModels.link.LinkableViewModel;

import java.util.ArrayList;
import java.util.List;

public class LinkableNodeDto {
    /**
     * Document, NamedEntity, Lemma, ...
     */
    private String type;
    private String unique;
    /**
     * Doesn't always exist, can be null.
     */
    private Link link;
    /**
     * A rendered HTML presentation of this linkableNode
     */
    private String nodeHtml;
    public List<LinkableNodeDto> toNodes;
    public List<LinkableNodeDto> fromNodes;

    public LinkableNodeDto(Linkable linkable) {
        this.type = linkable.getClass().getSimpleName();
        this.unique = linkable.getUnique().toString();
    }

    public void setLink(Link link) {this.link = link;}

    public String getType() {
        return type;
    }

    public String getNodeHtml() {
        return nodeHtml;
    }

    public void setNodeHtml(String nodeHtml) {
        this.nodeHtml = nodeHtml;
    }
}
