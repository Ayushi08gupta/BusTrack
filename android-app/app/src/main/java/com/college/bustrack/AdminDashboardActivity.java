package com.college.bustrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.GenericResponse;
import com.college.bustrack.models.User;
import com.college.bustrack.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextInputEditText etUserName, etUserEmail, etUserRole;
    private TextInputEditText etRouteId, etStopName, etStopLat, etStopLng;
    private MaterialButton btnAddUser, btnBulkUpload, btnAddStop, btnTabUsers, btnTabRoutes;
    private LinearLayout containerUsers, containerRoutes;
    private RecyclerView rvUsers;
    private ImageButton btnLogout;

    private ApiService apiService;
    private SessionManager sessionManager;
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);

        // UI Containers
        containerUsers = findViewById(R.id.containerUsers);
        containerRoutes = findViewById(R.id.containerRoutes);

        // Navigation Tabs
        btnTabUsers = findViewById(R.id.btnTabUsers);
        btnTabRoutes = findViewById(R.id.btnTabRoutes);

        // Form Fields
        etUserName = findViewById(R.id.etUserName);
        etUserEmail = findViewById(R.id.etUserEmail);
        etUserRole = findViewById(R.id.etUserRole);
        etRouteId = findViewById(R.id.etRouteId);
        etStopName = findViewById(R.id.etStopName);
        etStopLat = findViewById(R.id.etStopLat);
        etStopLng = findViewById(R.id.etStopLng);

        // Buttons
        btnAddUser = findViewById(R.id.btnAddUser);
        btnBulkUpload = findViewById(R.id.btnBulkUpload);
        btnAddStop = findViewById(R.id.btnAddStop);
        btnLogout = findViewById(R.id.btnLogout);

        // User List
        rvUsers = findViewById(R.id.rvUsers);

        setupTabs();
        setupUserList();

        if (btnAddUser != null) btnAddUser.setOnClickListener(v -> addUser());
        if (btnAddStop != null) btnAddStop.setOnClickListener(v -> addStop());
        
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                sessionManager.logout();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
        }

        loadUsers();
    }

    private void setupTabs() {
        if (btnTabUsers != null) {
            btnTabUsers.setOnClickListener(v -> {
                containerUsers.setVisibility(View.VISIBLE);
                containerRoutes.setVisibility(View.GONE);
            });
        }
        if (btnTabRoutes != null) {
            btnTabRoutes.setOnClickListener(v -> {
                containerUsers.setVisibility(View.GONE);
                containerRoutes.setVisibility(View.VISIBLE);
            });
        }
    }

    private void setupUserList() {
        if (rvUsers != null) {
            rvUsers.setLayoutManager(new LinearLayoutManager(this));
            userAdapter = new UserAdapter(new ArrayList<>(), this::onUserAction);
            rvUsers.setAdapter(userAdapter);
        }
    }

    private void onUserAction(User user, String action) {
        if ("delete".equals(action)) {
            new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Delete " + user.getName() + "?")
                .setPositiveButton("Yes", (d, w) -> deleteUser(user.getId()))
                .setNegativeButton("No", null)
                .show();
        } else if ("status".equals(action)) {
            toggleUserStatus(user);
        }
    }

    private void loadUsers() {
        apiService.adminGetUsers(sessionManager.getToken()).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (userAdapter != null) userAdapter.updateUsers(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    private void addUser() {
        String name = etUserName.getText().toString().trim();
        String email = etUserEmail.getText().toString().trim();
        String role = etUserRole.getText().toString().trim().toLowerCase();

        if (email.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "Email and Role are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);

        apiService.adminAddUser(sessionManager.getToken(), userData).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminDashboardActivity.this, "User created!", Toast.LENGTH_SHORT).show();
                    loadUsers();
                    etUserName.setText(""); etUserEmail.setText(""); etUserRole.setText("");
                } else {
                    Toast.makeText(AdminDashboardActivity.this, "Failed to create user", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addStop() {
        String routeId = etRouteId.getText().toString().trim();
        String stopName = etStopName.getText().toString().trim();
        String latStr = etStopLat.getText().toString().trim();
        String lngStr = etStopLng.getText().toString().trim();

        if (routeId.isEmpty() || stopName.isEmpty() || latStr.isEmpty() || lngStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double lat = Double.parseDouble(latStr);
            double lng = Double.parseDouble(lngStr);

            Map<String, Object> stopData = new HashMap<>();
            stopData.put("name", stopName);
            stopData.put("latitude", lat);
            stopData.put("longitude", lng);

            apiService.adminAddStop(sessionManager.getToken(), routeId, stopData).enqueue(new Callback<GenericResponse>() {
                @Override
                public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AdminDashboardActivity.this, "Stop added", Toast.LENGTH_SHORT).show();
                        etStopName.setText(""); etStopLat.setText(""); etStopLng.setText("");
                    }
                }
                @Override
                public void onFailure(Call<GenericResponse> call, Throwable t) {}
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid Latitude/Longitude", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleUserStatus(User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("isActive", !user.isActive());
        apiService.adminToggleUserStatus(sessionManager.getToken(), user.getId(), body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) loadUsers();
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {}
        });
    }

    private void deleteUser(String id) {
        apiService.adminDeleteUser(sessionManager.getToken(), id).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    loadUsers();
                    Toast.makeText(AdminDashboardActivity.this, "User deleted", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {}
        });
    }
}
