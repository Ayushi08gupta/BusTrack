package com.college.bustrack;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.Bus;
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
import com.google.android.material.chip.Chip;

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

public class TrackBusActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "TrackBusActivity";
    private GoogleMap mMap;
    private Socket mSocket;
    private String busId;
    private Marker busMarker;
    private Polyline routePolyline;
    private List<Marker> stopMarkers = new ArrayList<>();

    private TextView tvBusNumber, tvDriverName, tvETA, tvDistance, tvNextStop, tvRemainingStops;
    private Chip chipStatus;
    private ImageButton btnBack;
    private MaterialButton btnReportIssue;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_bus);

        busId = getIntent().getStringExtra("BUS_ID");
        if (busId == null) {
            Toast.makeText(this, "Bus ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);

        initViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fetchBusDetails();
        setupSocket();
    }

    private void initViews() {
        tvBusNumber = findViewById(R.id.tvBusNumber);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvETA = findViewById(R.id.tvETA);
        tvDistance = findViewById(R.id.tvDistance);
        tvNextStop = findViewById(R.id.tvNextStop);
        tvRemainingStops = findViewById(R.id.tvRemainingStops);
        chipStatus = findViewById(R.id.chipStatus);
        btnBack = findViewById(R.id.btnBack);
        btnReportIssue = findViewById(R.id.btnReportIssue);

        btnBack.setOnClickListener(v -> finish());
        
        btnReportIssue.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportIssueActivity.class);
            intent.putExtra("BUS_ID", busId);
            startActivity(intent);
        });
    }

    private void fetchBusDetails() {
        apiService.getBusFullDetails(sessionManager.getToken(), busId).enqueue(new Callback<Bus>() {
            @Override
            public void onResponse(Call<Bus> call, Response<Bus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                }
            }
            @Override
            public void onFailure(Call<Bus> call, Throwable t) {
                Toast.makeText(TrackBusActivity.this, "Error fetching details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(Bus bus) {
        tvBusNumber.setText("Bus: " + (bus.getBusNumber() != null ? bus.getBusNumber() : "N/A"));
        if (bus.getDriverId() != null) {
            tvDriverName.setText("Driver: " + bus.getDriverId().getName());
        }
        updateStatus(bus.getStatus());
        
        if (bus.getRouteId() != null && bus.getRouteId().getStops() != null && !bus.getRouteId().getStops().isEmpty()) {
            List<Stop> stops = bus.getRouteId().getStops();
            drawRoute(stops);
            
            Stop next = stops.get(0);
            tvNextStop.setText("Next Stop: " + next.getName());
            tvETA.setText(next.getEta()); // Show the first stop's scheduled time as initial ETA
            tvRemainingStops.setText(stops.size() + " stops total");
        }
    }

    private void updateStatus(String status) {
        if ("active".equalsIgnoreCase(status)) {
            chipStatus.setText("LIVE");
            chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_dark);
        } else {
            chipStatus.setText("OFFLINE");
            chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
        }
    }

    private void setupSocket() {
        try {
            mSocket = IO.socket(ApiClient.getBaseUrl());
            mSocket.on(Socket.EVENT_CONNECT, args -> {
                JSONObject joinData = new JSONObject();
                try {
                    joinData.put("busId", busId);
                    mSocket.emit("join:bus", joinData);
                } catch (JSONException e) { e.printStackTrace(); }
            });

            mSocket.on("location:update", args -> {
                JSONObject data = (JSONObject) args[0];
                runOnUiThread(() -> {
                    try {
                        if (data.getString("busId").equals(busId)) {
                            double lat = data.getDouble("latitude");
                            double lng = data.getDouble("longitude");
                            updateBusLocation(new LatLng(lat, lng));
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                });
            });

            mSocket.connect();
        } catch (URISyntaxException e) { e.printStackTrace(); }
    }

    private void updateBusLocation(LatLng pos) {
        if (mMap == null) return;
        if (busMarker == null) {
            busMarker = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title("Live Bus")
                    .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_directions)));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
        } else {
            busMarker.setPosition(pos);
        }
    }

    private void drawRoute(List<Stop> stops) {
        if (mMap == null || stops == null || stops.isEmpty()) return;

        PolylineOptions polyOptions = new PolylineOptions()
                .color(Color.parseColor("#3F51B5"))
                .width(12f)
                .geodesic(true);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (int i = 0; i < stops.size(); i++) {
            Stop stop = stops.get(i);
            LatLng pos = new LatLng(stop.getLatitude(), stop.getLongitude());
            polyOptions.add(pos);
            builder.include(pos);

            // Added scheduled time (ETA) to the marker snippet
            mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title((i + 1) + ". " + stop.getName())
                    .snippet("Scheduled Arrival: " + stop.getEta())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        if (routePolyline != null) routePolyline.remove();
        routePolyline = mMap.addPolyline(polyOptions);
        
        // Adjust camera to fit the whole route
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
        }
    }
}
