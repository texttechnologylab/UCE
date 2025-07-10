package org.texttechnologylab.models.emotion;

import lombok.Getter;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "emotion_type")
public class EmotionType extends ModelBase {

    @Getter
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    public EmotionType() {
        // Default constructor for JPA
    }

    public EmotionType(String name) {
        this.name = name;
    }

}
