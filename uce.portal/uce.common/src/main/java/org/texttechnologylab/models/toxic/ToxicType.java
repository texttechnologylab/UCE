package org.texttechnologylab.models.toxic;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "toxic_type")
public class ToxicType extends ModelBase {

    @Getter
    @Setter
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    public ToxicType() {}

    public ToxicType(String name) {
        this.name = name;
    }

}
