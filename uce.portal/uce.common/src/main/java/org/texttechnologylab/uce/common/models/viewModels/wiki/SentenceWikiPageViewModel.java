package org.texttechnologylab.uce.common.models.viewModels.wiki;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.corpus.Lemma;

import java.util.List;

public class SentenceWikiPageViewModel extends AnnotationWikiPageViewModel{

    @Getter
    @Setter
    private List<Document> similarDocuments;

    @Getter
    @Setter
    private List<Lemma> lemmas;

    public SentenceWikiPageViewModel(){}

}
