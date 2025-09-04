package org.texttechnologylab.uce.common.models.dto.rdf;

public class RDFSelectQueryDto extends RDFRequestDto {

    private HeadDto head;
    private ResultsDto results;

    public ResultsDto getResults() {
        return results;
    }

    public void setResults(ResultsDto results) {
        this.results = results;
    }

    public HeadDto getHead() {
        return head;
    }

    public void setHead(HeadDto head) {
        this.head = head;
    }
}
