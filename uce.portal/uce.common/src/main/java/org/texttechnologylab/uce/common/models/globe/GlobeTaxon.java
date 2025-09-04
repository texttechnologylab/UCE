package org.texttechnologylab.uce.common.models.globe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GlobeTaxon {

    private double longitude;
    private double latitude;
    private String taxonId;
    private String name;
    private String value;
    private String country;
    private String region;
    private String image;

    public GlobeTaxon() {
    }

}
