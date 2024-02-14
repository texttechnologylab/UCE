package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Paragraph extends UIMAAnnotation {

    private int leftIndent;
    private int rightIndent;
    private int startIndent;
    private String align;
    private int lineSpacing;
    private String coveredText;

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

    /**
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
        var ends = new ArrayList<Integer>();

        var finalText = new StringBuilder();

        // We go through each character and add the html build
        for(var i = 0; i < coveredText.length(); i++){
            var c = coveredText.charAt(i);

            // Get all annotations that start at this index
            var annos = beginToAnnotations.getOrDefault(i, new ArrayList<>());
            for(var a: annos){

                var cssClass = "";

                if(a instanceof NamedEntity ne){
                    cssClass = "ne-" + ne.getType();
                } else if (a instanceof Time time) {
                    cssClass = "time";
                } else if(a instanceof WikipediaLink wikipediaLink){
                    cssClass = "wiki";
                }

                finalText.append(String.format("<span class='annotation %1$s'>", cssClass));
                ends.add(a.getEnd() - offset);
            }

            // Append the original text as well.
            finalText.append(c);

            // Are there any end spans we need to close?
            for(var end: ends){
                if(end == i){
                    finalText.append("</span>");
                }
            }
        }
        return finalText.toString();








        /*
        var text = new StringBuffer(coveredText);
        var offset = getBegin();
        var addedLength = 0;
        annotations = annotations.stream().sorted(Comparator.comparingInt(UIMAAnnotation::getBegin)).collect(Collectors.toList());

        for (var annotation: annotations.stream().filter(a -> a.getBegin() >= getBegin() && a.getEnd() <= getEnd()).toList()){
            var begin = (annotation.getBegin() - offset) + addedLength;
            var end = (annotation.getEnd() - offset) + addedLength;

            // This means the NE is most likely corrupt, as a 40 character NE is highly unlikely
            if(end - begin > 50) continue;;

            var insertText = "";
            var cssClass = "";

            if(annotation instanceof NamedEntity ne){
                insertText = ne.getCoveredText();
                cssClass = "ne-" + ne.getType();
            } else if (annotation instanceof Time time) {
                insertText = time.getCoveredText();
                cssClass = "time";
            } else if(annotation instanceof WikipediaLink wikipediaLink){
                insertText = wikipediaLink.getCoveredText();
                cssClass = "wiki";
            }

            var startInsert = String.format("<span class='annotation %1$s'>", cssClass);
            var endInsert = "</span>";
            var insert = startInsert + insertText + endInsert;

            text = text.replace(begin, end, insert);
            addedLength += startInsert.length() + endInsert.length();
        }

        return text.toString();*/
    }
}
