package com.college.bustrack.models;

import com.google.gson.annotations.SerializedName;

public class Trip {
    @SerializedName("_id")
    private String id;
    private String busId;
    private String driverId;
    private String routeId;
    private String startTime;
    private String endTime;
    private String status;
    private double distanceCovered;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getDistanceCovered() { return distanceCovered; }
    public void setDistanceCovered(double distanceCovered) { this.distanceCovered = distanceCovered; }
}
