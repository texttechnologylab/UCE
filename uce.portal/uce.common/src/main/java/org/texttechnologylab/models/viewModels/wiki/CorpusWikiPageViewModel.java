package org.texttechnologylab.models.viewModels.wiki;

import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.UCEMetadataValueType;
import org.texttechnologylab.models.viewModels.CorpusViewModel;
import org.texttechnologylab.models.viewModels.JsonViewModel;
import org.texttechnologylab.utils.JsonBeautifier;

import java.util.List;

public class CorpusWikiPageViewModel extends AnnotationWikiPageViewModel{

    private int documentsCount;

    public int getDocumentsCount() {
        return documentsCount;
    }

    public void setDocumentsCount(int documentsCount) {
        this.documentsCount = documentsCount;
    }

    public List<JsonViewModel> getCorpusConfigJsonAsIterable() {
        var beautifier = new JsonBeautifier();
        return beautifier.parseJsonToViewModel(getCorpus().getCorpus().getCorpusJsonConfig());
    }

}
