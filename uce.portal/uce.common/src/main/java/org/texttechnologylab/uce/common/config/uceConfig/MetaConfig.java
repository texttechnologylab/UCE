package org.texttechnologylab.uce.common.config.uceConfig;

public class MetaConfig {
    private String name;
    private String version;
    private String description;

    public MetaConfig(){}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
