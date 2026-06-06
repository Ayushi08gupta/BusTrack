package com.college.bustrack.models;

public class LocationUpdateRequest {
    private String busId;
    private String driverId;
    private String tripId;
    private double latitude;
    private double longitude;
    private float speed;
    private float heading;
    private String status;

    public LocationUpdateRequest(String busId, String driverId, String tripId, double latitude, double longitude, float speed, float heading, String status) {
        this.busId = busId;
        this.driverId = driverId;
        this.tripId = tripId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.heading = heading;
        this.status = status;
    }

    public String getBusId() { return busId; }
    public String getDriverId() { return driverId; }
    public String getTripId() { return tripId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getSpeed() { return speed; }
    public float getHeading() { return heading; }
    public String getStatus() { return status; }
}
