package org.texttechnologylab.uce.common.models.dto.map;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Setter
@Getter
public class LocationDto {

    private double longitude;
    private double latitude;
    private double radius;

    public static LocationDto fromCommandString(String commandString){
        var split = commandString.split(";");
        var lng = Arrays.stream(split)
                .filter(s -> s.startsWith("lng="))
                .map(s -> s.split("=")[1])
                .map(Double::parseDouble)
                .findFirst();
        var lat = Arrays.stream(split)
                .filter(s -> s.startsWith("lat="))
                .map(s -> s.split("=")[1])
                .map(Double::parseDouble)
                .findFirst();
        var range = Arrays.stream(split)
                .filter(s -> s.startsWith("r="))
                .map(s -> s.split("=")[1])
                .map(Double::parseDouble)
                .findFirst();
        if(lng.isEmpty() || lat.isEmpty() || range.isEmpty()) return null;
        return new LocationDto(lat.get(), lng.get(), range.get());
    }

    public LocationDto(){}
    public LocationDto(Double latitude, Double longitude, Double radius){
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

}
