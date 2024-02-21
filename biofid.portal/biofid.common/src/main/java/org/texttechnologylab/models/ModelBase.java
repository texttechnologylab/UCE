package org.texttechnologylab.models;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Date;
import java.util.UUID;

public class ModelBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
