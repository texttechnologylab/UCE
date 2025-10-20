package org.texttechnologylab.uce.common.models.corpus;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.texttechnologylab.models.authentication.DocumentPermission;
import org.texttechnologylab.uce.common.models.Linkable;
import org.texttechnologylab.uce.common.models.ModelBase;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.WikiModel;
import org.texttechnologylab.uce.common.models.biofid.BiofidTaxon;
import org.texttechnologylab.uce.common.models.biofid.GazetteerTaxon;
import org.texttechnologylab.uce.common.models.biofid.GnFinderTaxon;
import org.texttechnologylab.uce.common.models.corpus.emotion.Emotion;
import org.texttechnologylab.uce.common.models.corpus.links.AnnotationToDocumentLink;
import org.texttechnologylab.uce.common.models.corpus.links.DocumentLink;
import org.texttechnologylab.uce.common.models.corpus.links.DocumentToAnnotationLink;
import org.texttechnologylab.uce.common.models.negation.*;
import org.texttechnologylab.uce.common.models.topic.TopicValueBase;
import org.texttechnologylab.uce.common.models.topic.TopicValueBaseWithScore;
import org.texttechnologylab.uce.common.models.topic.UnifiedTopic;
import org.texttechnologylab.uce.common.utils.FreemarkerUtils;
import org.texttechnologylab.uce.common.utils.StringUtils;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return List.of(DocumentLink.class, DocumentToAnnotationLink.class, AnnotationToDocumentLink.class);
    }

    @Override
    public long getPrimaryDbIdentifier() {
        return this.getId();
    }

    @Setter
    private String language;
    @Setter
    @Column(columnDefinition = "TEXT")
    private String documentTitle;
    @Getter
    @Setter
    private String documentId;
    @Getter
    @Setter
    private long corpusId;
    @Getter
    @Column(columnDefinition = "TEXT")
    private String fullText;
    @Getter
    @Setter
    @Column
    private boolean postProcessed;
    @Getter
    @Column(columnDefinition = "TEXT")
    private String fullTextCleaned;

    @Setter
    private String mimeType;

    // Binary data for mime types that are not text, e.g. PDFs and images
    @Getter
    @Setter
    @Lob
    private byte[] documentData;

    @Setter
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @JoinColumn(name = "document_Id")
    private List<Page> pages;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Sentence> sentences;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<NamedEntity> namedEntities;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<GeoName> geoNames;

    @Setter
    @Getter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Sentiment> sentiments;

    @Setter
    @Getter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Emotion> emotions;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Lemma> lemmas;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<SrLink> srLinks;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Time> times;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<GazetteerTaxon> gazetteerTaxons;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<GnFinderTaxon> gnFinderTaxons;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<BiofidTaxon> biofidTaxons;

    @Setter
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @BatchSize(size = 50)
    @JoinColumn(name = "document_Id")
    @Filter(name = "valueTypeFilter", condition = "valueType != 2")
    private List<UCEMetadata> uceMetadata;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<WikipediaLink> wikipediaLinks;

    @Getter
    @Setter
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_id")
    private MetadataTitleInfo metadataTitleInfo;

    @Getter
    @Setter
    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "document_id")
    private DocumentKeywordDistribution documentKeywordDistribution;

    @Setter
    @Getter
    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private DocumentTopThreeTopics documentTopThreeTopics;
    // Negations:
    @Getter
    @Setter
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<CompleteNegation> completeNegations;

    @Setter
    @Getter
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Cue> cues;

    @Setter
    @Getter
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Event> events;

    @Setter
    @Getter
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Focus> focuses;

    @Setter
    @Getter
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Scope> scopes;

    @Setter
    @Getter
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<XScope> xscopes;

    @Setter
    @Getter
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<UnifiedTopic> unifiedTopics;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Image> images;

    @Getter
    @OneToMany(
            mappedBy = "document",
            cascade = CascadeType.ALL,      // propagate persist/remove
            orphanRemoval = true,           // remove children when not referenced
            fetch = FetchType.EAGER         // TODO permissions should be always loaded with the document
    )
    private Set<DocumentPermission> permissions = new HashSet<>();

    // NOTE permissions should always be set using these methods to ensure both sides of the relation are in sync
    public void addPermission(DocumentPermission permission) {
        permissions.add(permission);
        permission.setDocument(this);
    }

    public void removePermission(DocumentPermission permission) {
        permissions.remove(permission);
        permission.setDocument(null);
    }

    public Document() {
        metadataTitleInfo = new MetadataTitleInfo();
    }

    public Document(String language, String documentTitle, String documentId, long corpusId) {
        this.language = language;
        // DUUI often produces %20 instead of a space... can't change that, but it's ugly.
        this.documentTitle = documentTitle.replaceAll("%20", " ");
        this.documentId = documentId.replaceAll("%20", " ");
        this.corpusId = corpusId;
    }

    public String getMimeType() {
        // default to "text/plain" if no mime type is set, this prevents errors in the template comparisons
        if (mimeType == null) {
            return "text/plain";
        }
        return mimeType;
    }

    public String getDocumentDataBase64() {
        return Base64.getEncoder().encodeToString(documentData);
    }

    public boolean hasJsonUceMetadata() {
        return getUceMetadata().stream().anyMatch(u -> u.getValueType() == UCEMetadataValueType.JSON);
    }

    public List<UCEMetadata> getUceMetadataWithoutJson() {
        return getUceMetadata()
                .stream()
                .filter(u -> u.getValueType() != UCEMetadataValueType.JSON)
                .sorted(Comparator
                        .comparing(UCEMetadata::getValueType)
                        .thenComparing(filter -> {
                            // Try to extract a number in the beginning of the key
                            String key = filter.getKey();

                            // TODO this is a special case for Coh-Metrix, should be generalized
                            // TODO duplicated in "Corpus getUceMetadataFilters"
                            if (key.contains(":")) {
                                String[] parts = key.split(":");
                                if (parts.length > 1) {
                                    try {
                                        int number = Integer.parseInt(parts[0].trim());
                                        return String.format("%05d", number);
                                    } catch (NumberFormatException e) {
                                        // return the original key on error
                                    }
                                }
                            }

                            return key;
                        })
                )
                .toList();
    }

    public List<UCEMetadata> getUceMetadata() {
        if (uceMetadata == null) new ArrayList<>();
        uceMetadata.sort(Comparator.comparing(UCEMetadata::getValueType));
        return uceMetadata;
    }

    public void setFullTextCleaned(String fullTextCleaned) {
        // Remove control characters: https://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
        fullTextCleaned = fullTextCleaned.replaceAll("\\p{Cntrl}", "");
        this.fullTextCleaned = fullTextCleaned;
    }

    public String getFullTextSnippet(int take) {
        if (fullText == null || fullText.isEmpty()) {
            return "";
        }
        // Opening HTML Tags may cause the UI HTML to break!
        var cleaned = fullText.replace("<", "");
        var words = Arrays.stream(cleaned.trim().split("\\s+")).toList();
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
        return StringUtils.getHtmlText(StringUtils.mergeBoldTags(StringUtils.addBoldTags(snippet, offsetList)));
    }

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
            return StringUtils.getHtmlText(StringUtils.mergeBoldTags(StringUtils.addBoldTags(snippet, offsetList)));
        }

        return null;
    }

    public List<Taxon> getAllTaxa(){
        return Stream.concat(this.gazetteerTaxons.stream(), this.gnFinderTaxons.stream()).toList();
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
        annotations.addAll(gazetteerTaxons.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(gnFinderTaxons.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(namedEntities.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(geoNames.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(times.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        var pageOverlap = 40000;
        annotations.addAll(sentiments.stream().filter(a -> a.getBegin() + pageOverlap >= pagesBegin && a.getEnd() - pageOverlap <= pagesEnd).toList());
        annotations.addAll(emotions.stream().filter(a -> a.getBegin() + pageOverlap >= pagesBegin && a.getEnd() - pageOverlap <= pagesEnd).toList());
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
        annotations.addAll(images.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());

        annotations.sort(Comparator.comparingInt(UIMAAnnotation::getBegin));
        return annotations;
    }

    public void setFullText(String fullText) {
        // this.fullText = fullText.replaceAll("<", "");
        this.fullText = fullText;
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

    public String getDocumentTitle() {
        var title = metadataTitleInfo.getTitle() == null ? documentTitle : metadataTitleInfo.getTitle();
        return title == null ? "(Unbekannt)" : title;
    }

    public String getLanguage() {
        return language == null ? "-" : language;
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

}
