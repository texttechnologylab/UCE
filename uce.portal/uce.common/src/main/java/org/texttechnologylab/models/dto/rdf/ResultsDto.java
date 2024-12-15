package org.texttechnologylab.models.dto.rdf;

import java.util.List;

public class ResultsDto {

    private List<RDFNodeDto> bindings;

    public List<RDFNodeDto> getBindings() {
        return bindings;
    }

    public void setBindings(List<RDFNodeDto> bindings) {
        this.bindings = bindings;
    }
}
