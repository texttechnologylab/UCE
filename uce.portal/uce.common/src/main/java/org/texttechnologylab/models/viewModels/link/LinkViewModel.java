package org.texttechnologylab.models.viewModels.link;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.Linkable;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.corpus.links.AnnotationToDocumentLink;
import org.texttechnologylab.models.corpus.links.DocumentLink;
import org.texttechnologylab.models.corpus.links.DocumentToAnnotationLink;
import org.texttechnologylab.models.corpus.links.Link;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.utils.ReflectionUtils;

import javax.persistence.Transient;

public class LinkViewModel {
    private static final Logger logger = LogManager.getLogger(LinkViewModel.class);
    @Transient
    private transient final PostgresqlDataInterface_Impl db;
    private Link link;
    private LinkableViewModel fromLinkableViewModel;
    private LinkableViewModel toLinkableViewModel;

    public LinkViewModel(Link link, PostgresqlDataInterface_Impl db) {
        this.link = link;
        this.db = db;
    }

    public LinkableViewModel getFromLinkableViewModel() {
        if (fromLinkableViewModel == null) {
            try {

                // Depending on what kind of link we have here (DocumentLink, DocumentAnnotationLink..) we have to fetch specific links
                Class<? extends Linkable> fromClazz = null;
                if (link instanceof DocumentLink) fromClazz = Document.class;
                else if (link instanceof DocumentToAnnotationLink) fromClazz = Document.class;
                else if (link instanceof AnnotationToDocumentLink annoToDoc)
                    fromClazz = ReflectionUtils.getClassFromClassName(annoToDoc.getFromAnnotationType(), Linkable.class);
                final var finalFrom = fromClazz;

                fromLinkableViewModel = ExceptionUtils.tryCatchLog(() -> db.getLinkable(link.getFromId(), finalFrom).getLinkableViewModel(),
                        (ex) -> logger.error("Error fetching the fromLinkable of a link.", ex));
            } catch (Exception ex) {
                logger.error("Error fetching the fromLinkable of a link.", ex);
            }
        }
        return fromLinkableViewModel;
    }

    public LinkableViewModel getToLinkableViewModel() {
        if (toLinkableViewModel == null) {
            try {
                Class<? extends Linkable> toClazz = null;
                if (link instanceof DocumentLink) toClazz = Document.class;
                else if (link instanceof DocumentToAnnotationLink docToAnno)
                    toClazz = ReflectionUtils.getClassFromClassName(docToAnno.getToAnnotationType(), Linkable.class);
                else if (link instanceof AnnotationToDocumentLink) toClazz = Document.class;
                final var finalTo = toClazz;

                toLinkableViewModel = db.getLinkable(link.getToId(), finalTo).getLinkableViewModel();
            } catch (Exception ex) {
                logger.error("Error fetching the toLinkable of a link.", ex);
            }
        }
        return toLinkableViewModel;
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }
}
