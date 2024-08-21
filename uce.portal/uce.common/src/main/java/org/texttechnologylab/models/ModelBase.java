package org.texttechnologylab.models;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@MappedSuperclass
public class ModelBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    public ModelBase() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
