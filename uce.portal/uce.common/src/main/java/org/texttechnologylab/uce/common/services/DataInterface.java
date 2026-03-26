package org.texttechnologylab.uce.common.services;

import java.util.List;

import org.texttechnologylab.uce.common.exceptions.DatabaseOperationException;
import org.texttechnologylab.uce.common.exceptions.DocumentAccessDeniedException;
import org.texttechnologylab.uce.common.models.biofid.BiofidTaxon;
import org.texttechnologylab.uce.common.models.biofid.GazetteerTaxon;
import org.texttechnologylab.uce.common.models.biofid.GnFinderTaxon;
import org.texttechnologylab.uce.common.models.corpus.Corpus;
import org.texttechnologylab.uce.common.models.corpus.CorpusTsnePlot;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.corpus.GeoName;
import org.texttechnologylab.uce.common.models.corpus.KeywordDistribution;
import org.texttechnologylab.uce.common.models.corpus.Lemma;
import org.texttechnologylab.uce.common.models.corpus.LexiconEntry;
import org.texttechnologylab.uce.common.models.corpus.LexiconEntryId;
import org.texttechnologylab.uce.common.models.corpus.NamedEntity;
import org.texttechnologylab.uce.common.models.corpus.Page;
import org.texttechnologylab.uce.common.models.corpus.Sentence;
import org.texttechnologylab.uce.common.models.corpus.Time;
import org.texttechnologylab.uce.common.models.corpus.UCELog;
import org.texttechnologylab.uce.common.models.corpus.UCEMetadata;
import org.texttechnologylab.uce.common.models.corpus.UCEMetadataFilter;
import org.texttechnologylab.uce.common.models.corpus.links.AnnotationLink;
import org.texttechnologylab.uce.common.models.corpus.links.AnnotationToDocumentLink;
import org.texttechnologylab.uce.common.models.corpus.links.DocumentLink;
import org.texttechnologylab.uce.common.models.corpus.links.DocumentToAnnotationLink;
import org.texttechnologylab.uce.common.models.dto.UCEMetadataFilterDto;
import org.texttechnologylab.uce.common.models.gbif.GbifOccurrence;
import org.texttechnologylab.uce.common.models.globe.GlobeTaxon;
import org.texttechnologylab.uce.common.models.imp.ImportLog;
import org.texttechnologylab.uce.common.models.imp.UCEImport;
import org.texttechnologylab.uce.common.models.search.AnnotationSearchResult;
import org.texttechnologylab.uce.common.models.search.DocumentSearchResult;
import org.texttechnologylab.uce.common.models.search.OrderByColumn;
import org.texttechnologylab.uce.common.models.search.SearchLayer;
import org.texttechnologylab.uce.common.models.search.SearchOrder;

public interface DataInterface {

    /**
     * Fetches annotations (NE, Taxon, Time,...) of a given corpus.
     * @throws DocumentAccessDeniedException 
     */
    public List<AnnotationSearchResult> getAnnotationsOfCorpus(long corpusId, int skip, int take) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Returns all biofidurls (if any) of biofidtaxon that match the given string values.
     * @throws DocumentAccessDeniedException 
     */
    public List<String> getIdentifiableTaxonsByValue(String token) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Counts all documents within a given corpus
     * @throws DocumentAccessDeniedException 
     */
    public int countDocumentsInCorpus(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Returns true if the document with the given documentId exists in
     * the given corpus
     * @throws DocumentAccessDeniedException 
     */
    public boolean documentExists(long corpusId, String documentId) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a single corpus by its id.
     * @throws DocumentAccessDeniedException 
     */
    public Corpus getCorpusById(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Stores a page topic distribution by a page.
     * @throws DocumentAccessDeniedException 
     */
    public void savePageKeywordDistribution(Page page) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Stores a document topic distributions by a document.
     * @throws DocumentAccessDeniedException 
     */
    public void saveDocumentKeywordDistribution(Document document) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Returns a corpus by name. As they aren't unique, it returns the first match.
     * @throws DocumentAccessDeniedException 
     */
    public Corpus getCorpusByName(String name) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets all UCE filters of a corpus.
     * @throws DocumentAccessDeniedException 
     *
     */
    public List<UCEMetadataFilter> getUCEMetadataFiltersByCorpusId(long corpusId) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets all documents that belong to the given corpus
     * @throws DocumentAccessDeniedException 
     *
     */
    public List<Document> getDocumentsByCorpusId(long corpusId, int skip, int take) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets all DocumentLinks that belong to a document.
     * @throws DocumentAccessDeniedException 
     */
    public List<DocumentLink> getManyDocumentLinksOfDocument(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Get all DocumentLinks of a corpus that have either 'from' or 'to' as its documentId
     * @throws DocumentAccessDeniedException 
     */
    public List<DocumentLink> getManyDocumentLinksByDocumentId(String documentId, long corpusId) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets all documents of a corpus which aren't post-processed yet.
     * @throws DocumentAccessDeniedException 
     */
    public List<Document> getNonePostprocessedDocumentsByCorpusId(long corpusId) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Returns a corpus tsne plot by the given corpusId
     * @throws DocumentAccessDeniedException 
     */
    public CorpusTsnePlot getCorpusTsnePlotByCorpusId(long corpusId) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets all corpora from the database
     * @throws DocumentAccessDeniedException 
     */
    public List<Corpus> getAllCorpora() throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets the data required for the world globus to render correctly.
     * @throws DocumentAccessDeniedException 
     */
    public List<GlobeTaxon> getGlobeDataForDocument(long documentId) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets many documents by their ids
     * @throws DocumentAccessDeniedException 
     */
    public List<Document> getManyDocumentsByIds(List<Long> documentIds) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Returns a list of lexicon entries depending on the parameters.
     * @throws DocumentAccessDeniedException 
     */
    public List<LexiconEntry> getManyLexiconEntries(int skip, int take, List<String> alphabet,
                                                    List<String> annotationFilters, String sortColumn,
                                                    String sortOrder, String searchInput) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Does a semantic role label search and returns document hits
     * @throws DocumentAccessDeniedException 
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
    ) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Does a negation search and returns document hits
     * @throws DocumentAccessDeniedException 
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
                                                                   List<UCEMetadataFilterDto> filters)
        throws DatabaseOperationException, DocumentAccessDeniedException;
    /**
     * Searches for documents with a variety of criterias. It's the main db search of the biofid portal
     * The function calls a variety of stored procedures in the database.
     *
     * @param skip
     * @param take
     * @return
     * @throws DocumentAccessDeniedException 
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
                                                          String sourceTable,
                                                          List<String> expandedTerms
                                                          ) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a Topic Distribution determined by the T generic inheritance.
     *
     * @param clazz
     * @param id
     * @param <T>
     * @return
     * @throws DatabaseOperationException
     * @throws DocumentAccessDeniedException 
     */
    public <T extends KeywordDistribution> T getKeywordDistributionById(Class<T> clazz, long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Get Keyword Distributions by a keyword. This is basically a search for annotated keywords.
     *
     * @param clazz
     * @param topic
     * @param <T>
     * @return
     * @throws DatabaseOperationException
     * @throws DocumentAccessDeniedException 
     */
    public <T extends KeywordDistribution> List<T> getKeywordDistributionsByString(Class<T> clazz, String topic, int limit) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a document by its corpusId and the documentId, which isn't its primary key identifier "id".
     * @throws DocumentAccessDeniedException 
     * @throws NumberFormatException 
     */
    public Document getDocumentByCorpusAndDocumentId(long corpusId, String documentId) throws DatabaseOperationException, NumberFormatException, DocumentAccessDeniedException;

    public List<UCEMetadata> getUCEMetadataByDocumentId(long documentId) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a single UCEImport object from the database.
     * @throws DocumentAccessDeniedException 
     */
    public UCEImport getUceImportByImportId(String importId) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Generic operation that fetches documents given the parameters
     * @throws DocumentAccessDeniedException 
     */
    public Document getDocumentById(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a fully initialized page by its id.
     * @throws DocumentAccessDeniedException 
     */
    public Page getPageById(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a page by its documentid and whether the begin and end is in the page's begin and end.
     * @throws DocumentAccessDeniedException 
     */
    public Page getPageByDocumentIdAndBeginEnd(long documentId, int begin, int end, boolean initialize) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets the corresponding gbifOccurrences to a gbifTaxonId
     * @throws DocumentAccessDeniedException 
     */
    public List<GbifOccurrence> getGbifOccurrencesByGbifTaxonId(long gbifTaxonId) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a list of distinct documents that contain a named entity with a given covered text.
     * @param annotationName Either "namedEntities", "times", "sentences". It's the **list name** of the annotations within a Document objects.
     * @throws DocumentAccessDeniedException 
     */
    public List<Document> getDocumentsByAnnotationCoveredText(String coveredText, int limit, String annotationName) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets lemmas from a specific document that are within a begin and end range
     * @throws DocumentAccessDeniedException 
     *
     */
    public List<Lemma> getLemmasWithinBeginAndEndOfDocument(int begin, int end, long documentId) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a GeoName annotation by its unique id.
     * @throws DocumentAccessDeniedException 
     */
    public GeoName getGeoNameAnnotationById(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a time annotation by its id
     * @throws DocumentAccessDeniedException 
     */
    public Time getTimeAnnotationById(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Returns a sentence annotation by its id.
     * @throws DocumentAccessDeniedException 
     */
    public Sentence getSentenceAnnotationById(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Counts the entries in the lexicon
     * @throws DocumentAccessDeniedException 
     */
    public long countLexiconEntries() throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a Lexicon entry by its composite id.
     * @throws DocumentAccessDeniedException 
     */
    public LexiconEntry getLexiconEntryId(LexiconEntryId id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a named entity by its id
     * @throws DocumentAccessDeniedException 
     */
    public NamedEntity getNamedEntityById(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    public GazetteerTaxon getGazetteerTaxonById(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    public GnFinderTaxon getGnFinderTaxonById(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a single taxon by its id
     * @throws DocumentAccessDeniedException 
     */
    public BiofidTaxon getBiofidTaxonById(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a lemma by its id
     * @throws DocumentAccessDeniedException 
     */
    public Lemma getLemmaById(long id) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Given a string value, return a list of lemmas that match that value.
     * @throws DocumentAccessDeniedException 
     */
    public List<Lemma> getLemmasByValue(String covered, int limit, long documentId) throws DatabaseOperationException, DocumentAccessDeniedException;

    public boolean checkIfGbifOccurrencesExist(long gbifTaxonId) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Gets a complete document, alongside its lists, from the database.
     * @throws DocumentAccessDeniedException 
     */
    public Document getCompleteDocumentById(long id, int skipPages, int pageLimit) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Saves or updates an ImportLog belonging to a UCEImport.
     * @throws DocumentAccessDeniedException 
     */
    public void saveOrUpdateImportLog(ImportLog importLog) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Saves or updates a UCEImport object.
     * @throws DocumentAccessDeniedException 
     */
    public void saveOrUpdateUceImport(UCEImport uceImport) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Saves and updates a filter.
     * @throws DocumentAccessDeniedException 
     *
     */
    public void saveOrUpdateUCEMetadataFilter(UCEMetadataFilter filter) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Stores a new UCEMetadataFilter
     * @throws DocumentAccessDeniedException 
     *
     */
    public void saveUCEMetadataFilter(UCEMetadataFilter filter) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Stores the complete document with all its lists in the database.
     * @throws DocumentAccessDeniedException 
     *
     */
    public void saveDocument(Document document) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Updates a document
     * @throws DocumentAccessDeniedException 
     */
    public void updateDocument(Document document) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Saves a UCELog to the database. In those, we log requests from the user and more.
     *
     * @param log
     * @throws DatabaseOperationException
     * @throws DocumentAccessDeniedException 
     */
    public void saveUceLog(UCELog log) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Stores an corpus tsne plot instance
     *
     * @param corpusTsnePlot
     * @throws DocumentAccessDeniedException 
     */
    public void saveOrUpdateCorpusTsnePlot(CorpusTsnePlot corpusTsnePlot, Corpus corpus) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     *  Saves or updates a list of documentLinks.
     * @throws DocumentAccessDeniedException 
     */
    public void saveOrUpdateManyDocumentToAnnotationLinks(List<DocumentToAnnotationLink> links) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Saves or updates a list of annotation links.
     * @throws DocumentAccessDeniedException 
     */
    public void saveOrUpdateManyAnnotationLinks(List<AnnotationLink> links) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Saves or updates a list of DocumentToAnnotation Links
     * @throws DocumentAccessDeniedException 
     */
    public void saveOrUpdateManyAnnotationToDocumentLinks(List<AnnotationToDocumentLink> links) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Saves or updates a list of documentLinks.
     * @throws DocumentAccessDeniedException 
     */
    public void saveOrUpdateManyDocumentLinks(List<DocumentLink> documentLinks) throws DatabaseOperationException, DocumentAccessDeniedException;

    /**
     * Stores a corpus in the database.
     *
     * @param corpus
     * @throws DocumentAccessDeniedException 
     */
    public void saveCorpus(Corpus corpus) throws DatabaseOperationException, DocumentAccessDeniedException;
}
