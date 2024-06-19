package org.texttechnologylab;

import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.search.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * A class that holds all states of a biofid search. We can use this class to serialize the search. It shouldn't hold any services.
 */
public class SearchState {
    private UUID searchId;

    /**
     * The raw search phrase
     */
    private String searchPhrase;
    private List<String> searchTokens;
    private List<SearchLayer> searchLayers;
    private SearchType searchType;
    private Integer currentPage = 1;
    private Integer take = 10;
    private long corpusId;
    private Integer totalHits;
    private SearchOrder order = SearchOrder.ASC;
    private OrderByColumn orderBy = OrderByColumn.TITLE;
    private ArrayList<AnnotationSearchResult> foundNamedEntities;
    private ArrayList<AnnotationSearchResult> foundTimes;
    private ArrayList<AnnotationSearchResult> foundTaxons;

    /**
     * This is only filled when the search layer contains embeddings
     */
    private ArrayList<DocumentChunkEmbeddingSearchResult> foundDocumentChunkEmbeddings;

    private String primarySearchLayer;

    /**
     * These are the current, paginated list of documents
     */
    private List<Document> currentDocuments;

    public SearchState(SearchType searchType){
        this.searchType = searchType;
        this.searchId = UUID.randomUUID();
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

    public Integer getTotalPages(){
        if(totalHits < take) return 1;
        return (int) Math.ceil((double) totalHits / take);
    }

    public Integer getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(Integer totalHits) {
        this.totalHits = totalHits;
    }

    /**
     * Returns the anootation type (NamedEntities, Taxons, Times etc.) of the given document
     */
    public List<AnnotationSearchResult> getAnnotationsByTypeAndDocumentId(String annotationType, Integer documentId, String neType){
        List<AnnotationSearchResult> currentAnnotations = new ArrayList<>();
        switch (annotationType){
            case "NamedEntities": currentAnnotations = getNamedEntitiesByType(neType, 0, 9999999); break;
            case "Taxons": currentAnnotations = foundTaxons; break;
            case "Times": currentAnnotations = foundTimes; break;
        }
        currentAnnotations = currentAnnotations.stream().filter(a -> a.getDocumentId() == documentId).toList();
        currentAnnotations = currentAnnotations.stream().sorted(Comparator.comparingInt(AnnotationSearchResult::getOccurrences).reversed()).toList();
        return currentAnnotations;
    }

    public List<AnnotationSearchResult> getNamedEntitiesByType(String type, int skip, int take){
        return  foundNamedEntities.stream().filter(ne -> ne.getInfo().equals(type)).skip(skip).limit(take).toList();
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

    public String getSearchPhrase() {
        return searchPhrase;
    }

    public void setSearchPhrase(String searchPhrase) {
        this.searchPhrase = searchPhrase;
    }

    public List<String> getSearchTokens() {
        return searchTokens;
    }

    public void setSearchTokens(List<String> searchTokens) {
        this.searchTokens = searchTokens;
    }

    public List<SearchLayer> getSearchLayers() {
        return searchLayers;
    }

    public void setSearchLayers(List<SearchLayer> searchLayers) {
        this.searchLayers = searchLayers;
        if(searchLayers.contains(SearchLayer.METADATA)) primarySearchLayer = "Meta";
        else primarySearchLayer = "Named-Entities";
    }

    /**
     * TODO: This needs rework. Hardcoded names and the whole search layers are awkward. They have ben redesigned
     * too many times now.
     * @return
     */
    public String getPrimarySearchLayer(){
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
}
