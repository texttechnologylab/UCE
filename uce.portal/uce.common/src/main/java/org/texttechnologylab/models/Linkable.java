package org.texttechnologylab.models;

import org.texttechnologylab.models.corpus.links.DocumentLink;
import org.texttechnologylab.models.corpus.links.Link;
import org.texttechnologylab.models.viewModels.LinkableNodeViewModel;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;

import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public interface Linkable {
    /**
     * A document can be linked in different ways than an annotation can e.g.
     * This list holds the possible types this Linkable object can be linked with.
     */
    List<Class<? extends ModelBase>> getCompatibleLinkTypes();

    long getPrimaryDbIdentifier();

    // Remember, fields in interfaces are globally static, hence this NodeMap is being shared by all
    // instances that implement this Linkable Interface, which is what we want. We hence cache the already visited
    // nodes and slowly build the node network between all of them.
    // TODO: With millions of links cached, this might cause serious RAM problems (but the WeakHashMap should be cleaned up automatically).
    WeakHashMap<Object, LinkableNodeViewModel> nodeMap = new WeakHashMap<>();

    default Object getUnique() {return this.getClass().getSimpleName() + this.getPrimaryDbIdentifier();}

    default void initLinkableNode(PostgresqlDataInterface_Impl db) {
        nodeMap.computeIfAbsent(getUnique(), k -> new LinkableNodeViewModel())
                .init(getCompatibleLinkTypes(), db, getPrimaryDbIdentifier());
    }

    default LinkableNodeViewModel getLinkableNodeVm() {
        return nodeMap.get(getUnique());
    }

}
