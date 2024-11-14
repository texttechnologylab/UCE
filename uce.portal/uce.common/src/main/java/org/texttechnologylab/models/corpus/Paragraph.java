package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name="paragraph")
public class Paragraph extends UIMAAnnotation {

    private int leftIndent;
    private int rightIndent;
    private int startIndent;
    private String align;
    private int lineSpacing;
    @Column(columnDefinition = "TEXT")
    private String coveredText;

    public Paragraph(){
        super(-1, -1);
    }

    public Paragraph(int begin, int end){
        super(begin, end);
    }

    public String getCoveredText() {
        return coveredText;
    }
    public void setCoveredText(String coveredText) {
        this.coveredText = coveredText;
    }

    public int getLeftIndent() {
        return leftIndent;
    }
    public void setLeftIndent(int leftIndent) {
        this.leftIndent = leftIndent;
    }

    public int getRightIndent() {
        return rightIndent;
    }
    public void setRightIndent(int rightIndent) {
        this.rightIndent = rightIndent;
    }

    public int getStartIndent() {
        return startIndent;
    }
    public void setStartIndent(int startIndent) {
        this.startIndent = startIndent;
    }

    public String getAlign() {
        return align;
    }
    public void setAlign(String align) {
        this.align = align;
    }

    public int getLineSpacing() {
        return lineSpacing;
    }
    public void setLineSpacing(int lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public String getFontWeight(){
        return Objects.equals(getAlign().toLowerCase(), "center") ? "bold" : "inherit";
    }

    public String getUnderlined(){
        return Objects.equals(getAlign().toLowerCase(), "center") ? "underline" : "inherit";
    }

    /**TODO: This and Page.buildHtmLstring() needs to be redone and adjusted to new settings. As of now it sucks.
     * Gets an HTML string of this blocks text to simply add to the UI
     * @return
     */
    public String buildHTMLString(List<UIMAAnnotation> annotations){
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

                if (a instanceof NamedEntity ne) {
                    html = String.format(
                            "<span class='open-wiki-page annotation custom-context-menu ne-%1$s' title='%2$s' data-wid='%3$s' data-wcovered='%4$s'>",
                            ne.getType(),
                            ne.getCoveredText(),
                            ne.getWikiId(),
                            ne.getCoveredText());
                    ends.add(new AbstractMap.SimpleEntry<>(a.getEnd() - offset, "</span>"));
                } else if (a instanceof Time time) {
                    html = String.format(
                            "<span class='open-wiki-page annotation custom-context-menu time' title='%1$s' data-wid='%2$s' data-wcovered='%3$s'>",
                            time.getCoveredText(),
                            time.getWikiId(),
                            time.getCoveredText());
                    ends.add(new AbstractMap.SimpleEntry<>(a.getEnd() - offset, "</span>"));
                } else if (a instanceof WikipediaLink wikipediaLink) {
                    html = String.format(
                            "<span class='open-wiki-page annotation custom-context-menu wiki' title='%1$s'>",
                            wikipediaLink.getCoveredText());
                    ends.add(new AbstractMap.SimpleEntry<>(a.getEnd() - offset, "</span>"));
                } else if (a instanceof Taxon taxon) {
                    html = String.format(
                            "<span class='open-wiki-page annotation custom-context-menu taxon' title='%1$s' data-wid='%2$s' data-wcovered='%3$s'>",
                            taxon.getCoveredText(),
                            taxon.getWikiId(),
                            taxon.getCoveredText());
                    ends.add(new AbstractMap.SimpleEntry<>(a.getEnd() - offset, "</span>"));
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
        return finalText.toString();
    }
}
