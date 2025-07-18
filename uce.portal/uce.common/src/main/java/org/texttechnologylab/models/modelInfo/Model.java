package org.texttechnologylab.models.modelInfo;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "model")
public class Model extends ModelBase {

    @Getter
    @Setter
    @Column(name = "model_name", nullable = false, unique = true)
    private String modelName;

    public Model() {
        // Default constructor for JPA
    }
    public Model(String modelName) {
        this.modelName = modelName;
    }

}
