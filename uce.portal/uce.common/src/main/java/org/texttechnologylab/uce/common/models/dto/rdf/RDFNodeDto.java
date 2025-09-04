package org.texttechnologylab.uce.common.models.dto.rdf;

public class RDFNodeDto {
    private TripletDto subject;
    private TripletDto predicate;
    private TripletDto object;

    public RDFNodeDto() {
    }

    public TripletDto getSubject() {
        return subject;
    }

    public void setSubject(TripletDto subject) {
        this.subject = subject;
    }

    public TripletDto getPredicate() {
        return predicate;
    }

    public void setPredicate(TripletDto predicate) {
        this.predicate = predicate;
    }

    public TripletDto getObject() {
        return object;
    }

    public void setObject(TripletDto object) {
        this.object = object;
    }
}
