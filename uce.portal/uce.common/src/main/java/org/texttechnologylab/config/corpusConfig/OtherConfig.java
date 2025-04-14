package org.texttechnologylab.config.corpusConfig;

public class OtherConfig{
    private boolean availableOnFrankfurtUniversityCollection;
    private boolean includeKeywordDistribution;
    private boolean enableEmbeddings;
    private boolean enableRAGBot;

    public boolean isIncludeKeywordDistribution() {
        return includeKeywordDistribution;
    }

    public void setIncludeKeywordDistribution(boolean includeKeywordDistribution) {
        this.includeKeywordDistribution = includeKeywordDistribution;
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
