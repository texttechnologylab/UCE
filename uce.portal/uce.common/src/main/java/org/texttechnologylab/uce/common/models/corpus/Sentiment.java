package org.texttechnologylab.uce.common.models.corpus;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.annotation.SentimentModel;
import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.WikiModel;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
@Entity
@Table(name = "sentiment")
@Typesystem(types = {SentimentModel.class})
public class Sentiment extends UIMAAnnotation implements WikiModel {
    private double positive;
    private double neutral;
    private double negative;
    private String model;

    public double getPrimaryValue(){
        return Collections.max(List.of(this.positive, this.negative, this.neutral));
    }

    public String getPrimaryType() {
        if (positive == negative && negative == neutral) {
            return "neu"; // perfect tie
        }
        if (positive >= negative && positive >= neutral) {
            return "pos";
        } else if (negative >= positive && negative >= neutral) {
            return "neg";
        } else {
            return "neu";
        }
    }

    public Color getPrimaryColor(){
        var type = this.getPrimaryType();
        if(type.equals("neu")) return new Color(70, 130, 180, 255);
        else if(type.equals("neg")) return new Color(139, 0, 0, 255);
        else if(type.equals("pos")) return new Color(34, 139, 34, 255);
        else return new Color(0, 0, 0, 255);
    }

    public String generateSentimentMarker() {
        return String.format(
                "<span class='open-wiki-page annotation custom-context-menu annotation-marker sentiment-marker' title='%1$s' data-wid='%2$s' data-wcovered=''>S</span>",
                this.getPrimaryType(), this.getWikiId());
    }

    public String getDescription() {
        var model = this.getModel();
        if (model.contains("__")) model = model.split("__")[0];
        return String.format(
                Locale.US,
                "<b>Sentiment:</b> %1$s <br/><b>Neg:</b> %2$.2f - <b>Neu:</b> %3$.2f - <b>Pos:</b> %4$.2f <br/><b>Model:</b> %5$s",
                this.getPrimaryType(),
                this.getNegative(),
                this.getNeutral(),
                this.getPositive(),
                model
        );
    }

    public Sentiment(int begin, int end){
        super(begin, end);
    }

    public Sentiment(){ }

    @Override
    public String getWikiId() { return "SE-" + this.getId(); }
}
