package org.texttechnologylab.services;

import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.models.corpus.DocumentKeywordDistribution;
import org.texttechnologylab.models.corpus.PageKeywordDistribution;
import org.texttechnologylab.models.corpus.KeywordDistribution;
import org.texttechnologylab.models.viewModels.wiki.*;
import org.texttechnologylab.states.KeywordInContextState;
import org.texttechnologylab.utils.SystemStatus;

import java.io.IOException;
import java.util.List;

public class WikiService {
    private final PostgresqlDataInterface_Impl db;
    private final JenaSparqlService sparqlService;

    public WikiService(PostgresqlDataInterface_Impl db, RAGService ragService, JenaSparqlService sparqlService) {
        this.db = db;
        this.sparqlService = sparqlService;
    }

    public CorpusWikiPageViewModel buildCorpusWikiPageViewModle(long corpusId, String coveredText) throws DatabaseOperationException {
        var viewModel = new CorpusWikiPageViewModel();
        var corpus = db.getCorpusById(corpusId);
        viewModel.setWikiModel(corpus);
        viewModel.setCoveredText(coveredText);
        viewModel.setAnnotationType("Corpus");
        viewModel.setCorpus(corpus.getViewModel());
        viewModel.setDocumentsCount(db.countDocumentsInCorpus(corpusId));

        return viewModel;
    }

    /**
     * Builds a view model for a documentation page.
     */
    public AnnotationWikiPageViewModel buildDocumentationWikiPageViewModel(){
        var viewModel = new AnnotationWikiPageViewModel();
        viewModel.setAnnotationType("Documentation");
        return viewModel;
    }

    /**
     * Builds a view model to render a lemma annotation wiki page
     */
    public LemmaAnnotationWikiPageViewModel buildLemmaAnnotationWikiPageViewModel(long id, String coveredText) throws DatabaseOperationException {
        var viewModel = new LemmaAnnotationWikiPageViewModel();
        var lemma = db.getLemmaById(id);
        viewModel.setWikiModel(lemma);
        viewModel.setDocument(db.getDocumentById(lemma.getDocumentId()));
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setCoveredText(coveredText);
        viewModel.setAnnotationType("Lemma");

        var kwicState = new KeywordInContextState();
        kwicState.recalculate(List.of(viewModel.getDocument()), List.of(viewModel.getCoveredText()));
        viewModel.setKwicState(kwicState);

        return viewModel;
    }

    /**
     * Builds a view model to render a negation (cue basis) annotation wiki page : id = cue_id
     */
    public NegationAnnotationWikiPageViewModel buildNegationAnnotationWikiPageViewModel(long id, String coveredText) throws DatabaseOperationException {
        var viewModel = new NegationAnnotationWikiPageViewModel();
        //var negation = db.getCompleteNegationById(id);
        var negation = db.getCompleteNegationByCueId(id);
        var cue = negation.getCue();
        viewModel.setWikiModel(cue);
        viewModel.setDocument(db.getDocumentById(negation.getDocument().getId()));
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setCoveredText(coveredText);
        viewModel.setAnnotationType("Cue");

        viewModel.setCue(cue);
        viewModel.setEventList(negation.getEventList());
        viewModel.setFocusList(negation.getFocusList());
        viewModel.setScopeList(negation.getScopeList());
        viewModel.setXscopeList(negation.getXscopeList());
        viewModel.setNegType(negation.getNegType());


        return viewModel;
    }

    /**
     * Gets an UnifiedTopicWikiPageViewModel to render a Wikipage for that annotation
     */
    public UnifiedTopicWikiPageViewModel buildUnifiedTopicWikiPageViewModel(long id, String coveredText) throws DatabaseOperationException {
        var viewModel = new UnifiedTopicWikiPageViewModel();
        var unifiedTopic = db.getUnifiedTopicById(id);
        viewModel.setWikiModel(unifiedTopic);
        viewModel.setDocument(db.getDocumentById(unifiedTopic.getDocument().getId()));
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setCoveredText(coveredText);
        viewModel.setAnnotationType("UnifiedTopic");
        viewModel.setTopics(unifiedTopic.getOrderedTopics("desc"));


        return viewModel;
    }

    /**
     * Gets a TopicValueBaseWikiPageViewModel to render a Wikipage for that annotation
     */
    public TopicValueBaseWikiPageViewModel buildTopicValueBaseWikiPageViewModel(long id, String coveredText) throws DatabaseOperationException {
        var viewModel = new TopicValueBaseWikiPageViewModel();
        var topicValueBase = db.getTopicValueBaseById(id);
        viewModel.setWikiModel(topicValueBase);
        viewModel.setDocument(db.getDocumentById(topicValueBase.getDocument().getId()));
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setCoveredText(coveredText);
        viewModel.setAnnotationType("TopicValueBase");
        viewModel.setTopic(topicValueBase);


        return viewModel;
    }

    /**
     * Gets an DocumentAnnitationWikiPageViewmodel to render a Wikipage for this document.
     */
    public DocumentAnnotationWikiPageViewModel buildDocumentWikiPageViewModel(long id) throws DatabaseOperationException {
        var viewModel = new DocumentAnnotationWikiPageViewModel();
        var doc = db.getDocumentById(id);
        viewModel.setDocument(doc);
        viewModel.setWikiModel(doc);
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setCoveredText("Document");
        viewModel.setAnnotationType("Document");
        if(viewModel.getCorpus().getCorpusConfig().getAnnotations().isUceMetadata())
            viewModel.setUceMetadata(db.getUCEMetadataByDocumentId(doc.getId()));

        return viewModel;
    }

    /**
     * Gets an TaxonAnnotationWikiPageViewModel to render a Wikipage for that annotation
     */
    public TaxonAnnotationWikiPageViewModel buildTaxonWikipageViewModel(long id, String coveredText) throws DatabaseOperationException, IOException {
        var viewModel = new TaxonAnnotationWikiPageViewModel();
        viewModel.setCoveredText(coveredText);
        var taxon = db.getTaxonById(id);
        viewModel.setAnnotationType("Taxon");
        viewModel.setLemmas(db.getLemmasWithinBeginAndEndOfDocument(taxon.getBegin(), taxon.getEnd(), taxon.getDocumentId()));
        viewModel.setWikiModel(taxon);
        // We are not interested in the standard w3 XML triplets
        viewModel.setNextRDFNodes(
                sparqlService.queryBySubject(taxon.getPrimaryBiofidOntologyIdentifier()));
        viewModel.setGbifOccurrences(db.getGbifOccurrencesByGbifTaxonId(taxon.getGbifTaxonId()));
        viewModel.setDocument(db.getDocumentById(taxon.getDocumentId()));
        viewModel.setAnnotationType("Taxon");
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setSimilarDocuments(
                db.getDocumentsByAnnotationCoveredText(taxon.getCoveredText(), 10, "namedEntities")
                        .stream()
                        .filter(d -> d.getId() != viewModel.getDocument().getId())
                        .toList());

        var kwicState = new KeywordInContextState();
        kwicState.recalculate(List.of(viewModel.getDocument()), List.of(viewModel.getCoveredText()));
        viewModel.setKwicState(kwicState);

        if(SystemStatus.JenaSparqlStatus.isAlive() && taxon.getIdentifier() != null && !taxon.getIdentifier().isEmpty()){
            viewModel.setAlternativeNames(sparqlService.getAlternativeNamesOfTaxons(taxon.getIdentifierAsList()));
        }

        return viewModel;
    }

    /**
     * Gets an AnnotationWikiPageViewModel to render a Wikipage for that annotation.
     */
    public NamedEntityAnnotationWikiPageViewModel buildTimeAnnotationWikiPageViewModel(long id, String coveredText) throws DatabaseOperationException {
        var viewModel = new NamedEntityAnnotationWikiPageViewModel();
        viewModel.setCoveredText(coveredText);
        // Currently, Time is handled like a NamedEntity.
        var time = db.getTimeAnnotationById(id);
        viewModel.setLemmas(db.getLemmasWithinBeginAndEndOfDocument(time.getBegin(), time.getEnd(), time.getDocumentId()));
        viewModel.setWikiModel(time);
        viewModel.setDocument(db.getDocumentById(time.getDocumentId()));
        viewModel.setAnnotationType("Named-Entity");
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setSimilarDocuments(
                db.getDocumentsByAnnotationCoveredText(time.getCoveredText(), 10, "times")
                        .stream()
                        .filter(d -> d.getId() != viewModel.getDocument().getId())
                        .toList());

        var kwicState = new KeywordInContextState();
        kwicState.recalculate(List.of(viewModel.getDocument()), List.of(viewModel.getCoveredText()));
        viewModel.setKwicState(kwicState);

        return viewModel;
    }

    /**
     * Gets an AnnotationWikiPageViewModel to render a Wikipage for that annotation.
     */
    public NamedEntityAnnotationWikiPageViewModel buildNamedEntityWikiPageViewModel(long id, String coveredText) throws DatabaseOperationException {
        var viewModel = new NamedEntityAnnotationWikiPageViewModel();
        viewModel.setCoveredText(coveredText);
        var ner = db.getNamedEntityById(id);
        viewModel.setLemmas(db.getLemmasWithinBeginAndEndOfDocument(ner.getBegin(), ner.getEnd(), ner.getDocumentId()));
        viewModel.setWikiModel(ner);
        viewModel.setDocument(db.getDocumentById(ner.getDocumentId()));
        viewModel.setAnnotationType("Named-Entity");
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setSimilarDocuments(
                db.getDocumentsByAnnotationCoveredText(ner.getCoveredText(), 10, "namedEntities")
                        .stream()
                        .filter(d -> d.getId() != viewModel.getDocument().getId())
                        .toList());

        var kwicState = new KeywordInContextState();
        kwicState.recalculate(List.of(viewModel.getDocument()), List.of(viewModel.getCoveredText()));
        viewModel.setKwicState(kwicState);

        return viewModel;
    }

    /**
     * Gets a KeywordAnnotationWikiPageViewModel that can be used to render a Wikipage for a Topic annotation.
     */
    public KeywordAnnotationWikiPageViewModel buildTopicAnnotationWikiPageViewModel(long id, String type, String coveredText) throws DatabaseOperationException {
        var viewModel = new KeywordAnnotationWikiPageViewModel();
        viewModel.setCoveredText(coveredText);
        Class<? extends KeywordDistribution> clazz = null;

        // We have currently document level topics and page level topics.
        if (type.equals("TD")) {
            clazz = DocumentKeywordDistribution.class;
            var docDist = db.getKeywordDistributionById(DocumentKeywordDistribution.class, id);
            viewModel.setWikiModel(docDist);
            viewModel.setKeywordDistribution(docDist);
            viewModel.setDocument(docDist.getDocument());
            viewModel.setAnnotationType("Document Keyword");
        } else if (type.equals("TP")) {
            clazz = PageKeywordDistribution.class;
            var pageDist = db.getKeywordDistributionById(PageKeywordDistribution.class, id);
            viewModel.setWikiModel(pageDist);
            viewModel.setKeywordDistribution(pageDist);
            viewModel.setPage(pageDist.getPage());
            viewModel.setDocument(db.getDocumentById(pageDist.getPage().getDocumentId()));
            viewModel.setAnnotationType("Page Keyword");
        }

        // Search for similar topic annotations, get them and visualize those.
        viewModel.setSimilarKeywordDistributions(db.getKeywordDistributionsByString(clazz, coveredText, 10).stream().filter(d -> d.getId() != id).toList());
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
