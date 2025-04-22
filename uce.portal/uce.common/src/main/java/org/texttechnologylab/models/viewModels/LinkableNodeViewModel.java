package org.texttechnologylab.models.viewModels;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.ModelBase;
import org.texttechnologylab.models.corpus.links.DocumentLink;
import org.texttechnologylab.models.corpus.links.Link;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;

import java.util.ArrayList;
import java.util.List;

public class LinkableNodeViewModel {

    private static final Logger logger = LogManager.getLogger(LinkableNodeViewModel.class);

    private final List<Link> incomingLinks = new ArrayList<>();
    private final List<Link> outgoingLinks = new ArrayList<>();
    private boolean isInitialized;
    private long dbPk;
    private PostgresqlDataInterface_Impl db;

    public LinkableNodeViewModel() {
    }

    public void init(List<Class<? extends ModelBase>> possibleLinkTypes,
                     PostgresqlDataInterface_Impl db,
                     long dbPk) {
        if (this.isInitialized) return;
        this.db = db;
        this.dbPk = dbPk;

        // Here, we now load all the links depending on which are compatible.
        // DocumentLinks
        if (possibleLinkTypes.contains(DocumentLink.class)) {
            var docLinks = ExceptionUtils.tryCatchLog(
                    () -> db.getManyDocumentLinksOfDocument(this.dbPk),
                    (ex) -> logger.error("Error fetching document links of a document.", ex));
            if (docLinks != null) {
                this.outgoingLinks.addAll(docLinks.stream().filter(l -> l.getFromId() == this.dbPk).toList());
                this.incomingLinks.addAll(docLinks.stream().filter(l -> l.getToId() == this.dbPk).toList());
            }
        }

        this.isInitialized = true;
    }

    public List<Link> getIncomingLinks() {
        return incomingLinks;
    }

    public List<Link> getOutgoingLinks() {
        return outgoingLinks;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public long getDbPk() {
        return dbPk;
    }
}
