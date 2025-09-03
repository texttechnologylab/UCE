package org.texttechnologylab.uce.common.models.viewModels.link;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.Linkable;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;

import java.util.ArrayList;
import java.util.List;

/**
 * The LinkableViewModel wraps extra logic and methods around a simple Linkable, to allow usage in UI and other situations.
 */
public class LinkableViewModel {
    private static final Logger logger = LogManager.getLogger(LinkableViewModel.class);
    private transient final PostgresqlDataInterface_Impl db;
    private final Linkable baseModel;
    private List<LinkViewModel> incomingLinks = new ArrayList<>();
    private List<LinkViewModel> outgoingLinks = new ArrayList<>();

    public LinkableViewModel(Linkable baseModel, PostgresqlDataInterface_Impl db){
        this.baseModel = baseModel;
        this.db = db;
        fetchLinks();
    }

    /**
     * Fetches all links of any type that somehow connects to this Linkable object.
     */
    private void fetchLinks(){
        var allLinks = ExceptionUtils.tryCatchLog(
                () ->db.getAllLinksOfLinkable(baseModel.getPrimaryDbIdentifier(), baseModel.getClass(), baseModel.getCompatibleLinkTypes()),
                (ex) -> logger.error("Error fetching all links connected to Linkable " + baseModel.getUnique(), ex));
        if(allLinks == null) return;
        this.incomingLinks = allLinks.stream().filter(l -> l.getToId() == baseModel.getPrimaryDbIdentifier())
                .map(l -> new LinkViewModel(l, db))
                .toList();
        this.outgoingLinks = allLinks.stream().filter(l -> l.getFromId() == baseModel.getPrimaryDbIdentifier())
                .map(l -> new LinkViewModel(l, db))
                .toList();
    }

    public Linkable getBaseModel() {
        return baseModel;
    }

    public List<LinkViewModel> getIncomingLinks() {
        return incomingLinks;
    }

    public List<LinkViewModel> getOutgoingLinks() {
        return outgoingLinks;
    }
}
