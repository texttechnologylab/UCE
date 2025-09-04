package org.texttechnologylab.uce.common.models.corpus;

public enum GeoNameFeatureClass {

    A("Country, state, region..."),
    H("Stream, lake, waterbody..."),
    L("Parks, area, zones..."),
    P("City, village, settlement..."),
    R("Roads, railroads..."),
    S("Spot, building, farm..."),
    T("Mountain, hill, rock..."),
    U("Undersea..."),
    V("Forest, heath, vegetation...");

    private final String fullName;

    GeoNameFeatureClass(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }
}
