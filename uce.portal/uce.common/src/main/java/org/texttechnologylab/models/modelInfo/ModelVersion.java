package org.texttechnologylab.models.modelInfo;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.*;

@Entity
@Table(name = "model_version", uniqueConstraints = @UniqueConstraint(columnNames = {"model_id", "version"}))
public class ModelVersion extends ModelBase {

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @Getter
    @Setter
    @Column(name = "version", nullable = false)
    private String version;

    public ModelVersion() {
        // Default constructor for JPA
    }

    public ModelVersion(Model model, String version) {
        this.model = model;
        this.version = version;
    }

}
