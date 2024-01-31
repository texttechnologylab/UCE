package org.texttechnologylab.models;

import java.util.Date;

public class ModelBase {

    private Date created;

    public ModelBase() {
        created = new Date();
    }

    public Date getCreated() {
        return created;
    }
}
