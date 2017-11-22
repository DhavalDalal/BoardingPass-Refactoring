package com.ddairways.model;

public class Airport {
    private String code;
    private String city;

    public Airport(String code, String city) {
        this.code = code;
        this.city = city;
    }

    public String getCode() {
        return code;
    }

    public String getCity() {
        return city;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", city, code);
    }
}
