package com.ddairways.model;

import java.util.Date;

public class Flight {
    private final String code;
    private final String number;
    private final Airport origin;
    private final Airport destination;
    private final Date departure;
    private final int durationInHours;

    public Flight(String code, String number, Airport origin, Airport destination, Date departure, int durationInHours) {
        this.code = code;
        this.number = number;
        this.origin = origin;
        this.destination = destination;
        this.departure = departure;
        this.durationInHours = durationInHours;
    }

    public String getCode() {
        return code;
    }

    public String getNumber() {
        return number;
    }

    public String originAirportCode() {
        return origin.getCode();
    }

    public String destinationAirportCode() {
        return destination.getCode();
    }

    public String getCompleteNumber() {
        return code + number;
    }

    public String getDepartureTime() {
        return new org.joda.time.DateTime(departure).toString("HH:mm");
    }

    public String getArrivalTime() {
        return new org.joda.time.DateTime(departure)
                .plusHours(durationInHours)
                .toString("HH:mm");
    }

    public String getDepartureDate() {
        return new org.joda.time.DateTime(departure).toString("dd-MMM-yyyy");
    }

    public Date getDeparture() {
        return departure;
    }
    public String getBarcodeData() {
        return new StringBuilder()
                .append(code)
                .append(number)
                .append(getDepartureDate())
                .append(originAirportCode())
                .append(getDepartureTime())
                .append(destinationAirportCode())
                .append(getArrivalTime())
                .toString();
    }

    public String getOriginDestinationAirportCodes() {
        return String.format("FROM: %s  TO: %s", origin.getCode(), destination.getCode());
    }

    public String getDestinationCity() {
        return destination.getCity();
    }

    public String getOriginCityWithAirportCode() {
        return String.format("FROM: %s", origin);
    }

    public String getDestinationCityWithAirportCode() {
        return String.format("TO: %s", destination);
    }
}
