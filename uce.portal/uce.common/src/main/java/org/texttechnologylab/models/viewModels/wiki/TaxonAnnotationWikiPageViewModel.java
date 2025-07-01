package org.texttechnologylab.models.viewModels.wiki;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.dto.rdf.RDFNodeDto;
import org.texttechnologylab.models.gbif.GbifOccurrence;

import java.util.List;

public class TaxonAnnotationWikiPageViewModel extends NamedEntityAnnotationWikiPageViewModel {

    @Getter
    @Setter
    private List<String> alternativeNames;

    /**
     * The odds that this recognized taxon is correct. Only applied to GNFinder taxa.
     */
    @Getter
    @Setter
    private double odds;

    @Getter
    @Setter
    private List<GbifOccurrence> gbifOccurrences;

    @Getter
    @Setter
    private List<RDFNodeDto> nextRDFNodes;

    @Getter
    @Setter
    private String annotatedBy;

    public TaxonAnnotationWikiPageViewModel(){
        super();
    }

}
