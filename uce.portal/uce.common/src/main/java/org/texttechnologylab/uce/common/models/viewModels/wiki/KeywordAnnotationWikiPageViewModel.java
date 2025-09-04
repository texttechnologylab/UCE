package org.texttechnologylab.uce.common.models.viewModels.wiki;

import org.texttechnologylab.uce.common.models.corpus.KeywordDistribution;

import java.util.List;

public class KeywordAnnotationWikiPageViewModel extends AnnotationWikiPageViewModel {
    private KeywordDistribution keywordDistribution;
    private List<? extends KeywordDistribution> similarKeywordDistributions;
    public KeywordAnnotationWikiPageViewModel(){super();}

    public List<? extends KeywordDistribution> getSimilarKeywordDistributions() {
        return similarKeywordDistributions;
    }

    public void setSimilarKeywordDistributions(List<? extends KeywordDistribution> similarKeywordDistributions) {
        this.similarKeywordDistributions = similarKeywordDistributions;
    }

    public KeywordDistribution getKeywordDistribution() {
        return keywordDistribution;
    }

    public void setKeywordDistribution(KeywordDistribution keywordDistribution) {
        this.keywordDistribution = keywordDistribution;
    }
}
