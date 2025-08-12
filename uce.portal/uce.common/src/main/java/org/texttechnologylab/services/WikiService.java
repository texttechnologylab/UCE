package org.texttechnologylab.services;

import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.models.biofid.GnFinderTaxon;
import org.texttechnologylab.models.corpus.DocumentKeywordDistribution;
import org.texttechnologylab.models.corpus.PageKeywordDistribution;
import org.texttechnologylab.models.corpus.KeywordDistribution;
import org.texttechnologylab.models.topic.TopicWord;
import org.texttechnologylab.models.viewModels.wiki.*;
import org.texttechnologylab.states.KeywordInContextState;
import org.texttechnologylab.utils.MathUtils;
import org.texttechnologylab.utils.StringUtils;
import org.texttechnologylab.utils.SystemStatus;

import java.io.IOException;
import java.util.ArrayList;
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
        viewModel.setPagesCount(db.countPagesInCorpus(corpusId));
        viewModel.setNormalizedTopicWords(db.getNormalizedTopicWordsForCorpus(corpusId));
        viewModel.setTopicDistributions(db.getTopNormalizedTopicsByCorpusId(corpusId));

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
        viewModel.setPage(lemma.getPage());
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
        viewModel.setPage(cue.getPage());
        viewModel.setDocument(db.getDocumentById(negation.getDocument().getId()));
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setCoveredText(coveredText);
        //viewModel.setCoveredText("lol");
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
        var unifiedTopic = db.getInitializedUnifiedTopicById(id);
        viewModel.setWikiModel(unifiedTopic);
        viewModel.setPage(unifiedTopic.getPage());
        viewModel.setDocument(db.getDocumentById(unifiedTopic.getDocument().getId()));
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setCoveredText(coveredText);
        viewModel.setAnnotationType("UnifiedTopic");
        viewModel.setTopics(unifiedTopic.getOrderedTopics("desc"));

        return viewModel;
    }

    public EmotionWikiPageViewModel buildEmotionWikiPageViewModel(long id, String coveredText) throws DatabaseOperationException {
        var viewModel = new EmotionWikiPageViewModel();
        var emotion = db.getInitializedEmotionById(id);
        viewModel.setWikiModel(emotion);
        viewModel.setDocument(db.getDocumentById(emotion.getDocumentId()));
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setCoveredText(coveredText);
        viewModel.setAnnotationType("Emotion");

        return viewModel;
    }

    /**
     * Builds a view model to render a toxic annotation wiki page
     */
    public ToxicAnnotationWikiPageViewModel buildToxicAnnotationWikiPageViewModel(long id, String coveredText) throws DatabaseOperationException {
        var viewModel = new ToxicAnnotationWikiPageViewModel();
        var toxic = db.getInitializedToxicById(id);
        viewModel.setWikiModel(toxic);
        viewModel.setDocument(db.getDocumentById(toxic.getDocumentId()));
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setCoveredText(coveredText);
        viewModel.setAnnotationType("Toxic");

        return viewModel;
    }

    /**
     * Gets a DocumentTopicDistributionWikiPageViewModel to render a Wikipage for that topic distribution
     */
    public TopicWikiPageViewModel buildTopicWikiPageViewModel(long id, String coveredText) throws DatabaseOperationException {
        var viewModel = new TopicWikiPageViewModel();
        var documentTopThreeTopics = db.getDocumentTopThreeTopicsById(id);
        viewModel.setWikiModel(documentTopThreeTopics);
        viewModel.setDocument(db.getDocumentById(documentTopThreeTopics.getDocumentId()));
        var corpusId = viewModel.getDocument().getCorpusId();
        viewModel.setCorpus(db.getCorpusById(corpusId).getViewModel());
        viewModel.setCoveredText(coveredText);
        viewModel.setAnnotationType("Topic");
        viewModel.setDocumentTopicDistribution(documentTopThreeTopics);

        if (coveredText != null && !coveredText.isEmpty()) {
            List<TopicWord> topicWords = db.getTopicWordsByTopicLabel(
                coveredText, corpusId
            );
            viewModel.setTopicTerms(topicWords);

            List<Object[]> topDocuments = db.getTopDocumentsByTopicLabel(coveredText, corpusId, 20);
            viewModel.setTopDocumentsForTopic(topDocuments);

            List<Object[]> similarTopics = db.getSimilarTopicsbyTopicLabel(coveredText, corpusId, 2, 8);
            viewModel.setSimilarTopics(similarTopics);
        }

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

        viewModel.setTopicDistribution(db.getTopTopicsByDocument(doc.getId(), 10));
        viewModel.setTopicWords(db.getDocumentWordDistribution(doc.getId()));
        viewModel.setSimilarDocuments(db.getSimilarDocumentbyDocumentId(doc.getId()));

        return viewModel;
    }

    /**
     * Gets an TaxonAnnotationWikiPageViewModel to render a Wikipage for that annotation
     */
    public TaxonAnnotationWikiPageViewModel buildTaxonWikipageViewModel(long id, String coveredText, Class<?> clazz) throws DatabaseOperationException, IOException {
        var viewModel = new TaxonAnnotationWikiPageViewModel();
        viewModel.setCoveredText(coveredText);
        var taxon = clazz == GnFinderTaxon.class ? db.getGnFinderTaxonById(id) : db.getGazetteerTaxonById(id);
        viewModel.setAnnotatedBy(clazz.getSimpleName());
        viewModel.setLemmas(db.getLemmasWithinBeginAndEndOfDocument(taxon.getBegin(), taxon.getEnd(), taxon.getDocumentId()));
        viewModel.setWikiModel(taxon);
        viewModel.setPage(taxon.getPage());
        viewModel.setOdds(1);
        if(clazz == GnFinderTaxon.class)
            viewModel.setOdds(MathUtils.log10OddsToProbability(((GnFinderTaxon)taxon).getOddsLog10()));
        // We are not interested in the standard w3 XML triplets
        var biofidUrl = StringUtils.BIOFID_URL_BASE + taxon.getRecordId();
        viewModel.setNextRDFNodes(
                sparqlService.queryBySubject(biofidUrl));
        viewModel.setGbifOccurrences(new ArrayList<>());
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

        if(SystemStatus.JenaSparqlStatus.isAlive() && taxon.getRecordId() != -1){
            viewModel.setAlternativeNames(sparqlService.getAlternativeNamesOfTaxons(List.of(biofidUrl)));
        }

        return viewModel;
    }

    /**
     * Builds a SentenceAnnotationWikiPageViewModel for a wiki page for a Sentence annotation.
     */
    public SentenceWikiPageViewModel buildSentenceAnnotationWikiPageViewModel(long id) throws DatabaseOperationException {
        var viewModel = new SentenceWikiPageViewModel();
        var sentence = db.getSentenceAnnotationById(id);
        viewModel.setCoveredText(sentence.getCoveredText());
        viewModel.setLemmas(db.getLemmasWithinBeginAndEndOfDocument(sentence.getBegin(), sentence.getEnd(), sentence.getDocumentId()));
        viewModel.setWikiModel(sentence);
        viewModel.setPage(sentence.getPage());
        viewModel.setDocument(db.getDocumentById(sentence.getDocumentId()));
        viewModel.setAnnotationType("Sentence");
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setSimilarDocuments(
                db.getDocumentsByAnnotationCoveredText(sentence.getCoveredText(), 10, "sentences")
                        .stream()
                        .filter(d -> d.getId() != viewModel.getDocument().getId())
                        .toList());

        var kwicState = new KeywordInContextState();
        kwicState.recalculate(List.of(viewModel.getDocument()), List.of(viewModel.getCoveredText()));
        viewModel.setKwicState(kwicState);

        return viewModel;
    }

    public GeoNameAnnotationWikiPageViewModel buildGeoNameAnnotationWikiPageViewModel(long id, String coveredText) throws DatabaseOperationException {
        var viewModel = new GeoNameAnnotationWikiPageViewModel();
        viewModel.setCoveredText(coveredText);
        var geoName = db.getGeoNameAnnotationById(id);
        viewModel.setLemmas(db.getLemmasWithinBeginAndEndOfDocument(geoName.getBegin(), geoName.getEnd(), geoName.getDocumentId()));
        viewModel.setWikiModel(geoName);
        viewModel.setPage(geoName.getPage());
        viewModel.setDocument(db.getDocumentById(geoName.getDocumentId()));
        viewModel.setAnnotationType("GeoName");
        viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()).getViewModel());
        viewModel.setSimilarDocuments(
                db.getDocumentsByAnnotationCoveredText(geoName.getCoveredText(), 10, "geoNames")
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
    public NamedEntityAnnotationWikiPageViewModel buildTimeAnnotationWikiPageViewModel(long id, String coveredText) throws DatabaseOperationException {
        var viewModel = new NamedEntityAnnotationWikiPageViewModel();
        viewModel.setCoveredText(coveredText);
        // Currently, Time is handled like a NamedEntity.
        var time = db.getTimeAnnotationById(id);
        viewModel.setPage(time.getPage());
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
        viewModel.setPage(ner.getPage());
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
