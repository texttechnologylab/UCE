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
@Table(name = "model_category")
public class ModelCategory extends ModelBase {

    @Getter
    @Setter
    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;

    @Getter
    @Setter
    @ManyToMany(mappedBy = "categories")
    @Fetch(FetchMode.JOIN)
    private Set<Model> models;

    public ModelCategory() {
        // Default constructor for JPA
    }

    public ModelCategory(String categoryName) {
        this.categoryName = categoryName;
    }

}
