package org.texttechnologylab.models.toxic;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.*;

@Entity
@Table(name = "toxic_value")
public class ToxicValue extends ModelBase {

    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "toxic_id", nullable = false)
    private Toxic toxic;

    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "toxic_type_id", nullable = false)
    private ToxicType toxicType;

    @Getter
    @Setter
    @Column(name = "value", nullable = false)
    private double value;

    public ToxicValue() {
    }

}
