package com.ddairways.model;

import java.net.URI;
import java.net.URISyntaxException;

public class Passenger {
    private String firstName;
    private String lastName;
    private String email;
    private final String travelClass;

    public Passenger(String firstName, String lastName, String email, String travelClass) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.travelClass = travelClass;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public URI getEmailUri() throws URISyntaxException {
        return new URI("mailto:" + email);
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getTravelClass() {
        return travelClass;
    }
}
