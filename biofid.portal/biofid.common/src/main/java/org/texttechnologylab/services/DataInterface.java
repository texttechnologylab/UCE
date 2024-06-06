package org.texttechnologylab.services;

import org.texttechnologylab.models.corpus.Corpus;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.globe.GlobeTaxon;
import org.texttechnologylab.models.search.*;

import java.util.List;

public interface DataInterface {

    /**
     * Fetches annotations (NE, Taxon, Time,...) of a given corpus.
     * @return
     */
    public List<AnnotationSearchResult> getAnnotationsOfCorpus(long corpusId, int skip, int take);

    /**
     * Counts all documents within a given corpus
     * @return
     */
    public int countDocumentsInCorpus(long id);

    /**
     * Gets a single corpus by its id.
     *
     * @param id
     * @return
     */
    public Corpus getCorpusById(long id);

    /**
     * Gets all documents that belong to the given corpus
     * @param corpusId
     * @return
     */
    public List<Document> getDocumentsByCorpusId(long corpusId);

    /**
     * Gets all corpora from the database
     *
     * @return
     */
    public List<Corpus> getAllCorpora();

    /**
     * Gets the data required for the world globus to render correctly.
     * @param documentId
     * @return
     */
    public List<GlobeTaxon> getGlobeDataForDocument(long documentId);

    /**
     * Gets many documents by their ids
     *
     * @return
     */
    public List<Document> getManyDocumentsByIds(List<Integer> documentIds);

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
    );

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
                                                          long corpusId);

    /**
     * Generic operation that fetches documents given the paramters
     *
     * @return
     */
    public Document getDocumentById(long id);

    public boolean checkIfGbifOccurrencesExist(long gbifTaxonId);

    /**
     * Gets a complete document, alongside its lists, from the database.
     *
     * @param id
     * @return
     */
    public Document getCompleteDocumentById(long id, int skipPages, int pageLimit);

    /**
     * Stores the complete document with all its lists in the database.
     *
     * @param document
     */
    public void saveDocument(Document document);

    /**
     * Stores a corpus in the database.
     *
     * @param corpus
     */
    public void saveCorpus(Corpus corpus);
}
