package org.texttechnologylab.services;

import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.dto.UCEMetadataFilterDto;
import org.texttechnologylab.models.gbif.GbifOccurrence;
import org.texttechnologylab.models.globe.GlobeTaxon;
import org.texttechnologylab.models.imp.ImportLog;
import org.texttechnologylab.models.imp.UCEImport;
import org.texttechnologylab.models.search.*;

import java.util.List;

public interface DataInterface {

    /**
     * Fetches annotations (NE, Taxon, Time,...) of a given corpus.
     *
     * @return
     */
    public List<AnnotationSearchResult> getAnnotationsOfCorpus(long corpusId, int skip, int take) throws DatabaseOperationException;

    /**
     * Returns all taxons (if any) that match the given string values AND their identifier column is not empty.
     *
     * @return
     * @throws DatabaseOperationException
     */
    public List<Taxon> getIdentifiableTaxonsByValues(List<String> tokens) throws DatabaseOperationException;

    /**
     * Counts all documents within a given corpus
     *
     * @return
     */
    public int countDocumentsInCorpus(long id) throws DatabaseOperationException;

    /**
     * Returns true if the document with the given documentId exists in
     * the given corpus
     *
     * @param corpusId
     * @param documentId
     * @return
     */
    public boolean documentExists(long corpusId, String documentId) throws DatabaseOperationException;

    /**
     * Gets a single corpus by its id.
     *
     */
    public Corpus getCorpusById(long id) throws DatabaseOperationException;

    /**
     * Stores a page topic distribution by a page.
     *
     */
    public void savePageTopicDistribution(Page page) throws DatabaseOperationException;

    /**
     * Stores a document topic distributions by a document.
     *
     */
    public void saveDocumentTopicDistribution(Document document) throws DatabaseOperationException;

    /**
     * Returns a corpus by name. As they aren't unique, it returns the first match.
     *
     */
    public Corpus getCorpusByName(String name) throws DatabaseOperationException;

    /**
     * Gets all UCE filters of a corpus.
     *
     */
    public List<UCEMetadataFilter> getUCEMetadataFiltersByCorpusId(long corpusId) throws DatabaseOperationException;

    /**
     * Gets all documents that belong to the given corpus
     *
     */
    public List<Document> getDocumentsByCorpusId(long corpusId, int skip, int take) throws DatabaseOperationException;

    /**
     * Gets all documents of a corpus which arent psotprocessed yet.
     *
     */
    public List<Document> getNonePostprocessedDocumentsByCorpusId(long corpusId) throws DatabaseOperationException;

    /**
     * Returns a corpus tsne plot by the given corpusId
     *
     */
    public CorpusTsnePlot getCorpusTsnePlotByCorpusId(long corpusId) throws DatabaseOperationException;

    /**
     * Gets all corpora from the database
     *
     * @return
     */
    public List<Corpus> getAllCorpora() throws DatabaseOperationException;

    /**
     * Gets the data required for the world globus to render correctly.
     *
     * @param documentId
     * @return
     */
    public List<GlobeTaxon> getGlobeDataForDocument(long documentId) throws DatabaseOperationException;

    /**
     * Gets many documents by their ids
     *
     * @return
     */
    public List<Document> getManyDocumentsByIds(List<Integer> documentIds) throws DatabaseOperationException;

    /**
     * Does a semantic role label search and returns document hits
     */
    public DocumentSearchResult semanticRoleSearchForDocuments(
            int skip,
            int take,
            List<String> arg0,
            List<String> arg1,
            List<String> arg2,
            List<String> argm,
            String verb,
            boolean countAll,
            SearchOrder order,
            OrderByColumn orderedByColumn,
            long corpusId
    ) throws DatabaseOperationException;

    /**
     * Searches for documents with a variety of criterias. It's the main db search of the biofid portal
     * The function calls a variety of stored procedures in the database.
     *
     * @param skip
     * @param take
     * @return
     */
    public DocumentSearchResult defaultSearchForDocuments(int skip,
                                                          int take,
                                                          String ogSearchQuery,
                                                          List<String> searchTokens,
                                                          SearchLayer layer,
                                                          boolean countAll,
                                                          SearchOrder order,
                                                          OrderByColumn orderedByColumn,
                                                          long corpusId,
                                                          List<UCEMetadataFilterDto> uceMetadataFilters,
                                                          boolean useTsVectorSearch,
                                                          String schema,
                                                          String sourceTable) throws DatabaseOperationException;

    /**
     * Gets a Topic Distribution determined by the T generic inheritance.
     *
     * @param clazz
     * @param id
     * @param <T>
     * @return
     * @throws DatabaseOperationException
     */
    public <T extends TopicDistribution> T getTopicDistributionById(Class<T> clazz, long id) throws DatabaseOperationException;

    /**
     * Get Topic Distributions by a topic. This is basically a search for annotated topics.
     *
     * @param clazz
     * @param topic
     * @param <T>
     * @return
     * @throws DatabaseOperationException
     */
    public <T extends TopicDistribution> List<T> getTopicDistributionsByString(Class<T> clazz, String topic, int limit) throws DatabaseOperationException;

    /**
     * Gets a document by its corpusId and the documentId, which isn't its primary key identifier "id".
     */
    public Document getDocumentByCorpusAndDocumentId(long corpusId, String documentId) throws DatabaseOperationException;

    public List<UCEMetadata> getUCEMetadataByDocumentId(long documentId) throws DatabaseOperationException;

    /**
     * Gets a single UCEImport object from the database.
     */
    public UCEImport getUceImportByImportId(String importId) throws DatabaseOperationException;

    /**
     * Generic operation that fetches documents given the parameters
     */
    public Document getDocumentById(long id) throws DatabaseOperationException;

    /**
     * Gets the corresponding gbifOccurrences to a gbifTaxonId
     */
    public List<GbifOccurrence> getGbifOccurrencesByGbifTaxonId(long gbifTaxonId) throws DatabaseOperationException;

    /**
     * Gets a list of distinct documents that contain a named entity with a given covered text.
     * @param annotationName Either "namedEntities" or "times". It's the list name of the annotations within a Document objects.
     */
    public List<Document> getDocumentsByAnnotationCoveredText(String coveredText, int limit, String annotationName) throws DatabaseOperationException;

    /**
     * Gets lemmas from a specific document that are within a begin and end range
     *
     */
    public List<Lemma> getLemmasWithinBeginAndEndOfDocument(int begin, int end, long documentId) throws DatabaseOperationException;

    /**
     * Gets a time annotation by its id
     */
    public Time getTimeAnnotationById(long id) throws DatabaseOperationException;

    /**
     * Gets a Lexicon entry by its composite id.
     */
    public LexiconEntry getLexiconEntryId(LexiconEntryId id) throws DatabaseOperationException;

    /**
     * Gets a named entity by its id
     */
    public NamedEntity getNamedEntityById(long id) throws DatabaseOperationException;

    /**
     * Gets a single taxon by its id
     */
    public Taxon getTaxonById(long id) throws DatabaseOperationException;

    /**
     * Gets a lemma by its id
     */
    public Lemma getLemmaById(long id) throws DatabaseOperationException;

    /**
     * Given a string value, return a list of lemmas that match that value.
     */
    public List<Lemma> getLemmasByValue(String covered, int limit, long documentId) throws DatabaseOperationException;

    public boolean checkIfGbifOccurrencesExist(long gbifTaxonId) throws DatabaseOperationException;

    /**
     * Gets a complete document, alongside its lists, from the database.
     */
    public Document getCompleteDocumentById(long id, int skipPages, int pageLimit) throws DatabaseOperationException;

    /**
     * Saves or updates an ImportLog belonging to a UCEImport.
     */
    public void saveOrUpdateImportLog(ImportLog importLog) throws DatabaseOperationException;

    /**
     * Saves or updates a UCEImport object.
     */
    public void saveOrUpdateUceImport(UCEImport uceImport) throws DatabaseOperationException;

    /**
     * Saves and updates a filter.
     *
     */
    public void saveOrUpdateUCEMetadataFilter(UCEMetadataFilter filter) throws DatabaseOperationException;

    /**
     * Stores a new UCEMetadataFilter
     *
     */
    public void saveUCEMetadataFilter(UCEMetadataFilter filter) throws DatabaseOperationException;

    /**
     * Stores the complete document with all its lists in the database.
     *
     */
    public void saveDocument(Document document) throws DatabaseOperationException;

    /**
     * Updates a document
     */
    public void updateDocument(Document document) throws DatabaseOperationException;

    /**
     * Saves a UCELog to the database. In those, we log requests from the user and more.
     *
     * @param log
     * @throws DatabaseOperationException
     */
    public void saveUceLog(UCELog log) throws DatabaseOperationException;

    /**
     * Stores an corpus tsne plot instance
     *
     * @param corpusTsnePlot
     */
    public void saveOrUpdateCorpusTsnePlot(CorpusTsnePlot corpusTsnePlot, Corpus corpus) throws DatabaseOperationException;

    /**
     * Stores a corpus in the database.
     *
     * @param corpus
     */
    public void saveCorpus(Corpus corpus) throws DatabaseOperationException;
}
