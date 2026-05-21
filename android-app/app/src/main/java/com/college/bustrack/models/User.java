package com.college.bustrack.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("_id")
    private String id;
    private String name;
    private String email;
    private String username;
    private String password;
    private String role;
    private boolean isFirstLogin;
    private String assignedBusId;
    private boolean isActive;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isFirstLogin() { return isFirstLogin; }
    public void setFirstLogin(boolean firstLogin) { isFirstLogin = firstLogin; }

    public String getAssignedBusId() { return assignedBusId; }
    public void setAssignedBusId(String assignedBusId) { this.assignedBusId = assignedBusId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
