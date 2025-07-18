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
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    public Model() {
        // Default constructor for JPA
    }

    public Model(String name) {
        this.name = name;
    }

}
