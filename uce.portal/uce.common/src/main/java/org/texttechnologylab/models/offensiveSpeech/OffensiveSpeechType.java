package org.texttechnologylab.models.offensiveSpeech;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "offensive_speech_type")
public class OffensiveSpeechType extends ModelBase {

    @Getter
    @Setter
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    public OffensiveSpeechType() {
    }

    public OffensiveSpeechType(String name) {
        this.name = name;
    }

}
