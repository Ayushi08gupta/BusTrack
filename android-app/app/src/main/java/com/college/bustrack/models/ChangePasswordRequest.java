package com.college.bustrack.models;

public class ChangePasswordRequest {
    private String newPassword;

    public ChangePasswordRequest(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() { return newPassword; }
}
