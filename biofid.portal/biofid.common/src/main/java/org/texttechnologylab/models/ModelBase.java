package org.texttechnologylab.models;

import java.util.Date;
import java.util.UUID;

public class ModelBase {

    private UUID modelId;
    private Date created;

    public ModelBase() {
        modelId = UUID.randomUUID();
        created = new Date();
    }

    public UUID getModelId() {
        return modelId;
    }

    public Date getCreated() {
        return created;
    }
}
