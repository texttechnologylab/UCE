package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.annotations.Searchable;
import org.texttechnologylab.uce.common.models.ModelBase;
import org.texttechnologylab.uce.common.models.WikiModel;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class KeywordDistribution extends ModelBase implements WikiModel {

    // The following properties aren't really "topics". They are quick and
    // easy ways to determine the keywords and sentences from a text.
    // Later, we want better topic modelling built into DUUI, but for now,
    // we work with that.

    // RAKE is a simple key phrase extraction, hence these are probably phrases.
    @Column(columnDefinition = "TEXT")
    private String rakeTopicOne;
    @Column(columnDefinition = "TEXT")
    private String rakeTopicTwo;
    @Column(columnDefinition = "TEXT")
    private String rakeTopicThree;

    // YAKE on the other hand does keyword extraction and hence these are words.
    @Searchable
    private String yakeTopicOne;
    @Searchable
    private String yakeTopicTwo;
    @Searchable
    private String yakeTopicThree;
    @Searchable
    private String yakeTopicFour;
    @Searchable
    private String yakeTopicFive;

    public String toString(){
        return rakeTopicOne + " "
                + rakeTopicTwo + " "
                + rakeTopicThree + " "
                + yakeTopicOne + " "
                + yakeTopicTwo + " "
                + yakeTopicThree + " "
                + yakeTopicFour + " "
                + yakeTopicFive + " ";
    }

    public String getRakeTopicOne() {
        return rakeTopicOne;
    }

    public void setRakeTopicOne(String rakeTopicOne) {
        this.rakeTopicOne = rakeTopicOne;
    }

    public String getRakeTopicTwo() {
        return rakeTopicTwo;
    }

    public void setRakeTopicTwo(String rakeTopicTwo) {
        this.rakeTopicTwo = rakeTopicTwo;
    }

    public String getRakeTopicThree() {
        return rakeTopicThree;
    }

    public void setRakeTopicThree(String rakeTopicThree) {
        this.rakeTopicThree = rakeTopicThree;
    }

    public String getYakeTopicOne() {
        return yakeTopicOne;
    }

    public void setYakeTopicOne(String yakeTopicOne) {
        this.yakeTopicOne = yakeTopicOne;
    }

    public String getYakeTopicTwo() {
        return yakeTopicTwo;
    }

    public void setYakeTopicTwo(String yakeTopicTwo) {
        this.yakeTopicTwo = yakeTopicTwo;
    }

    public String getYakeTopicThree() {
        return yakeTopicThree;
    }

    public void setYakeTopicThree(String yakeTopicThree) {
        this.yakeTopicThree = yakeTopicThree;
    }

    public String getYakeTopicFour() {
        return yakeTopicFour;
    }

    public void setYakeTopicFour(String yakeTopicFour) {
        this.yakeTopicFour = yakeTopicFour;
    }

    public String getYakeTopicFive() {
        return yakeTopicFive;
    }

    public void setYakeTopicFive(String yakeTopicFive) {
        this.yakeTopicFive = yakeTopicFive;
    }

    @Override
    public String getWikiId() {
        return "T" + this.getClass().getSimpleName().charAt(0) + "-" + this.getId();
    }
}
