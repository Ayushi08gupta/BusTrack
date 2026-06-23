package com.college.bustrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.Bus;
import com.college.bustrack.models.GenericResponse;
import com.college.bustrack.models.Trip;
import com.college.bustrack.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverDashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private com.google.android.gms.maps.MapView map;
    private GoogleMap googleMap;
    private Polyline routePolyline;
    private List<Marker> stopMarkers = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private Socket mSocket;

    private TextView tvWelcomeDriver, tvTripStatus, tvBusNumber;
    private MaterialButton btnStartTrip, btnEndTrip;
    private ImageButton btnLogout;

    private SessionManager sessionManager;
    private ApiService apiService;
    private boolean isTripActive = false;
    private String currentTripId;
    private Bus assignedBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupSocket();
        map.onCreate(savedInstanceState);
        map.getMapAsync(this);

        if (sessionManager.getUserName() != null) {
            tvWelcomeDriver.setText("Hello, " + sessionManager.getUserName());
        }

        btnStartTrip.setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                startJourney();
            }
        });

        btnEndTrip.setOnClickListener(v -> stopJourney());

        tvTripStatus.setOnLongClickListener(v -> {
            if (isTripActive) showDelayDialog();
            return true;
        });

        btnLogout.setOnClickListener(v -> {
            if (isTripActive) stopJourney();
            sessionManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        fetchAssignment();
    }

    private void setupSocket() {
        try {
            mSocket = IO.socket(ApiClient.getBaseUrl());
            mSocket.on("assignment:updated", args -> runOnUiThread(this::fetchAssignment));
            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void showDelayDialog() {
        String[] options = {"5 mins", "10 mins", "15 mins"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Delay")
                .setItems(options, (dialog, which) -> {
                    String delay = options[which];
                    if (mSocket != null && mSocket.connected() && assignedBus != null) {
                        try {
                            org.json.JSONObject data = new org.json.JSONObject();
                            data.put("busId", assignedBus.getId());
                            data.put("delay", delay);
                            mSocket.emit("bus:delay", data);
                            Toast.makeText(this, "Delay alert sent: " + delay, Toast.LENGTH_SHORT).show();
                        } catch (org.json.JSONException e) { e.printStackTrace(); }
                    }
                }).show();
    }

    private void initViews() {
        map = findViewById(R.id.mapView);
        tvWelcomeDriver = findViewById(R.id.tvWelcomeDriver);
        tvTripStatus = findViewById(R.id.tvTripStatus);
        tvBusNumber = findViewById(R.id.tvBusNumber);
        btnStartTrip = findViewById(R.id.btnStartTrip);
        btnEndTrip = findViewById(R.id.btnEndTrip);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void fetchAssignment() {
        apiService.getDriverAssignedBus(sessionManager.getToken()).enqueue(new Callback<Bus>() {
            @Override
            public void onResponse(Call<Bus> call, Response<Bus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    assignedBus = response.body();
                    sessionManager.setAssignedBusId(assignedBus.getId());
                    tvBusNumber.setText("Bus: " + assignedBus.getBusNumber());
                    if (assignedBus.getRouteId() != null) {
                        tvBusNumber.append(" | Route: " + assignedBus.getRouteId().getRouteName());
                        drawRoute(assignedBus.getRouteId().getStops());
                    }
                } else {
                    tvBusNumber.setText("No Bus Assigned");
                    btnStartTrip.setEnabled(false);
                }
            }

            @Override
            public void onFailure(Call<Bus> call, Throwable t) {
                Toast.makeText(DriverDashboardActivity.this, "Failed to load assignment", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startJourney() {
        if (assignedBus == null) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestPermissions();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            Map<String, String> body = new HashMap<>();
            body.put("busId", assignedBus.getId());
            if (location != null) {
                body.put("latitude", String.valueOf(location.getLatitude()));
                body.put("longitude", String.valueOf(location.getLongitude()));
            }

            apiService.startJourney(sessionManager.getToken(), body).enqueue(new Callback<Trip>() {
                @Override
                public void onResponse(Call<Trip> call, Response<Trip> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        currentTripId = response.body().getId();
                        startTracking();
                    } else {
                        Toast.makeText(DriverDashboardActivity.this, "Failed to start journey", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Trip> call, Throwable t) {
                    Toast.makeText(DriverDashboardActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void stopJourney() {
        if (assignedBus == null) return;

        Map<String, String> body = new HashMap<>();
        body.put("busId", assignedBus.getId());
        body.put("tripId", currentTripId);

        apiService.stopJourney(sessionManager.getToken(), body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    stopTracking();
                } else {
                    Toast.makeText(DriverDashboardActivity.this, "Failed to stop journey", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(DriverDashboardActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        setupMap();
    }

    private void setupMap() {
        if (googleMap == null) return;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setRotateGesturesEnabled(true);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(23.2599, 77.4126), 15f));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    private void drawRoute(List<com.college.bustrack.models.Stop> stops) {
        if (googleMap == null || stops == null) return;
        
        // Clear old stop markers
        for (Marker m : stopMarkers) m.remove();
        stopMarkers.clear();

        if (routePolyline != null) routePolyline.remove();

        PolylineOptions polylineOptions = new PolylineOptions()
                .color(android.graphics.Color.BLUE)
                .width(10f);

        for (com.college.bustrack.models.Stop stop : stops) {
            LatLng latLng = new LatLng(stop.getLatitude(), stop.getLongitude());
            polylineOptions.add(latLng);
            
            Marker stopMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(stop.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            if (stopMarker != null) stopMarkers.add(stopMarker);
        }
        routePolyline = googleMap.addPolyline(polylineOptions);
    }

    private boolean checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissionsNeeded) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startTracking();
            } else {
                Toast.makeText(this, "Permissions are required for tracking", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startTracking() {
        isTripActive = true;
        tvTripStatus.setText("Status: ACTIVE");
        tvTripStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        btnStartTrip.setVisibility(View.GONE);
        btnEndTrip.setVisibility(View.VISIBLE);

        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        serviceIntent.putExtra("trip_id", currentTripId);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopTracking() {
        isTripActive = false;
        tvTripStatus.setText("Status: INACTIVE");
        tvTripStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        btnStartTrip.setVisibility(View.VISIBLE);
        btnEndTrip.setVisibility(View.GONE);

        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        map.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        map.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onDestroy();
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        map.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        map.onLowMemory();
    }
}
