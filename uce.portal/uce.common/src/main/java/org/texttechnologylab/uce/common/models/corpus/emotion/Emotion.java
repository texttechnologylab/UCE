package org.texttechnologylab.uce.common.models.corpus.emotion;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.WikiModel;

import javax.persistence.*;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
@Entity
@Table(name = "emotion")
@Typesystem(types = {Emotion.class})
public class Emotion extends UIMAAnnotation implements WikiModel {

    private String model;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "emotion_id")
    private List<Feeling> feelings;

    public String generateEmotionMarker() {
        var tooltip = "";
        if (this.feelings != null && !this.feelings.isEmpty()) {
            for (var feeling : feelings) {
                tooltip += String.format(
                        Locale.US,
                        "<b>%1$s</b>: %2$.2f <br/>",
                        feeling.getFeeling(), feeling.getValue()
                );
            }
        }
        return String.format(
                "<span class='open-wiki-page annotation custom-context-menu annotation-marker emotion-marker' title='%1$s' data-wid='%2$s' data-wcovered='' " +
                "data-trigger='hover' data-toggle='popover' data-placement='bottom' data-html='true' data-content='%4$s'>%3$s</span>",
                this.getPrimaryFeeling(), this.getWikiId(), this.getPrimaryFeeling(), tooltip);
    }

    public double getPrimaryValue() {
        if (feelings == null || feelings.isEmpty()) return 0;
        return feelings.stream()
                .max(Comparator.comparingDouble(Feeling::getValue))
                .map(Feeling::getValue)
                .orElse(0.0);
    }

    public String getPrimaryFeeling() {
        if (feelings == null || feelings.isEmpty()) return "";
        return feelings.stream()
                .max(Comparator.comparingDouble(Feeling::getValue))
                .map(Feeling::getFeeling)
                .orElse("");
    }

    public Emotion() {
    }

    public Emotion(int begin, int end) {
        super(begin, end);
    }

    @Override
    public String getWikiId() {
        return "EM-" + this.getId();
    }
}
