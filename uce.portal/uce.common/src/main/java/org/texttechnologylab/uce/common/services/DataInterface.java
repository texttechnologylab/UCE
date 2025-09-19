package org.texttechnologylab.uce.common.services;

import org.texttechnologylab.uce.common.exceptions.DatabaseOperationException;
import org.texttechnologylab.uce.common.models.authentication.UceUser;
import org.texttechnologylab.uce.common.models.biofid.BiofidTaxon;
import org.texttechnologylab.uce.common.models.biofid.GazetteerTaxon;
import org.texttechnologylab.uce.common.models.biofid.GnFinderTaxon;
import org.texttechnologylab.uce.common.models.corpus.*;
import org.texttechnologylab.uce.common.models.corpus.links.AnnotationLink;
import org.texttechnologylab.uce.common.models.corpus.links.AnnotationToDocumentLink;
import org.texttechnologylab.uce.common.models.corpus.links.DocumentLink;
import org.texttechnologylab.uce.common.models.corpus.links.DocumentToAnnotationLink;
import org.texttechnologylab.uce.common.models.dto.UCEMetadataFilterDto;
import org.texttechnologylab.uce.common.models.gbif.GbifOccurrence;
import org.texttechnologylab.uce.common.models.globe.GlobeTaxon;
import org.texttechnologylab.uce.common.models.imp.ImportLog;
import org.texttechnologylab.uce.common.models.imp.UCEImport;
import org.texttechnologylab.uce.common.models.search.*;

import java.util.List;

public interface DataInterface {

    /**
     * Fetches annotations (NE, Taxon, Time,...) of a given corpus.
     */
    public List<AnnotationSearchResult> getAnnotationsOfCorpus(long corpusId, int skip, int take) throws DatabaseOperationException;

    /**
     * Returns all biofidurls (if any) of biofidtaxon that match the given string values.
     */
    public List<String> getIdentifiableTaxonsByValue(String token) throws DatabaseOperationException;

    /**
     * Counts all documents within a given corpus
     */
    public int countDocumentsInCorpus(long id) throws DatabaseOperationException;

    /**
     * Returns true if the document with the given documentId exists in
     * the given corpus
     */
    public boolean documentExists(long corpusId, String documentId) throws DatabaseOperationException;

    /**
     * Gets a single corpus by its id.
     */
    public Corpus getCorpusById(long id) throws DatabaseOperationException;

    /**
     * Stores a page topic distribution by a page.
     */
    public void savePageKeywordDistribution(Page page) throws DatabaseOperationException;

    /**
     * Stores a document topic distributions by a document.
     */
    public void saveDocumentKeywordDistribution(Document document) throws DatabaseOperationException;

    /**
     * Returns a corpus by name. As they aren't unique, it returns the first match.
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
    public List<Document> getDocumentsByCorpusId(long corpusId, int skip, int take, UceUser user) throws DatabaseOperationException;

    /**
     * Gets all DocumentLinks that belong to a document.
     */
    public List<DocumentLink> getManyDocumentLinksOfDocument(long id) throws DatabaseOperationException;

    /**
     * Get all DocumentLinks of a corpus that have either 'from' or 'to' as its documentId
     */
    public List<DocumentLink> getManyDocumentLinksByDocumentId(String documentId, long corpusId) throws DatabaseOperationException;

    /**
     * Gets all documents of a corpus which aren't post-processed yet.
     */
    public List<Document> getNonePostprocessedDocumentsByCorpusId(long corpusId) throws DatabaseOperationException;

    /**
     * Returns a corpus tsne plot by the given corpusId
     */
    public CorpusTsnePlot getCorpusTsnePlotByCorpusId(long corpusId) throws DatabaseOperationException;

    /**
     * Gets all corpora from the database
     */
    public List<Corpus> getAllCorpora() throws DatabaseOperationException;

    /**
     * Gets the data required for the world globus to render correctly.
     */
    public List<GlobeTaxon> getGlobeDataForDocument(long documentId) throws DatabaseOperationException;

    /**
     * Gets many documents by their ids
     */
    public List<Document> getManyDocumentsByIds(List<Integer> documentIds) throws DatabaseOperationException;

    /**
     * Returns a list of lexicon entries depending on the parameters.
     */
    public List<LexiconEntry> getManyLexiconEntries(int skip, int take, List<String> alphabet,
                                                    List<String> annotationFilters, String sortColumn,
                                                    String sortOrder, String searchInput) throws DatabaseOperationException;

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
     * Does a negation search and returns document hits
     */
    public DocumentSearchResult completeNegationSearchForDocuments(int skip,
                                                                   int take,
                                                                   List<String> cue,
                                                                   List<String> event,
                                                                   List<String> focus,
                                                                   List<String> scope,
                                                                   List<String> xscope,
                                                                   boolean countAll,
                                                                   SearchOrder order,
                                                                   OrderByColumn orderedByColumn,
                                                                   long corpusId,
                                                                   List<UCEMetadataFilterDto> filters,
                                                                   UceUser user)
        throws DatabaseOperationException;
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
    public <T extends KeywordDistribution> T getKeywordDistributionById(Class<T> clazz, long id) throws DatabaseOperationException;

    /**
     * Get Keyword Distributions by a keyword. This is basically a search for annotated keywords.
     *
     * @param clazz
     * @param topic
     * @param <T>
     * @return
     * @throws DatabaseOperationException
     */
    public <T extends KeywordDistribution> List<T> getKeywordDistributionsByString(Class<T> clazz, String topic, int limit) throws DatabaseOperationException;

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
     * Gets a fully initialized page by its id.
     */
    public Page getPageById(long id) throws DatabaseOperationException;

    /**
     * Gets a page by its documentid and whether the begin and end is in the page's begin and end.
     */
    public Page getPageByDocumentIdAndBeginEnd(long documentId, int begin, int end, boolean initialize) throws DatabaseOperationException;

    /**
     * Gets the corresponding gbifOccurrences to a gbifTaxonId
     */
    public List<GbifOccurrence> getGbifOccurrencesByGbifTaxonId(long gbifTaxonId) throws DatabaseOperationException;

    /**
     * Gets a list of distinct documents that contain a named entity with a given covered text.
     * @param annotationName Either "namedEntities", "times", "sentences". It's the **list name** of the annotations within a Document objects.
     */
    public List<Document> getDocumentsByAnnotationCoveredText(String coveredText, int limit, String annotationName) throws DatabaseOperationException;

    /**
     * Gets lemmas from a specific document that are within a begin and end range
     *
     */
    public List<Lemma> getLemmasWithinBeginAndEndOfDocument(int begin, int end, long documentId) throws DatabaseOperationException;

    /**
     * Gets a GeoName annotation by its unique id.
     */
    public GeoName getGeoNameAnnotationById(long id) throws DatabaseOperationException;

    /**
     * Gets a time annotation by its id
     */
    public Time getTimeAnnotationById(long id) throws DatabaseOperationException;

    /**
     * Returns a sentence annotation by its id.
     */
    public Sentence getSentenceAnnotationById(long id) throws DatabaseOperationException;

    /**
     * Counts the entries in the lexicon
     */
    public long countLexiconEntries() throws DatabaseOperationException;

    /**
     * Gets a Lexicon entry by its composite id.
     */
    public LexiconEntry getLexiconEntryId(LexiconEntryId id) throws DatabaseOperationException;

    /**
     * Gets a named entity by its id
     */
    public NamedEntity getNamedEntityById(long id) throws DatabaseOperationException;

    public GazetteerTaxon getGazetteerTaxonById(long id) throws DatabaseOperationException;

    public GnFinderTaxon getGnFinderTaxonById(long id) throws DatabaseOperationException;

    /**
     * Gets a single taxon by its id
     */
    public BiofidTaxon getBiofidTaxonById(long id) throws DatabaseOperationException;

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
    public Document getCompleteDocumentById(long id, int skipPages, int pageLimit, UceUser user) throws DatabaseOperationException;

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
     *  Saves or updates a list of documentLinks.
     */
    public void saveOrUpdateManyDocumentToAnnotationLinks(List<DocumentToAnnotationLink> links) throws DatabaseOperationException;

    /**
     * Saves or updates a list of annotation links.
     */
    public void saveOrUpdateManyAnnotationLinks(List<AnnotationLink> links) throws DatabaseOperationException;

    /**
     * Saves or updates a list of DocumentToAnnotation Links
     */
    public void saveOrUpdateManyAnnotationToDocumentLinks(List<AnnotationToDocumentLink> links) throws DatabaseOperationException;

    /**
     * Saves or updates a list of documentLinks.
     */
    public void saveOrUpdateManyDocumentLinks(List<DocumentLink> documentLinks) throws DatabaseOperationException;

    /**
     * Stores a corpus in the database.
     *
     * @param corpus
     */
    public void saveCorpus(Corpus corpus) throws DatabaseOperationException;
}
