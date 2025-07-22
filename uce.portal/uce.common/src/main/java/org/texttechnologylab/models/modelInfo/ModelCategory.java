package org.texttechnologylab.models.modelInfo;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "model_category")
public class ModelCategory extends ModelBase {

    @Getter
    @Setter
    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;

    public ModelCategory() {
        // Default constructor for JPA
    }

    public ModelCategory(String categoryName) {
        this.categoryName = categoryName;
    }

}
