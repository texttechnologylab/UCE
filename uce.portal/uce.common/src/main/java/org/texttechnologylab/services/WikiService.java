package org.texttechnologylab.services;

import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.models.corpus.DocumentTopicDistribution;
import org.texttechnologylab.models.corpus.PageTopicDistribution;
import org.texttechnologylab.models.corpus.TopicDistribution;
import org.texttechnologylab.models.viewModels.wiki.AnnotationWikiPageViewModel;
import org.texttechnologylab.models.viewModels.wiki.NamedEntityAnnotationWikiPageViewModel;
import org.texttechnologylab.models.viewModels.wiki.TopicAnnotationWikiPageViewModel;
import org.texttechnologylab.states.KeywordInContextState;

import java.util.List;

public class WikiService {
    private PostgresqlDataInterface_Impl db = null;
    private RAGService ragService = null;
    private JenaSparqlService sparqlService = null;

    public WikiService(PostgresqlDataInterface_Impl db, RAGService ragService, JenaSparqlService sparqlService) {
        this.db = db;
        this.sparqlService = sparqlService;
        ragService = ragService;
    }

    /**
     * Gets an DocumentAnnitationWikiPageViewmodel to render a Wikipage for this document.
     */
    public AnnotationWikiPageViewModel buildDocumentWikiPageViewModel(long id) throws DatabaseOperationException {
        var viewModel = new AnnotationWikiPageViewModel();
        var doc = db.getDocumentById(id);
        viewModel.setDocument(doc);
        viewModel.setWikiModel(doc);
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setCoveredText("Document");
        viewModel.setAnnotationType("Document");

        return viewModel;
    }

    /**
     * Gets an AnnotationWikiPageViewModel to render a Wikipage for that annotation.
     */
    public AnnotationWikiPageViewModel buildNamedEntityWikiPageViewModel(long id, String coveredText) throws DatabaseOperationException {
        var viewModel = new NamedEntityAnnotationWikiPageViewModel();
        viewModel.setCoveredText(coveredText);
        var ner = db.getNamedEntityById(id);
        viewModel.setLemmas(db.getLemmasWithinBeginAndEndOfDocument(ner.getBegin(), ner.getEnd(), ner.getDocumentId()));
        viewModel.setWikiModel(ner);
        viewModel.setDocument(db.getDocumentById(ner.getDocumentId()));
        viewModel.setAnnotationType("Named-Entity");
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setSimilarDocuments(
                db.getDocumentsByNamedEntityValue(ner.getCoveredText(), 10)
                        .stream()
                        .filter(d -> d.getId() != viewModel.getDocument().getId())
                        .toList());

        var kwicState = new KeywordInContextState();
        kwicState.recalculate(List.of(viewModel.getDocument()), List.of(viewModel.getCoveredText()));
        viewModel.setKwicState(kwicState);

        return viewModel;
    }

    /**
     * Gets a TopicAnnotationWikiPageViewModel that can be used to render a Wikipage for a Topic annotation.
     */
    public TopicAnnotationWikiPageViewModel buildTopicAnnotationWikiPageViewModel(long id, String type, String coveredText) throws DatabaseOperationException {
        var viewModel = new TopicAnnotationWikiPageViewModel();
        viewModel.setCoveredText(coveredText);
        Class<? extends TopicDistribution> clazz = null;

        // We have currently document level topics and page level topics.
        if (type.equals("TD")) {
            clazz = DocumentTopicDistribution.class;
            var docDist = db.getTopicDistributionById(DocumentTopicDistribution.class, id);
            viewModel.setWikiModel(docDist);
            viewModel.setTopicDistribution(docDist);
            viewModel.setDocument(docDist.getDocument());
            viewModel.setAnnotationType("Document Keyword");
        } else if (type.equals("TP")) {
            clazz = PageTopicDistribution.class;
            var pageDist = db.getTopicDistributionById(PageTopicDistribution.class, id);
            viewModel.setWikiModel(pageDist);
            viewModel.setTopicDistribution(pageDist);
            viewModel.setPage(pageDist.getPage());
            viewModel.setDocument(db.getDocumentById(pageDist.getPage().getDocumentId()));
            viewModel.setAnnotationType("Page Keyword");
        }

        // Search for similar topic annotations, get them and visualize those.
        viewModel.setSimilarTopicDistributions(db.getTopicDistributionsByString(clazz, coveredText, 10).stream().filter(d -> d.getId() != id).toList());
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());

        // Search if this keyword is a lemma somewhere
        // TODO: Decide if and how this will be used.
        //var test = db.getLemmasByValue(coveredText, 10, viewModel.getDocument().getId());

        var kwicState = new KeywordInContextState();
        kwicState.recalculate(List.of(viewModel.getDocument()), List.of(viewModel.getCoveredText()));
        viewModel.setKwicState(kwicState);

        return viewModel;
    }
}
