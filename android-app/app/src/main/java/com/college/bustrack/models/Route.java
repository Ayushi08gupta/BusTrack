package com.college.bustrack.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Route {
    @SerializedName("_id")
    private String id;
    private String routeName;
    private List<Stop> stops;
    private double distance;
    private int estimatedTime;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public List<Stop> getStops() { return stops; }
    public void setStops(List<Stop> stops) { this.stops = stops; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public int getEstimatedTime() { return estimatedTime; }
    public void setEstimatedTime(int estimatedTime) { this.estimatedTime = estimatedTime; }
}
