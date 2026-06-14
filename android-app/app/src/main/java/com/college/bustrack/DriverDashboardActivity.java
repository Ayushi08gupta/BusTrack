package com.college.bustrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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
import com.college.bustrack.models.Route;
import com.college.bustrack.models.Stop;
import com.college.bustrack.utils.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverDashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 200;

    private GoogleMap mMap;
    private Polyline routePolyline;
    private List<Marker> stopMarkers = new ArrayList<>();

    private TextView tvBusNumber, tvRouteName, tvStopsCount, tvRouteDuration, tvTripStatus, tvUpcomingStopName;
    private MaterialButton btnStartTrip, btnEndTrip;
    private MaterialCardView upcomingStopCard;

    private SessionManager sessionManager;
    private ApiService apiService;
    private boolean isTripActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();

        initViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fetchAssignedBusDetails();
    }

    private void initViews() {
        tvBusNumber = findViewById(R.id.tvBusNumber);
        tvRouteName = findViewById(R.id.tvRouteName);
        tvStopsCount = findViewById(R.id.tvStopsCount);
        tvRouteDuration = findViewById(R.id.tvRouteDuration);
        tvTripStatus = findViewById(R.id.tvTripStatus);
        tvUpcomingStopName = findViewById(R.id.tvUpcomingStopName);
        upcomingStopCard = findViewById(R.id.upcomingStopCard);
        
        btnStartTrip = findViewById(R.id.btnStartTrip);
        btnEndTrip = findViewById(R.id.btnEndTrip);

        btnStartTrip.setOnClickListener(v -> startTripProcess());
        btnEndTrip.setOnClickListener(v -> stopTrackingService());
        
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            sessionManager.logout();
            finish();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    private void fetchAssignedBusDetails() {
        apiService.getDriverAssignedBus(sessionManager.getToken()).enqueue(new Callback<Bus>() {
            @Override
            public void onResponse(Call<Bus> call, Response<Bus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Bus bus = response.body();
                    tvBusNumber.setText("Bus: " + bus.getBusNumber());
                    
                    Route route = bus.getRouteId();
                    if (route != null && route.getStops() != null) {
                        tvRouteName.setText(route.getRouteName());
                        tvStopsCount.setText(route.getStops().size() + " stops");
                        drawRoute(route.getStops());
                        if (!route.getStops().isEmpty()) {
                            Stop next = route.getStops().get(0);
                            tvUpcomingStopName.setText(next.getName() + " (" + next.getEta() + ")");
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Bus> call, Throwable t) {}
        });
    }

    private void drawRoute(List<Stop> stops) {
        if (mMap == null || stops.isEmpty()) return;
        PolylineOptions options = new PolylineOptions().color(Color.parseColor("#3F51B5")).width(12f);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        
        for (Stop s : stops) {
            LatLng pos = new LatLng(s.getLatitude(), s.getLongitude());
            options.add(pos);
            builder.include(pos);
            mMap.addMarker(new MarkerOptions().position(pos).title(s.getName()).snippet("Time: " + s.getEta())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }
        if (routePolyline != null) routePolyline.remove();
        routePolyline = mMap.addPolyline(options);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
    }

    private void startTripProcess() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isTripActive = true;
            btnStartTrip.setVisibility(View.GONE);
            btnEndTrip.setVisibility(View.VISIBLE);
            tvTripStatus.setText("Status: ACTIVE");
            tvTripStatus.setTextColor(Color.GREEN);
            upcomingStopCard.setVisibility(View.VISIBLE);
            
            Intent serviceIntent = new Intent(this, LocationTrackingService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void stopTrackingService() {
        isTripActive = false;
        btnStartTrip.setVisibility(View.VISIBLE);
        btnEndTrip.setVisibility(View.GONE);
        tvTripStatus.setText("Status: INACTIVE");
        tvTripStatus.setTextColor(Color.GRAY);
        stopService(new Intent(this, LocationTrackingService.class));
    }
}
