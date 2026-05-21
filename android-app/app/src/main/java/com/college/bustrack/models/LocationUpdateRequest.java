package com.college.bustrack.models;

public class LocationUpdateRequest {
    private double latitude;
    private double longitude;
    private String status;

    public LocationUpdateRequest(double latitude, double longitude, String status) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getStatus() { return status; }
}
