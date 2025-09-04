package org.texttechnologylab.uce.common.models.dto.map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PointDto {
    private long id;
    private long annotationId;
    private String annotationType;
    private String locationCoveredText;
    private String location;
    private String date;
    private String dateCoveredText;
    private String label;
    private double latitude;
    private double longitude;
}
