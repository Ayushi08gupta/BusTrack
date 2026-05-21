package com.college.bustrack.models;

import com.google.gson.annotations.SerializedName;

public class Bus {
    @SerializedName("_id")
    private String id;
    private String busNumber;
    private User driverId; // Populated from API
    private Route routeId; // Populated from API
    private CurrentLocation currentLocation;
    private String status;

    public static class CurrentLocation {
        private double latitude;
        private double longitude;
        private String timestamp;

        // Getters & Setters
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }

    public User getDriverId() { return driverId; }
    public void setDriverId(User driverId) { this.driverId = driverId; }

    public Route getRouteId() { return routeId; }
    public void setRouteId(Route routeId) { this.routeId = routeId; }

    public CurrentLocation getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(CurrentLocation currentLocation) { this.currentLocation = currentLocation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
