package com.college.bustrack;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.Bus;
import com.college.bustrack.models.Stop;
import com.college.bustrack.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackBusActivity extends AppCompatActivity implements OnMapReadyCallback {

    private com.google.android.gms.maps.MapView map;
    private GoogleMap googleMap;
    private Marker busMarker;
    private Polyline routePolyline;
    
    private Socket mSocket;
    private SessionManager sessionManager;
    private ApiService apiService;
    
    private TextView tvBusId, tvDriver, tvSpeed, tvETA, tvNextStop, tvDistance, tvLiveStatus;
    private LinearLayout timelineContainer;
    private MaterialButton btnBack;
    private ExtendedFloatingActionButton fabReport;

    private String busId;
    private Bus currentBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_bus);

        busId = getIntent().getStringExtra("bus_id");
        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();

        initViews();
        map.onCreate(savedInstanceState);
        map.getMapAsync(this);
        loadBusDetails();
    }

    private void initViews() {
        map = findViewById(R.id.mapView);
        tvBusId = findViewById(R.id.tvBusId);
        tvDriver = findViewById(R.id.tvDriver);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvETA = findViewById(R.id.tvETA);
        tvNextStop = findViewById(R.id.tvNextStop);
        tvDistance = findViewById(R.id.tvDistance);
        tvLiveStatus = findViewById(R.id.tvLiveStatus);
        timelineContainer = findViewById(R.id.timelineContainer);
        btnBack = findViewById(R.id.btnBack);
        fabReport = findViewById(R.id.fabReport);

        btnBack.setOnClickListener(v -> finish());
        fabReport.setOnClickListener(v -> {
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        setupMap();
        setupSocket();
    }

    private void setupMap() {
        if (googleMap == null) return;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(23.2599, 77.4126), 15f));
    }

    private void loadBusDetails() {
        apiService.searchBuses(sessionManager.getToken(), busId).enqueue(new Callback<List<Bus>>() {
            @Override
            public void onResponse(Call<List<Bus>> call, Response<List<Bus>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    currentBus = response.body().get(0);
                    updateUI(currentBus);
                    drawRoute(currentBus.getRouteId().getStops());
                }
            }
            @Override
            public void onFailure(Call<List<Bus>> call, Throwable t) {
                Toast.makeText(TrackBusActivity.this, "Error loading bus data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(Bus bus) {
        tvBusId.setText("Bus: " + bus.getBusNumber());
        tvDriver.setText("Driver: " + (bus.getDriverId() != null ? bus.getDriverId().getName() : "N/A"));
        updateStatus(bus.getStatus());
        
        if (bus.getCurrentLocation() != null) {
            updateBusLocation(
                new LatLng(bus.getCurrentLocation().getLatitude(), bus.getCurrentLocation().getLongitude()),
                bus.getCurrentLocation().getSpeed(),
                bus.getCurrentLocation().getHeading()
            );
        }
        
        if (bus.getRouteId() != null && bus.getRouteId().getStops() != null) {
            buildTimeline(bus.getRouteId().getStops());
        }
    }

    private void drawRoute(List<Stop> stops) {
        if (googleMap == null || stops == null) return;

        if (routePolyline != null) routePolyline.remove();

        PolylineOptions polylineOptions = new PolylineOptions()
                .color(Color.parseColor("#C4B5FD"))
                .width(12f);

        for (Stop stop : stops) {
            LatLng latLng = new LatLng(stop.getLatitude(), stop.getLongitude());
            polylineOptions.add(latLng);
            
            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(stop.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }
        routePolyline = googleMap.addPolyline(polylineOptions);
    }

    private void buildTimeline(List<Stop> stops) {
        timelineContainer.removeAllViews();
        for (int i = 0; i < stops.size(); i++) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_timeline_stop, timelineContainer, false);
            TextView tvStopName = itemView.findViewById(R.id.tvStopName);
            View indicator = itemView.findViewById(R.id.indicator);
            View line = itemView.findViewById(R.id.line);

            tvStopName.setText(stops.get(i).getName());
            
            // For now, let's mark the first stop as current/active for UI demo
            if (i == 0) {
                tvStopName.setTextColor(ContextCompat.getColor(this, R.color.primary_dark));
                tvStopName.setTypeface(null, android.graphics.Typeface.BOLD);
                ((GradientDrawable)indicator.getBackground()).setColor(ContextCompat.getColor(this, R.color.primary));
            }

            if (i == stops.size() - 1) {
                line.setVisibility(View.GONE);
            }

            timelineContainer.addView(itemView);
        }
    }

    private void setupSocket() {
        try {
            mSocket = IO.socket(ApiClient.getBaseUrl());
            mSocket.on(Socket.EVENT_CONNECT, args -> {
                JSONObject data = new JSONObject();
                try {
                    data.put("busId", busId);
                    mSocket.emit("join:bus", data);
                } catch (JSONException e) { e.printStackTrace(); }
            });

            mSocket.on("location:update", args -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    if (data.getString("busId").equals(busId)) {
                        double lat = data.getDouble("latitude");
                        double lng = data.getDouble("longitude");
                        double speed = data.optDouble("speed", 0.0);
                        double heading = data.optDouble("heading", 0.0);

                        new Handler(Looper.getMainLooper()).post(() -> 
                            updateBusLocation(new LatLng(lat, lng), (float) speed, (float) heading)
                        );
                    }
                } catch (JSONException e) { e.printStackTrace(); }
            });

            mSocket.on("trip:started", args -> {
                new Handler(Looper.getMainLooper()).post(() -> updateStatus("active"));
            });

            mSocket.on("trip:ended", args -> {
                new Handler(Looper.getMainLooper()).post(() -> updateStatus("inactive"));
            });

            mSocket.on("bus:offline", args -> {
                new Handler(Looper.getMainLooper()).post(() -> updateStatus("inactive"));
            });

            mSocket.connect();
        } catch (URISyntaxException e) { e.printStackTrace(); }
    }

    private void updateBusLocation(LatLng point, float speed, float heading) {
        if (googleMap == null || point == null) return;

        if (busMarker == null) {
            busMarker = googleMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title("Bus Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        } else {
            busMarker.setPosition(point);
            busMarker.setRotation(heading);
        }

        googleMap.animateCamera(CameraUpdateFactory.newLatLng(point));
        tvLiveStatus.setText("LIVE");
        tvLiveStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.success_green));
        tvLiveStatus.setTextColor(Color.WHITE);

        // Convert speed from m/s to km/h
        int speedKmH = (int) (speed * 3.6);
        tvSpeed.setText(speedKmH + " km/h");

        // Keeping mocked ETA and Distance for now as per requirement
        tvDistance.setText("1.8 km");
        tvETA.setText("08:35 AM");
    }

    private void updateStatus(String status) {
        if ("active".equalsIgnoreCase(status)) {
            tvLiveStatus.setText("LIVE");
            tvLiveStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.success_green));
        } else {
            tvLiveStatus.setText("OFFLINE");
            tvLiveStatus.setBackgroundColor(Color.GRAY);
        }
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
        if (mSocket != null) mSocket.disconnect();
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
