package org.texttechnologylab.uce.common.models.corpus;

public enum UCEMetadataValueType {
    // NOTE: The order of these enums should not change, as we use the "ordinal" directly in the database.
    // New types should be added at the end.
    STRING,
    NUMBER,
    JSON,
    DATE,
    URL,
    ENUM,
}
