package org.texttechnologylab.models.modelInfo;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "model")
public class Model extends ModelBase {

    @Getter
    @Setter
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Getter
    @Setter
    @ManyToMany(mappedBy = "models")
    @Fetch(FetchMode.JOIN)
    private Set<ModelCategory> categories;

    public Model() {
        // Default constructor for JPA
    }

    public Model(String name) {
        this.name = name;
    }

}
