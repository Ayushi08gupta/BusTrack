package com.college.bustrack;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

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

public class StudentDashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker busMarker;
    private Polyline routePolyline;
    private List<Marker> stopMarkers = new ArrayList<>();
    
    private Socket mSocket;
    private SessionManager sessionManager;
    private ApiService apiService;
    
    private TextView tvStudentName, tvBusNo, tvRoute, tvETA, tvNextStop, tvStatusTag;
    private EditText etBusSearch;
    private ImageButton btnLogout, btnSearch;

    private String selectedBusId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();
        initViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupSocket();

        // Auto-load assigned bus if exists
        fetchAssignedBus();
    }

    private void initViews() {
        tvStudentName = findViewById(R.id.tvStudentName);
        tvBusNo = findViewById(R.id.tvBusNo);
        tvRoute = findViewById(R.id.tvRoute);
        tvETA = findViewById(R.id.tvETA);
        tvNextStop = findViewById(R.id.tvNextStop);
        tvStatusTag = findViewById(R.id.tvStatusTag);
        etBusSearch = findViewById(R.id.etBusSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnLogout = findViewById(R.id.btnLogout);

        if (sessionManager.getUserName() != null) {
            tvStudentName.setText("Welcome, " + sessionManager.getUserName());
        }

        btnSearch.setOnClickListener(v -> performSearch());
        etBusSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        LatLng defaultCenter = new LatLng(23.2599, 77.4126); // Bhopal
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCenter, 13f));
    }

    private void performSearch() {
        String query = etBusSearch.getText().toString().trim();
        if (query.isEmpty()) return;

        apiService.searchBuses(sessionManager.getToken(), query).enqueue(new Callback<List<Bus>>() {
            @Override
            public void onResponse(@NonNull Call<List<Bus>> call, @NonNull Response<List<Bus>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showBusSelectionDialog(response.body());
                } else {
                    Toast.makeText(StudentDashboardActivity.this, "No buses found for this route", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Bus>> call, @NonNull Throwable t) {
                Toast.makeText(StudentDashboardActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBusSelectionDialog(List<Bus> buses) {
        if (buses.isEmpty()) {
            Toast.makeText(this, "No buses available on this route", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] busNames = new String[buses.size()];
        for (int i = 0; i < buses.size(); i++) {
            busNames[i] = "Bus " + buses.get(i).getBusNumber() + " (" + buses.get(i).getRouteId().getRouteName() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Select a Bus to Track")
                .setItems(busNames, (dialog, which) -> startTrackingBus(buses.get(which)))
                .show();
    }

    private void fetchAssignedBus() {
        apiService.getStudentBusInfo(sessionManager.getToken()).enqueue(new Callback<Bus>() {
            @Override
            public void onResponse(@NonNull Call<Bus> call, @NonNull Response<Bus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    startTrackingBus(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<Bus> call, @NonNull Throwable t) {}
        });
    }

    private void startTrackingBus(Bus bus) {
        // Leave old room if any
        if (selectedBusId != null && mSocket != null) {
            mSocket.emit("leave:bus", selectedBusId);
        }

        selectedBusId = bus.getId();
        tvBusNo.setText("Bus: " + bus.getBusNumber());
        
        if (bus.getRouteId() != null) {
            tvRoute.setText(bus.getRouteId().getRouteName());
            drawRoute(bus.getRouteId().getStops());
        }

        // Update Status
        updateStatusTag(bus.getStatus());

        // Join socket room for real-time updates
        if (mSocket != null && mSocket.connected()) {
            joinBusRoom();
        }

        // If bus already has a location, show it
        if (bus.getCurrentLocation() != null && mMap != null) {
            updateBusLocation(new LatLng(
                bus.getCurrentLocation().getLatitude(), 
                bus.getCurrentLocation().getLongitude()
            ));
        }
    }

    private void drawRoute(List<Stop> stops) {
        if (mMap == null || stops == null) return;

        // Clear old stop markers
        for (Marker m : stopMarkers) m.remove();
        stopMarkers.clear();
        if (routePolyline != null) routePolyline.remove();

        PolylineOptions polylineOptions = new PolylineOptions()
                .color(Color.BLUE)
                .width(8f);

        for (Stop stop : stops) {
            LatLng pos = new LatLng(stop.getLatitude(), stop.getLongitude());
            polylineOptions.add(pos);
            
            Marker stopMarker = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(stop.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            if (stopMarker != null) stopMarkers.add(stopMarker);
        }
        routePolyline = mMap.addPolyline(polylineOptions);
        
        if (!stops.isEmpty()) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(stops.get(0).getLatitude(), stops.get(0).getLongitude()), 14f));
        }
    }

    private void setupSocket() {
        try {
            mSocket = IO.socket(ApiClient.getBaseUrl());
            mSocket.on(Socket.EVENT_CONNECT, args -> runOnUiThread(() -> {
                if (selectedBusId != null) joinBusRoom();
            }));

            mSocket.on("location:update", args -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    String busId = data.getString("busId");
                    if (busId.equals(selectedBusId)) {
                        double lat = data.getDouble("latitude");
                        double lng = data.getDouble("longitude");
                        runOnUiThread(() -> updateBusLocation(new LatLng(lat, lng)));
                    }
                } catch (JSONException e) {
                    Log.e("Socket", "Error parsing location", e);
                }
            });

            mSocket.on("bus:offline", args -> {
                runOnUiThread(() -> updateStatusTag("inactive"));
            });

            mSocket.connect();
        } catch (URISyntaxException e) {
            Log.e("Socket", "URI Syntax Error", e);
        }
    }

    private void joinBusRoom() {
        try {
            JSONObject data = new JSONObject();
            data.put("busId", selectedBusId);
            mSocket.emit("join:bus", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateBusLocation(LatLng point) {
        if (mMap == null || point == null || (point.latitude == 0 && point.longitude == 0)) return;
        
        if (busMarker == null) {
            busMarker = mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title("Live Bus")
                    .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_directions)));
        } else {
            busMarker.setPosition(point);
        }
        
        mMap.animateCamera(CameraUpdateFactory.newLatLng(point));
        updateStatusTag("active");
    }

    private void updateStatusTag(String status) {
        if ("active".equalsIgnoreCase(status)) {
            tvStatusTag.setText("LIVE");
            tvStatusTag.setBackgroundColor(Color.parseColor("#22C55E"));
        } else {
            tvStatusTag.setText("OFFLINE");
            tvStatusTag.setBackgroundColor(Color.parseColor("#718096"));
        }
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
