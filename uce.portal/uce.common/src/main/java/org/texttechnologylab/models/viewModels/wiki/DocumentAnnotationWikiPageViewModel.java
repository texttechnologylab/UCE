package org.texttechnologylab.models.viewModels.wiki;

import org.texttechnologylab.models.corpus.UCEMetadata;

import java.util.List;

public class DocumentAnnotationWikiPageViewModel extends AnnotationWikiPageViewModel{

    private List<UCEMetadata> uceMetadata;

    public List<UCEMetadata> getUceMetadata() {
        return uceMetadata;
    }

    public void setUceMetadata(List<UCEMetadata> uceMetadata) {
        this.uceMetadata = uceMetadata;
    }
}
