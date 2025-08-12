package org.texttechnologylab.models.offensiveSpeech;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.*;

@Entity
@Table(name = "offensivespeech_value")
public class OffensiveSpeechValue extends ModelBase {

    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "offensivespeech_type_id", nullable = false)
    private OffensiveSpeechType offensiveSpeechType;

    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "offensivespeech_id", nullable = false)
    private OffensiveSpeech offensiveSpeech;

    @Getter
    @Setter
    @Column(name = "value", nullable = false)
    private double value;

    public OffensiveSpeechValue() {
    }

}
