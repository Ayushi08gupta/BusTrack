package com.college.bustrack;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.Bus;
import com.college.bustrack.models.Route;
import com.college.bustrack.models.Stop;
import com.college.bustrack.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentDashboardActivity extends AppCompatActivity {

    private MapView map;
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
        
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_student_dashboard);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();
        initViews();
        setupMap();
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
        map = findViewById(R.id.mapView);

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
            finish();
        });
    }

    private void setupMap() {
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        map.getController().setCenter(new GeoPoint(23.2599, 77.4126)); // Default center (Bhopal/India example)

        busMarker = new Marker(map);
        busMarker.setTitle("Live Bus");
        busMarker.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_directions));
        busMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(busMarker);

        routePolyline = new Polyline();
        routePolyline.getOutlinePaint().setColor(Color.BLUE);
        routePolyline.getOutlinePaint().setStrokeWidth(8f);
        map.getOverlays().add(routePolyline);
    }

    private void performSearch() {
        String query = etBusSearch.getText().toString().trim();
        if (query.isEmpty()) return;

        apiService.searchBuses(sessionManager.getToken(), query).enqueue(new Callback<List<Bus>>() {
            @Override
            public void onResponse(Call<List<Bus>> call, Response<List<Bus>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showBusSelectionDialog(response.body());
                } else {
                    Toast.makeText(StudentDashboardActivity.this, "No buses found for this route", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Bus>> call, Throwable t) {
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
            public void onResponse(Call<Bus> call, Response<Bus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    startTrackingBus(response.body());
                }
            }
            @Override
            public void onFailure(Call<Bus> call, Throwable t) {}
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
        if (bus.getCurrentLocation() != null) {
            updateBusLocation(new GeoPoint(
                bus.getCurrentLocation().getLatitude(), 
                bus.getCurrentLocation().getLongitude()
            ));
        }
    }

    private void drawRoute(List<Stop> stops) {
        // Clear old stop markers
        for (Marker m : stopMarkers) map.getOverlays().remove(m);
        stopMarkers.clear();

        List<GeoPoint> points = new ArrayList<>();
        for (Stop stop : stops) {
            GeoPoint gp = new GeoPoint(stop.getLatitude(), stop.getLongitude());
            points.add(gp);
            
            Marker stopMarker = new Marker(map);
            stopMarker.setPosition(gp);
            stopMarker.setTitle(stop.getName());
            stopMarker.setIcon(ContextCompat.getDrawable(this, android.R.drawable.presence_online));
            stopMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            map.getOverlays().add(stopMarker);
            stopMarkers.add(stopMarker);
        }
        routePolyline.setPoints(points);
        if (!points.isEmpty()) {
            map.getController().animateTo(points.get(0));
        }
        map.invalidate();
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
                        runOnUiThread(() -> updateBusLocation(new GeoPoint(lat, lng)));
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

    private void updateBusLocation(GeoPoint point) {
        busMarker.setPosition(point);
        busMarker.setVisible(true);
        map.getController().animateTo(point);
        updateStatusTag("active");
        map.invalidate();
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
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
        }
    }
}
