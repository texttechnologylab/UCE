package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

import java.util.List;

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
    public String buildHTMLString(List<NamedEntity> namedEntities){
        var text = new StringBuffer(coveredText);
        var offset = getBegin();
        var addedLength = 0;

        for (var ne: namedEntities.stream().filter(ne -> ne.getBegin() >= getBegin() && ne.getEnd() <= getEnd()).toList()){
            var begin = (ne.getBegin() - offset) + addedLength;
            var end = (ne.getEnd() - offset) + addedLength;

            // This means the NE is most likely corrupt, as a 40 character NE is highly unlikely
            if(end - begin > 50) continue;;

            var startInsert = String.format("<span class='ne ne-%1$s'>", ne.getType());
            var endInsert = "</span>";
            var insert = startInsert + ne.getCoveredText() + endInsert;

            text = text.replace(begin, end, insert);
            addedLength += startInsert.length() + endInsert.length();
        }

        return text.toString();
    }
}
