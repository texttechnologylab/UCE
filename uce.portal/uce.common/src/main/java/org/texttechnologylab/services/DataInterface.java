package org.texttechnologylab.services;

import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.globe.GlobeTaxon;
import org.texttechnologylab.models.search.*;

import java.util.List;

public interface DataInterface {

    /**
     * Fetches annotations (NE, Taxon, Time,...) of a given corpus.
     * @return
     */
    public List<AnnotationSearchResult> getAnnotationsOfCorpus(long corpusId, int skip, int take) throws DatabaseOperationException;

    /**
     * Returns all taxons (if any) that match the given string values AND their identifier column is not empty.
     * @return
     * @throws DatabaseOperationException
     */
    public List<Taxon> getIdentifiableTaxonsByValues(List<String> tokens) throws DatabaseOperationException;

    /**
     * Counts all documents within a given corpus
     * @return
     */
    public int countDocumentsInCorpus(long id) throws DatabaseOperationException;

    /**
     * Returns true if the document with the given documentId exists in
     * the given corpus
     * @param corpusId
     * @param documentId
     * @return
     */
    public boolean documentExists(long corpusId, String documentId) throws DatabaseOperationException;

    /**
     * Gets a single corpus by its id.
     *
     * @param id
     * @return
     */
    public Corpus getCorpusById(long id) throws DatabaseOperationException;

    /**
     * Stores a page topic distribution by a page.
     * @param page
     */
    public void savePageTopicDistribution(Page page) throws DatabaseOperationException;

    /**
     * Stores a document topic distributions by a document.
     * @param document
     */
    public void saveDocumentTopicDistribution(Document document) throws DatabaseOperationException;

    /**
     * Returns a corpus by name. As they aren't unique, it returns the first match.
     * @param name
     * @return
     */
    public Corpus getCorpusByName(String name) throws DatabaseOperationException;

    /**
     * Gets all documents that belong to the given corpus
     * @param corpusId
     * @return
     */
    public List<Document> getDocumentsByCorpusId(long corpusId, int skip, int take) throws DatabaseOperationException;

    /**
     * Gets all documents of a corpus which arent psotprocessed yet.
     * @param corpusId
     * @return
     */
    public List<Document> getNonePostprocessedDocumentsByCorpusId(long corpusId) throws DatabaseOperationException;

    /**
     * Returns a corpus tsne plot by the given corpusId
     * @param corpusId
     * @return
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
                                                          List<String> searchTokens,
                                                          SearchLayer layer,
                                                          boolean countAll,
                                                          SearchOrder order,
                                                          OrderByColumn orderedByColumn,
                                                          long corpusId) throws DatabaseOperationException;

    /**
     * Gets a Topic Distribution determined by the T generic inheritance.
     * @param clazz
     * @param id
     * @return
     * @param <T>
     * @throws DatabaseOperationException
     */
    public <T extends TopicDistribution> T getTopicDistributionById(Class<T> clazz, long id) throws DatabaseOperationException;

    /**
     * Generic operation that fetches documents given the paramters
     *
     * @return
     */
    public Document getDocumentById(long id) throws DatabaseOperationException;

    public boolean checkIfGbifOccurrencesExist(long gbifTaxonId) throws DatabaseOperationException;

    /**
     * Gets a complete document, alongside its lists, from the database.
     *
     * @param id
     * @return
     */
    public Document getCompleteDocumentById(long id, int skipPages, int pageLimit) throws DatabaseOperationException;

    /**
     * Stores the complete document with all its lists in the database.
     *
     * @param document
     */
    public void saveDocument(Document document) throws DatabaseOperationException;

    /**
     * Updates a document
     */
    public void updateDocument(Document document) throws DatabaseOperationException;

    /**
     * Saves a UCELog to the database. In those, we log requests from the user and more.
     * @param log
     * @throws DatabaseOperationException
     */
    public void saveUceLog(UCELog log) throws DatabaseOperationException;

    /**
     * Stores an corpus tsne plot instance
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
