package org.texttechnologylab.models.viewModels.wiki;

import org.texttechnologylab.models.corpus.Lemma;

import java.util.List;

public class NamedEntityAnnotationWikiPageViewModel extends AnnotationWikiPageViewModel{
    private List<Lemma> lemmas;
    public NamedEntityAnnotationWikiPageViewModel(){}

    public List<Lemma> getLemmas() {
        return lemmas;
    }

    public void setLemmas(List<Lemma> lemmas) {
        this.lemmas = lemmas;
    }
}
