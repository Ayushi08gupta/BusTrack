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

import com.college.bustrack.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class DriverDashboardActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private MapView map;
    private MyLocationNewOverlay locationOverlay;
    private RotationGestureOverlay rotationGestureOverlay;
    private FusedLocationProviderClient fusedLocationClient;

    private TextView tvWelcomeDriver, tvTripStatus;
    private MaterialButton btnStartTrip, btnEndTrip;
    private ImageButton btnLogout;

    private SessionManager sessionManager;
    private boolean isTripActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        
        setContentView(R.layout.activity_driver_dashboard);

        sessionManager = new SessionManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupMap();

        if (sessionManager.getUserName() != null) {
            tvWelcomeDriver.setText("Hello, " + sessionManager.getUserName());
        }

        btnStartTrip.setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                startTracking();
            }
        });

        btnEndTrip.setOnClickListener(v -> stopTracking());

        btnLogout.setOnClickListener(v -> {
            stopTracking();
            sessionManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void initViews() {
        map = findViewById(R.id.mapView);
        tvWelcomeDriver = findViewById(R.id.tvWelcomeDriver);
        tvTripStatus = findViewById(R.id.tvTripStatus);
        btnStartTrip = findViewById(R.id.btnStartTrip);
        btnEndTrip = findViewById(R.id.btnEndTrip);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupMap() {
        map.setMultiTouchControls(true);
        map.getController().setZoom(18.0);
        map.getController().setCenter(new GeoPoint(23.2599, 77.4126)); // Initial center

        // 1. Smooth "Follow Me" and Marker Rotation using MyLocationNewOverlay
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        locationOverlay.setDrawAccuracyEnabled(true); // Shows orientation arrow
        map.getOverlays().add(locationOverlay);

        // 2. RotationGestureOverlay to allow map rotation (Uber-like feel)
        rotationGestureOverlay = new RotationGestureOverlay(map);
        rotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(rotationGestureOverlay);
        
        // 3. Automated "Bearing-to-North" orientation listener
        // This ensures the map rotates to match the direction of travel
        locationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            if (locationOverlay.getMyLocation() != null) {
                map.getController().animateTo(locationOverlay.getMyLocation());
            }
        }));
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
        if (locationOverlay != null) locationOverlay.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        if (locationOverlay != null) locationOverlay.disableMyLocation();
    }
}
