package org.texttechnologylab.models.dto;

import java.util.Arrays;

public class LocationDto {

    private double longitude;
    private double latitude;
    private double range;

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
    public LocationDto(Double latitude, Double longitude, Double range){
        this.latitude = latitude;
        this.longitude = longitude;
        this.range = range;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }
}
