package org.texttechnologylab.models.dto.rdf;

import com.fasterxml.jackson.annotation.JsonAlias;

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
