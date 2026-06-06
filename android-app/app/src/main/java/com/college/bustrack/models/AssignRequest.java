package com.college.bustrack.models;

public class AssignRequest {
    private String driverId;
    private String routeId;
    private String busId;

    public AssignRequest(String driverId, String busId, String routeId) {
        this.driverId = driverId;
        this.busId = busId;
        this.routeId = routeId;
    }

    public String getDriverId() { return driverId; }
    public String getRouteId() { return routeId; }
    public String getBusId() { return busId; }
}
