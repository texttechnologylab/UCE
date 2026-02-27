package org.texttechnologylab.uce.search;

import com.google.gson.Gson;
import org.joda.time.DateTime;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.config.CorpusConfig;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.corpus.UCEMetadata;
import org.texttechnologylab.uce.common.models.corpus.UCEMetadataValueType;
import org.texttechnologylab.uce.common.models.dto.UCEMetadataFilterDto;
import org.texttechnologylab.uce.common.models.search.*;
import org.texttechnologylab.uce.common.states.KeywordInContextState;

import java.util.*;

import static java.util.stream.Collectors.groupingBy;

/**
 * A class that holds all states of a biofid search. We can use this class to serialize the search. It shouldn't hold any services.
 */
public class SearchState extends CacheItem {
    private CommonConfig config;
    private UUID searchId;
    private final DateTime created;
    private boolean proModeActivated;
    /**
     * The raw search phrase
     */
    private String searchQuery;
    private String enrichedSearchQuery;
    private boolean enrichedSearchQueryIsCutoff;
    private String dbSchema = "public";
    private String sourceTable = "page";
    private List<EnrichedSearchToken> enrichedSearchTokens;
    private List<String> searchTokens;
    private List<SearchLayer> searchLayers;
    private List<UCEMetadataFilterDto> uceMetadataFilters;
    private LayeredSearch layeredSearch;
    private SearchType searchType;
    private Integer currentPage = 1;
    private Integer take = 10;
    private long corpusId;
    private CorpusConfig corpusConfig;
    private Integer totalHits;
    private SearchOrder order = SearchOrder.DESC;
    private OrderByColumn orderBy = OrderByColumn.RANK;
    private KeywordInContextState keywordInContextState;
    private ArrayList<AnnotationSearchResult> foundNamedEntities = new ArrayList<>();
    private ArrayList<AnnotationSearchResult> foundTimes = new ArrayList<>();
    private ArrayList<AnnotationSearchResult> foundTaxons = new ArrayList<>();
    // Negation
    private ArrayList<AnnotationSearchResult> foundScopes = new ArrayList<>();
    private ArrayList<AnnotationSearchResult> foundCues = new ArrayList<>();
    private ArrayList<AnnotationSearchResult> foundXScopes = new ArrayList<>();
    private ArrayList<AnnotationSearchResult> foundFoci = new ArrayList<>();
    private ArrayList<AnnotationSearchResult> foundEvents = new ArrayList<>();

    /**
     * This is only filled when the search layer contains embeddings
     */
    private ArrayList<DocumentChunkEmbeddingSearchResult> foundDocumentChunkEmbeddings;
    private String primarySearchLayer;
    /**
     * These are the current, paginated list of documents
     */
    private List<Document> currentDocuments;
    /**
     * This is currently not used.
     */
    private List<Integer> currentDocumentHits;
    private HashMap<Integer, ArrayList<PageSnippet>> documentIdxToSnippet;
    private HashMap<Long, ArrayList<PageSnippet>> documentIdToSnippet;
    private HashMap<Integer, Float> documentIdxToRank;

    public SearchState(SearchType searchType) {
        this.searchType = searchType;
        this.searchId = UUID.randomUUID();
        this.created = DateTime.now();
        this.config = new CommonConfig();
    }

    public void dispose(){ }


    public boolean isEnrichedSearchQueryIsCutoff() {
        return enrichedSearchQueryIsCutoff;
    }

    public void setEnrichedSearchQueryIsCutoff(boolean enrichedSearchQueryIsCutoff) {
        this.enrichedSearchQueryIsCutoff = enrichedSearchQueryIsCutoff;
    }

    public LayeredSearch getLayeredSearch() {
        return layeredSearch;
    }

    public void setLayeredSearch(LayeredSearch layeredSearch) {
        this.layeredSearch = layeredSearch;
    }

    public String getDbSchema() {
        if(this.layeredSearch == null) return this.dbSchema;
        return "search";
    }

    public void setDbSchema(String dbSchema) {
        this.dbSchema = dbSchema;
    }

    public String getSourceTable() {
        if(this.layeredSearch == null) return this.sourceTable;
        return this.layeredSearch.getFinalLayerTableName();
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public List<EnrichedSearchToken> getEnrichedSearchTokens() {
        return enrichedSearchTokens;
    }

    public void setEnrichedSearchTokens(List<EnrichedSearchToken> enrichedSearchTokens) {
        this.enrichedSearchTokens = enrichedSearchTokens;
    }

    public boolean isProModeActivated() {
        return proModeActivated;
    }

    public void setProModeActivated(boolean proModeActivated) {
        this.proModeActivated = proModeActivated;
    }

    public String getEnrichedSearchQuery() {
        return enrichedSearchQuery;
    }

    public void setEnrichedSearchQuery(String enrichedSearchQuery) {
        this.enrichedSearchQuery = enrichedSearchQuery;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public float getPossibleRankOfDocumentIdx(Integer idx) {
        if (this.documentIdxToRank != null && this.documentIdxToRank.containsKey(idx))
            return this.documentIdxToRank.get(idx);
        return -1;
    }

    public void setDocumentIdxToRank(HashMap<Integer, Float> documentIdxToRank) {
        this.documentIdxToRank = documentIdxToRank;
    }

    public boolean hasUceMetadataFilters(){
        return getUceMetadataFilters() != null && !getUceMetadataFilters().isEmpty();
    }

    public List<UCEMetadataFilterDto> getUceMetadataFilters() {
        return uceMetadataFilters;
    }

    public void setUceMetadataFilters(List<UCEMetadataFilterDto> uceMetadataFilters) {
        this.uceMetadataFilters = uceMetadataFilters;
    }

    public ArrayList<PageSnippet> getPossibleSnippetsOfDocumentIdx(Integer idx) {
        if (this.documentIdxToSnippet != null && this.documentIdxToSnippet.containsKey(idx))
            return this.documentIdxToSnippet.get(idx);
        return null;
    }

    public ArrayList<PageSnippet> getPossibleSnippetsOfDocumentId(long id) {
        if (this.documentIdToSnippet != null && this.documentIdToSnippet.containsKey(id))
            return this.documentIdToSnippet.get(id);
        return null;
    }

    public DateTime getCreated() {
        return this.created;
    }

    public void setDocumentIdxToSnippets(HashMap<Integer, ArrayList<PageSnippet>> map) {
        this.documentIdxToSnippet = map;

        // Whenever we set documents within a fulltext search, we should have found snippets.
        // In those are pageIds of the snippets. Let's fill them.
        if(searchLayers != null && searchLayers.contains(SearchLayer.FULLTEXT)){
            for(var i =0; i < this.currentDocuments.size(); i++){
                var currentDoc = this.currentDocuments.get(i);
                var pageSnippets = this.getPossibleSnippetsOfDocumentIdx(i);
                if(pageSnippets == null) continue;
                for(var page:pageSnippets){
                    var potentialPage = currentDoc.getPages().stream().filter(p -> p.getId() == page.getPageId()).findFirst();
                    potentialPage.ifPresent(page::setPage);
                }
            }
        }
    }

    public void setDocumentIdToSnippets(HashMap<Long, ArrayList<PageSnippet>> map) {
        this.documentIdToSnippet = map;

        // Whenever we set documents within a fulltext search, we should have found snippets.
        // In those are pageIds of the snippets. Let's fill them.
        if(searchLayers != null && searchLayers.contains(SearchLayer.FULLTEXT)){
            for(var i =0; i < this.currentDocuments.size(); i++){
                var currentDoc = this.currentDocuments.get(i);
                var pageSnippets = this.getPossibleSnippetsOfDocumentIdx(i);
                if(pageSnippets == null) continue;
                for(var page:pageSnippets){
                    var potentialPage = currentDoc.getPages().stream().filter(p -> p.getId() == page.getPageId()).findFirst();
                    potentialPage.ifPresent(page::setPage);
                }
            }
        }
    }

    public List<Integer> getCurrentDocumentHits() {
        return currentDocumentHits;
    }

    public void setCurrentDocumentHits(List<Integer> currentDocumentHits) {
        this.currentDocumentHits = currentDocumentHits;
    }

    public CorpusConfig getCorpusConfig() {
        return corpusConfig;
    }

    public void setCorpusConfig(CorpusConfig corpusConfig) {
        this.corpusConfig = corpusConfig;
    }

    public KeywordInContextState getKeywordInContextState() {
        return keywordInContextState;
    }

    public void setKeywordInContextState(KeywordInContextState keywordInContextState) {
        this.keywordInContextState = keywordInContextState;
    }

    public void setPrimarySearchLayer(String primarySearchLayer) {
        this.primarySearchLayer = primarySearchLayer;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public ArrayList<AnnotationSearchResult> getFoundTimes() {
        return foundTimes;
    }

    public ArrayList<AnnotationSearchResult> getFoundTaxons() {
        return foundTaxons;
    }

    public ArrayList<DocumentChunkEmbeddingSearchResult> getFoundDocumentChunkEmbeddings() {
        return foundDocumentChunkEmbeddings;
    }

    public void setFoundDocumentChunkEmbeddings(ArrayList<DocumentChunkEmbeddingSearchResult> foundDocumentChunkEmbeddings) {
        this.foundDocumentChunkEmbeddings = foundDocumentChunkEmbeddings;
    }

    public long getCorpusId() {
        return corpusId;
    }

    public void setCorpusId(long corpusId) {
        this.corpusId = corpusId;
    }

    public SearchOrder getOrder() {
        return order;
    }

    public void setOrder(SearchOrder order) {
        this.order = order;
    }

    public OrderByColumn getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(OrderByColumn orderBy) {
        this.orderBy = orderBy;
    }

    public Integer getTotalPages() {
        if (totalHits < take) return 1;
        return (int) Math.ceil((double) totalHits / take);
    }

    public Integer getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(Integer totalHits) {
        this.totalHits = totalHits;
    }

    public int getSearchHitsOfDocument(int documentId) {
        try {
            var documentIdx = currentDocuments.indexOf(currentDocuments.stream().filter(d -> d.getId() == documentId).findFirst().get());
            return currentDocumentHits.get(documentIdx);
        } catch (Exception ex) {
            // This exception should never happen!
            return -1;
        }
    }

    /**
     * Returns the anootation type (NamedEntities, Taxons, Times etc.) of the given document
     */
    public List<AnnotationSearchResult> getAnnotationsByTypeAndDocumentId(String annotationType, Integer documentId, String neType) {
        List<AnnotationSearchResult> currentAnnotations = new ArrayList<>();
        switch (annotationType) {
            case "NamedEntities":
                currentAnnotations = getNamedEntitiesByType(neType, 0, 9999999);
                break;
            case "Taxons":
                currentAnnotations = foundTaxons;
                break;
            case "Times":
                currentAnnotations = foundTimes;
                break;
            case "Cues":
                currentAnnotations = foundCues;
                break;
            case "Scopes":
                currentAnnotations = foundScopes;
                break;
            case "xScopes":
                currentAnnotations = foundXScopes;
                break;
            case "Events":
                currentAnnotations = foundEvents;
                break;
            case "Foci":
                currentAnnotations = foundFoci;
                break;
        }
        currentAnnotations = currentAnnotations.stream().filter(a -> a.getDocumentId() == documentId).toList();
        currentAnnotations = currentAnnotations.stream().sorted(Comparator.comparingInt(AnnotationSearchResult::getOccurrences).reversed()).toList();
        return currentAnnotations;
    }

    public List<AnnotationSearchResult> getNamedEntitiesByType(String type, int skip, int take) {
        return foundNamedEntities.stream().filter(ne -> ne.getInfo().equals(type)).skip(skip).limit(take).toList();
    }

    public ArrayList<AnnotationSearchResult> getFoundNamedEntities() {
        return foundNamedEntities;
    }

    public void setFoundNamedEntities(ArrayList<AnnotationSearchResult> foundNamedEntities) {
        // We have so much wrong annotations like . or a - dont show those which are shorter than 2 characters.
        this.foundNamedEntities = new ArrayList<>(foundNamedEntities.stream().filter(e -> e.getCoveredText().length() > 2).sorted(Comparator.comparingInt(AnnotationSearchResult::getOccurrences).reversed()).toList());
    }

    public ArrayList<AnnotationSearchResult> getFoundTimes(int skip, int take) {
        return new ArrayList<>(foundTimes.stream().skip(skip).limit(take).toList());
    }

    public void setFoundTimes(ArrayList<AnnotationSearchResult> foundTimes) {
        this.foundTimes = new ArrayList<>(foundTimes.stream().filter(e -> e.getCoveredText().length() > 2).sorted(Comparator.comparingInt(AnnotationSearchResult::getOccurrences).reversed()).toList());
    }

    public ArrayList<AnnotationSearchResult> getFoundTaxons(int skip, int take) {
        return new ArrayList<>(foundTaxons.stream().skip(skip).limit(take).toList());
    }

    public void setFoundTaxons(ArrayList<AnnotationSearchResult> foundTaxons) {
        this.foundTaxons = new ArrayList<>(foundTaxons.stream().filter(e -> e.getCoveredText().length() > 2).sorted(Comparator.comparingInt(AnnotationSearchResult::getOccurrences).reversed()).toList());
    }

    public void setCurrentDocuments(List<Document> currentDocuments) {
        this.currentDocuments = currentDocuments;

        if (searchLayers != null && searchLayers.contains(SearchLayer.KEYWORDINCONTEXT)) {
            // Whenever we set new current documents, recalculate the context state
            if (keywordInContextState == null) keywordInContextState = new KeywordInContextState();
            keywordInContextState.recalculate(this.currentDocuments, this.searchTokens);
        }
    }

    public List<Document> getCurrentDocuments() {
        return currentDocuments;
    }

    public UUID getSearchId() {
        return searchId;
    }

    public void setSearchId(UUID searchId) {
        this.searchId = searchId;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public List<String> getSearchTokens() {
        return searchTokens;
    }

    public String getSearchTokensAsString() {
        if (this.searchTokens == null) return "";
        return String.join(" ", this.searchTokens.stream().map(s -> "[" + s + "]").toList());
    }

    public void setSearchTokens(List<String> searchTokens) {
        this.searchTokens = searchTokens;
    }

    public List<SearchLayer> getSearchLayers() {
        return searchLayers;
    }

    public void setSearchLayers(List<SearchLayer> searchLayers) {
        this.searchLayers = searchLayers;
        if (searchLayers.contains(SearchLayer.FULLTEXT)) primarySearchLayer = "Fulltext";
        else primarySearchLayer = "Named-Entities";
    }

    /**
     * TODO: This needs rework. Hardcoded names and the whole search layers are awkward. They have ben redesigned
     * too many times now.
     *
     * @return
     */
    public String getPrimarySearchLayer() {
        return this.primarySearchLayer == null ? "Semantic Roles" : this.primarySearchLayer;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getTake() {
        return take;
    }

    public void setTake(Integer take) {
        this.take = take;
    }

    public ArrayList<AnnotationSearchResult> getFoundScopes(int skip, int take) {
        return new ArrayList<>(foundScopes.stream().skip(skip).limit(take).toList());
    }

    public void setFoundScopes(ArrayList<AnnotationSearchResult> foundScopes) {
        this.foundScopes = foundScopes;
    }

    public ArrayList<AnnotationSearchResult> getFoundCues(int skip, int take) {
        return new ArrayList<>(foundCues.stream().skip(skip).limit(take).toList());
    }

    public void setFoundCues(ArrayList<AnnotationSearchResult> foundCues) {
        this.foundCues = foundCues;
    }

    public ArrayList<AnnotationSearchResult> getFoundXScopes(int skip, int take) {
        return new ArrayList<>(foundXScopes.stream().skip(skip).limit(take).toList());
    }

    public void setFoundXScopes(ArrayList<AnnotationSearchResult> foundXScopes) {
        this.foundXScopes = foundXScopes;
    }

    public ArrayList<AnnotationSearchResult> getFoundFoci(int skip, int take) {
        return new ArrayList<>(foundFoci.stream().skip(skip).limit(take).toList());
    }

    public void setFoundFoci(ArrayList<AnnotationSearchResult> foundFoci) {
        this.foundFoci = foundFoci;
    }

    public ArrayList<AnnotationSearchResult> getFoundEvents(int skip, int take) {
        return new ArrayList<>(foundEvents.stream().skip(skip).limit(take).toList());
    }

    public void setFoundEvents(ArrayList<AnnotationSearchResult> foundEvents) {
        this.foundEvents = foundEvents;
    }

    public String getVisualizationData() {
        // Prepare data for visualization
        // TODO this only consideres the documents in the current page of the search, this needs some adaptations in the inner search procedure to consider all documents...
        Map<String, List<UCEMetadata>> visualizationData = this
                .getCurrentDocuments()
                .stream()
                .map(Document::getUceMetadataWithoutJson)
                .flatMap(Collection::stream)
                .filter(m -> m.getValueType() == UCEMetadataValueType.NUMBER || m.getValueType() == UCEMetadataValueType.DATE)
                .collect(groupingBy(UCEMetadata::getKey));

        Map<String, Object> data = new HashMap<>();
        data.put("data", visualizationData);
        data.put("currentPage", this.getCurrentPage());
        return new Gson().toJson(data);
    }
}
