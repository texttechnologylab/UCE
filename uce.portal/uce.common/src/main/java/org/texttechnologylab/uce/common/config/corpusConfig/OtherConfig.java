package org.texttechnologylab.uce.common.config.corpusConfig;

public class OtherConfig{
    private boolean availableOnFrankfurtUniversityCollection;
    private boolean includeKeywordDistribution;
    private boolean enableEmbeddings;
    private boolean enableRAGBot;
    private boolean enableS3Storage;

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

    public boolean isEnableS3Storage() {
        return enableS3Storage;
    }

    public void setEnableS3Storage(boolean enableS3Storage) {
        this.enableS3Storage = enableS3Storage;
    }
}
