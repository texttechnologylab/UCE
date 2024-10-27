package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.utils.StringUtils;

import javax.persistence.*;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name="page")
public class Page extends UIMAAnnotation {

    private int pageNumber;
    private String pageId;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="page_Id")
    private List<Paragraph> paragraphs;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="page_Id")
    private List<Block> blocks;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="page_Id")
    private List<Line> lines;

    @OneToOne(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private PageTopicDistribution pageTopicDistribution;

    public Page(int begin, int end, int pageNumber, String pageId){
        super(begin, end);
        this.pageNumber = pageNumber;
        this.pageId = pageId;
    }
    public Page(){
        super(-1, -1);
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public PageTopicDistribution getPageTopicDistribution() {
        return pageTopicDistribution;
    }

    public void setPageTopicDistribution(PageTopicDistribution pageTopicDistributions) {
        this.pageTopicDistribution = pageTopicDistributions;
    }

    public List<Line> getLines() {
        return lines;
    }
    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public List<Paragraph> getParagraphs() {
        try{
            return paragraphs;
        } catch (Exception ex){
            // I have no idea why, but sometimes, the lazy load of empty paragraphs throws an error.
            // There is nothing wrong with the document or page - it just throws an error here.
            // It's not a problem if the paragraphs are empty! So we just catch the error and return empty...
            // It's also not worth logging to the DB or file logger.
            System.err.println("Opened a document with unloadable lazy paragraphs.");
            return new ArrayList<>();
        }
    }
    public void setParagraphs(List<Paragraph> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public List<Block> getBlocks() {
        return blocks;
    }
    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public String getPageId() {
        return pageId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getCoveredText(String fullDocumentText){
        var length = fullDocumentText.length();
        return fullDocumentText.substring(getBegin(), Math.min(getEnd(), length));
    }

    /**
     * TODO: This and Paragraph.buildHtmLstring() needs to be redone and adjusted to new settings. As of now it sucks.
     * @param annotations
     * @param coveredText
     * @return
     */
    public String buildHTMLString(List<UIMAAnnotation> annotations, String coveredText){
        coveredText = getCoveredText(coveredText);

        var offset = getBegin();
        // Here we store each annotation with its begin index
        var beginToAnnotations = new HashMap<Integer, List<UIMAAnnotation>>();
        for(var annotation: annotations.stream().filter(a -> a.getBegin() >= getBegin() && a.getEnd() <= getEnd()).toList()){
            if(annotation.getBegin() == annotation.getEnd()) continue;
            beginToAnnotations.put(
                    annotation.getBegin() - offset,
                    Stream.concat(
                            beginToAnnotations.getOrDefault(annotation.getBegin(), new ArrayList<>()).stream(),
                            Stream.of(annotation)).collect(Collectors.toList()));
        }
        // In here we store all possible ends
        var ends = new ArrayList<Map.Entry<Integer, String>>();

        var finalText = new StringBuilder();

        // We go through each character and add the html build
        for(var i = 0; i < coveredText.length(); i++){
            var c = coveredText.charAt(i);

            // Get all annotations that start at this index
            var annos = beginToAnnotations.getOrDefault(i, new ArrayList<>());
            for(var a: annos){

                var html = "";

                if(a instanceof NamedEntity ne){
                    html = String.format("<span class='annotation custom-context-menu ne-%1$s' title='%2$s'>", ne.getType(), ne.getCoveredText());
                    ends.add(new AbstractMap.SimpleEntry<>(a.getEnd() - offset, "</span>"));
                } else if (a instanceof Time time) {
                    html = String.format("<span class='annotation custom-context-menu time' title='%1$s'>", time.getCoveredText());
                    ends.add(new AbstractMap.SimpleEntry<>(a.getEnd() - offset, "</span>"));
                } else if(a instanceof WikipediaLink wikipediaLink){
                    html = String.format("<span class='annotation custom-context-menu wiki' title='%2$s'>", wikipediaLink.getCoveredText());
                    ends.add(new AbstractMap.SimpleEntry<>(a.getEnd() - offset, "</span>"));
                } else if(a instanceof Taxon taxon){
                    html = String.format("<a class='annotation custom-context-menu taxon' href='%1$s' target='_blank' title='%2$s'>", taxon.getValue(), taxon.getCoveredText());
                    ends.add(new AbstractMap.SimpleEntry<>(a.getEnd() - offset, "</a><i class='mr-1 fas fa-external-link-alt'></i>"));
                }
                finalText.append(html);
            }

            // Append the original text as well.
            finalText.append(c);

            // Are there any end spans we need to close?
            for(var end: ends){
                if(end.getKey() == i){
                    finalText.append(end.getValue());
                }
            }
        }

        var text = finalText.toString();
        // At the end, we may postprocess the text into a more readable format. A page may otherwise be very compact.
        text = StringUtils.AddLineBreaks(StringUtils.CleanText(text), text.length());

        return text;
    }
}
