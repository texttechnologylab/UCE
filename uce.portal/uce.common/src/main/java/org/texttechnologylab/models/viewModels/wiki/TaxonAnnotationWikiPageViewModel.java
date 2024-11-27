package org.texttechnologylab.models.viewModels.wiki;

import org.texttechnologylab.models.gbif.GbifOccurrence;

import java.util.List;

public class TaxonAnnotationWikiPageViewModel extends NamedEntityAnnotationWikiPageViewModel {

    private List<String> alternativeNames;
    private List<GbifOccurrence> gbifOccurrences;

    public TaxonAnnotationWikiPageViewModel(){
        super();
    }

    public List<GbifOccurrence> getGbifOccurrences() {
        return gbifOccurrences;
    }

    public void setGbifOccurrences(List<GbifOccurrence> gbifOccurrences) {
        this.gbifOccurrences = gbifOccurrences;
    }

    public List<String> getAlternativeNames() {
        return alternativeNames;
    }

    public void setAlternativeNames(List<String> alternativeNames) {
        this.alternativeNames = alternativeNames;
    }
}
