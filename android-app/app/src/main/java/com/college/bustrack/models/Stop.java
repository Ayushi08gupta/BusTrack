package com.college.bustrack.models;

public class Stop {
    private String name;
    private double latitude;
    private double longitude;
    private String eta;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getEta() { return eta; }
    public void setEta(String eta) { this.eta = eta; }
}
