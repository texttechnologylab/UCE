package org.texttechnologylab.uce.common.models.viewModels.wiki;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.corpus.Lemma;

import java.util.List;

@Getter
@Setter
public class GeoNameAnnotationWikiPageViewModel extends AnnotationWikiPageViewModel{

    private List<Document> similarDocuments;
    private List<Lemma> lemmas;

    public GeoNameAnnotationWikiPageViewModel(){}

}
