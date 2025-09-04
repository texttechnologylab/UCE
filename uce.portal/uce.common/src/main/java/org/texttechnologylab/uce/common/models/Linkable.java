package org.texttechnologylab.uce.common.models;

import org.texttechnologylab.uce.common.models.corpus.links.LinkableRegistry;
import org.texttechnologylab.uce.common.models.viewModels.link.LinkableViewModel;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;

import java.util.List;

public interface Linkable {
    /**
     * A document can be linked in different ways than an annotation can e.g.
     * This list holds the possible types this Linkable object can be linked with.
     */
    List<Class<? extends ModelBase>> getCompatibleLinkTypes();

    long getPrimaryDbIdentifier();

    default String getClassName() {
        var clazz = this.getClass();
        return clazz.getName();
    }

    default Object getUnique() {
        return this.getClassName() + "-" + this.getPrimaryDbIdentifier();
    }

    default void initLinkableViewModel(PostgresqlDataInterface_Impl db) {
        LinkableRegistry.nodeMap.computeIfAbsent(getUnique(), k -> new LinkableViewModel(this, db));
    }

    default LinkableViewModel getLinkableViewModel() {
        return LinkableRegistry.nodeMap.get(getUnique());
    }

}
