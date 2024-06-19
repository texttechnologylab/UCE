package org.texttechnologylab.config.corpusConfig;

public class OtherConfig{
    private boolean availableOnFrankfurtUniversityCollection;
    private boolean includeTopicDistribution;
    private boolean enableEmbeddings;
    private boolean enableRAGBot;

    public boolean isIncludeTopicDistribution() {
        return includeTopicDistribution;
    }

    public void setIncludeTopicDistribution(boolean includeTopicDistribution) {
        this.includeTopicDistribution = includeTopicDistribution;
    }

    public boolean isAvailableOnFrankfurtUniversityCollection() {
        return availableOnFrankfurtUniversityCollection;
    }

    public void setAvailableOnFrankfurtUniversityCollection(boolean availableOnFrankfurtUniversityCollection) {
        this.availableOnFrankfurtUniversityCollection = availableOnFrankfurtUniversityCollection;
    }

    public boolean isEnableEmbeddings() {
        return enableEmbeddings;
    }

    public void setEnableEmbeddings(boolean enableEmbeddings) {
        this.enableEmbeddings = enableEmbeddings;
    }

    public boolean isEnableRAGBot() {
        return enableRAGBot;
    }

    public void setEnableRAGBot(boolean enableRAGBot) {
        this.enableRAGBot = enableRAGBot;
    }
}
