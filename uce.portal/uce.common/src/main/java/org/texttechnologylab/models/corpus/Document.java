package org.texttechnologylab.models.corpus;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.texttechnologylab.models.Linkable;
import org.texttechnologylab.models.ModelBase;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.biofid.BiofidTaxon;
import org.texttechnologylab.models.corpus.links.DocumentLink;
import org.texttechnologylab.models.corpus.links.Link;
import org.texttechnologylab.models.negation.*;
import org.texttechnologylab.models.search.AnnotationSearchResult;
import org.texttechnologylab.models.search.PageSnippet;
import org.texttechnologylab.models.topic.TopicValueBase;
import org.texttechnologylab.models.topic.TopicValueBaseWithScore;
import org.texttechnologylab.models.topic.UnifiedTopic;
import org.texttechnologylab.utils.FreemarkerUtils;
import org.texttechnologylab.utils.StringUtils;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "document")
/*
The documents should be scanned and extracted via OCR. This is a base class for that.
 */
public class Document extends ModelBase implements WikiModel, Linkable {
    @Override
    public String getWikiId() {
        return "D" + "-" + this.getId();
    }

    @Override
    public List<Class<? extends ModelBase>> getCompatibleLinkTypes() {
        return List.of(DocumentLink.class);
    }

    @Override
    public long getPrimaryDbIdentifier() {
        return this.getId();
    }

    private String language;
    @Column(columnDefinition = "TEXT")
    private String documentTitle;
    private String documentId;
    private long corpusId;
    @Column(columnDefinition = "TEXT")
    private String fullText;
    @Column
    private boolean postProcessed;
    @Column(columnDefinition = "TEXT")
    private String fullTextCleaned;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @JoinColumn(name = "document_Id")
    private List<Page> pages;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Sentence> sentences;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<NamedEntity> namedEntities;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Lemma> lemmas;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<SrLink> srLinks;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Time> times;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Taxon> taxons;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<BiofidTaxon> biofidTaxons;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @BatchSize(size = 50)
    @JoinColumn(name = "document_Id")
    @Filter(name = "valueTypeFilter", condition = "valueType != 2")
    // Dont eagerly fetch the json metadata. That is way too costly probably.
    private List<UCEMetadata> uceMetadata;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<WikipediaLink> wikipediaLinks;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_id")
    private MetadataTitleInfo metadataTitleInfo;

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "document_id")
    private DocumentKeywordDistribution documentKeywordDistribution;

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private DocumentTopThreeTopics documentTopThreeTopics;
    // Negations:
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<CompleteNegation> completeNegations;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Cue> cues;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Event> events;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Focus> focuses;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Scope> scopes;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<XScope> xscopes;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<UnifiedTopic> unifiedTopics;

    public Document() {
        metadataTitleInfo = new MetadataTitleInfo();
    }

    public Document(String language, String documentTitle, String documentId, long corpusId) {
        this.language = language;
        this.documentTitle = documentTitle;
        this.documentId = documentId;
        this.corpusId = corpusId;
    }

    public List<BiofidTaxon> getBiofidTaxons() {
        return biofidTaxons;
    }

    public void setBiofidTaxons(List<BiofidTaxon> biofidTaxons) {
        this.biofidTaxons = biofidTaxons;
    }

    public boolean hasJsonUceMetadata() {
        return getUceMetadata().stream().anyMatch(u -> u.getValueType() == UCEMetadataValueType.JSON);
    }

    public List<UCEMetadata> getUceMetadataWithoutJson() {
        return getUceMetadata().stream().filter(u -> u.getValueType() != UCEMetadataValueType.JSON).toList();
    }

    public List<UCEMetadata> getUceMetadata() {
        if (uceMetadata == null) new ArrayList<>();
        uceMetadata.sort(Comparator.comparing(UCEMetadata::getValueType));
        return uceMetadata;
    }

    public void setUceMetadata(List<UCEMetadata> uceMetadata) {
        this.uceMetadata = uceMetadata;
    }

    public boolean isPostProcessed() {
        return postProcessed;
    }

    public void setPostProcessed(boolean postProcessed) {
        this.postProcessed = postProcessed;
    }

    public DocumentKeywordDistribution getDocumentKeywordDistribution() {
        return documentKeywordDistribution;
    }

    public void setDocumentKeywordDistribution(DocumentKeywordDistribution documentKeywordDistribution) {
        this.documentKeywordDistribution = documentKeywordDistribution;
    }

    public List<Lemma> getLemmas() {
        return lemmas;
    }

    public void setLemmas(List<Lemma> lemmas) {
        this.lemmas = lemmas;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public List<SrLink> getSrLinks() {
        return srLinks;
    }

    public void setSrLinks(List<SrLink> srLinks) {
        this.srLinks = srLinks;
    }

    public long getCorpusId() {
        return corpusId;
    }

    public void setCorpusId(long corpusId) {
        this.corpusId = corpusId;
    }

    public String getFullTextCleaned() {
        return fullTextCleaned;
    }

    public void setFullTextCleaned(String fullTextCleaned) {
        // Remove control characters: https://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
        fullTextCleaned = fullTextCleaned.replaceAll("\\p{Cntrl}", "");
        this.fullTextCleaned = fullTextCleaned;
    }

    public MetadataTitleInfo getMetadataTitleInfo() {
        return metadataTitleInfo;
    }

    public void setMetadataTitleInfo(MetadataTitleInfo metadataTitleInfo) {
        this.metadataTitleInfo = metadataTitleInfo;
    }

    public List<WikipediaLink> getWikipediaLinks() {
        return wikipediaLinks;
    }

    public void setWikipediaLinks(List<WikipediaLink> wikipediaLinks) {
        this.wikipediaLinks = wikipediaLinks;
    }

    public List<Taxon> getTaxons() {
        return taxons;
    }

    public void setTaxons(List<Taxon> taxons) {
        this.taxons = taxons;
    }

    public List<Time> getTimes() {
        return times;
    }

    public void setTimes(List<Time> times) {
        this.times = times;
    }

    public List<NamedEntity> getNamedEntities() {
        return namedEntities;
    }

    public void setNamedEntities(List<NamedEntity> namedEntities) {
        this.namedEntities = namedEntities;
    }

    public List<Sentence> getSentences() {
        return sentences;
    }

    public void setSentences(List<Sentence> sentences) {
        this.sentences = sentences;
    }

    public String getFullText() {
        return fullText;
    }

    public String getFullTextSnippet(int take) {
        if (fullText == null || fullText.isEmpty()) {
            return "";
        }
        var words = Arrays.stream(fullText.trim().split("\\s+")).toList();
        // Take the first 30 words
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (String word : words.stream().skip(words.size() / 3).toList()) { // We skip the entry as its the table of content
            result.append(word).append(" ");
            count++;
            if (count == take) {
                break;
            }
        }
        return result.toString().trim();
    }

    public String getFullTextSnippetCharOffset(int start, int end) {
        StringBuilder result = new StringBuilder();
        if (fullText != null) {
            int idx = 0;
            for (char c : fullText.toCharArray()) {
                if (idx >= start && idx < end) {
                    result.append(c);
                }
                idx++;
            }
        }
        return result.toString();
    }

    public String getFullTextSnippetAnnotationOffset(UIMAAnnotation annotation) {
        int offsetStart = Math.max(annotation.getBegin() - 150, 0);
        int offsetEnd = Math.min(annotation.getEnd() + 150, annotation.getBegin() + 500);
        String snippet = getFullTextSnippetCharOffset(offsetStart, offsetEnd);
        List<ArrayList<Integer>> offsetList = new ArrayList<>();
        ArrayList<Integer> offsetArray = new ArrayList<>();
        offsetArray.add(annotation.getBegin());
        offsetArray.add(annotation.getEnd());
        offsetList.add(offsetArray);
        return StringUtils.mergeBoldTags(StringUtils.addBoldTags(snippet, offsetList)).replaceAll("\n", "<br/>").replaceAll(" ", "&nbsp;");
    }
    // List<ArrayList<Integer>> offsetList
    public String getFullTextSnippetOffsetList(TemplateModel model) throws TemplateModelException {
        if (model instanceof TemplateSequenceModel) {
            List<ArrayList<Integer>> offsetList = FreemarkerUtils.convertToNestedIntegerList((TemplateSequenceModel) model);


            int minBegin = 999999999;
            int maxEnd = 0;
            for (ArrayList<Integer> offset : offsetList) {
                if (minBegin > offset.getFirst()) {
                    minBegin = offset.getFirst();
                }
                if (maxEnd < offset.getLast()) {
                    maxEnd = offset.getLast();
                }
            }
            for (ArrayList<Integer> pair : offsetList) {
                for (int i = 0; i < pair.size(); i++) {
                    pair.set(i, pair.get(i) - Math.max(minBegin - 100, 0));
                }
            }
            String snippet = getFullTextSnippetCharOffset(Math.max(minBegin - 100, 0), Math.min(maxEnd + 100, minBegin + 500));
            return StringUtils.mergeBoldTags(StringUtils.addBoldTags(snippet, offsetList)).replaceAll("\n", "<br/>").replaceAll(" ", "&nbsp;");
        }
        return null;
    }

    /**
     * Gets all objects of type UIMAAnnotation of this document
     *
     * @return
     */
    public List<UIMAAnnotation> getAllAnnotations(int pagesSkip, int pagesTake) {
        var pagesBegin = getPages().stream().skip(pagesSkip).limit(1).findFirst().get().getBegin();
        var pagesEnd = getPages().stream().skip(Math.min(pagesSkip + pagesTake, getPages().size() - 1)).limit(1).findFirst().get().getEnd();

        var annotations = new ArrayList<UIMAAnnotation>();
        annotations.addAll(taxons.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(namedEntities.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(times.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(wikipediaLinks.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(lemmas.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        // negations TODO: completeNegations do not have start and end so far -> could cause problems?
        annotations.addAll(completeNegations.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(cues.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(focuses.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(scopes.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(xscopes.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(events.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        // unifiedTopics
        annotations.addAll(unifiedTopics.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());

        annotations.sort(Comparator.comparingInt(UIMAAnnotation::getBegin));
        return annotations;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText.replaceAll("<", "");
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    public List<Page> getPages() {
        return pages.stream()
                .sorted(Comparator.comparingInt(Page::getPageNumber))
                .toList();
    }

    public List<Page> getPages(int take, int skip) {
        return pages.stream()
                .sorted(Comparator.comparingInt(Page::getPageNumber))
                .skip(skip)
                .limit(take)
                .collect(Collectors.toList());
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentTitle() {
        var title = metadataTitleInfo.getTitle() == null ? documentTitle : metadataTitleInfo.getTitle();
        return title == null ? "(Unbekannt)" : title;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getLanguage() {
        return language == null ? "-" : language;
    }

    public List<CompleteNegation> getCompleteNegations() {
        return completeNegations;
    }

    public void setCompleteNegations(List<CompleteNegation> completeNegations) {
        this.completeNegations = completeNegations;
    }

    public List<Cue> getCues() {
        return cues;
    }

    public void setCues(List<Cue> cues) {
        this.cues = cues;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public List<Focus> getFocuses() {
        return focuses;
    }

    public void setFocuses(List<Focus> focuses) {
        this.focuses = focuses;
    }

    public List<Scope> getScopes() {
        return scopes;
    }

    public void setScopes(List<Scope> scopes) {
        this.scopes = scopes;
    }

    public List<XScope> getXscopes() {
        return xscopes;
    }

    public void setXscopes(List<XScope> xscopes) {
        this.xscopes = xscopes;
    }

    public List<UnifiedTopic> getUnifiedTopics() {
        return unifiedTopics;
    }

    public void setUnifiedTopics(List<UnifiedTopic> unifiedTopics) {
        this.unifiedTopics = unifiedTopics;
    }

    public List<TopicValueBase> getDocumentUnifiedTopicDistribution(Integer topN) {
        List<TopicValueBaseWithScore> scoredTopics = new ArrayList<>();
        List<TopicValueBase> unscoredTopics = new ArrayList<>();

        // Separate scored and unscored topics using getRepresentativeTopic()
        for (UnifiedTopic unifiedTopic : unifiedTopics) {
            TopicValueBase representativeTopic = unifiedTopic.getRepresentativeTopic();

            if (representativeTopic != null) {
                if (representativeTopic instanceof TopicValueBaseWithScore) {
                    scoredTopics.add((TopicValueBaseWithScore) representativeTopic);
                } else {
                    unscoredTopics.add(representativeTopic);
                }
            }
        }

        // If scored topics exist, sort and return topN
        if (!scoredTopics.isEmpty()) {
            return scoredTopics.stream()
                    .sorted(Comparator.comparingDouble(TopicValueBaseWithScore::getScore).reversed())
                    .limit(topN)
                    .collect(Collectors.toList());
        }

        // If no scored topics, return topN unscored topics
        return unscoredTopics.stream()
                .limit(topN)
                .collect(Collectors.toList());
    }

    public DocumentTopThreeTopics getDocumentTopThreeTopics() {
        return documentTopThreeTopics;
    }

    public void setDocumentTopThreeTopics(DocumentTopThreeTopics documentTopThreeTopics) {
        this.documentTopThreeTopics = documentTopThreeTopics;
    }
}
