package com.college.bustrack.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "BusTrackPref";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_ASSIGNED_BUS_ID = "assigned_bus_id";
    private static final String KEY_IS_FIRST_LOGIN = "is_first_login";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveSession(String token, String id, String name, String email, String role, boolean isFirstLogin, String assignedBusId) {
        editor.putString(KEY_TOKEN, token);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, id);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.putBoolean(KEY_IS_FIRST_LOGIN, isFirstLogin);
        editor.putString(KEY_ASSIGNED_BUS_ID, assignedBusId);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, null);
    }

    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, null);
    }

    public String getAssignedBusId() {
        return pref.getString(KEY_ASSIGNED_BUS_ID, null);
    }

    public boolean isFirstLogin() {
        return pref.getBoolean(KEY_IS_FIRST_LOGIN, true);
    }

    public void setFirstLogin(boolean isFirstLogin) {
        editor.putBoolean(KEY_IS_FIRST_LOGIN, isFirstLogin);
        editor.apply();
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
