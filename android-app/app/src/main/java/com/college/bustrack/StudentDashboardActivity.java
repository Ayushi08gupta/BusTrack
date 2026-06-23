package com.college.bustrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.Bus;
import com.college.bustrack.models.Route;
import com.college.bustrack.models.Stop;
import com.college.bustrack.models.User;
import com.college.bustrack.utils.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentDashboardActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private com.google.android.gms.maps.MapView map;
    private GoogleMap googleMap;
    private Marker busMarker;
    private Polyline routePolyline;
    private List<Marker> stopMarkers = new ArrayList<>();
    
    private Socket mSocket;
    private SessionManager sessionManager;
    private ApiService apiService;
    
    // Main UI Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNav;
    private Toolbar toolbar;
    private FrameLayout homeContainer, busesContainer, trackContainer, profileContainer;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private MaterialCardView bottomSheet, bottomNavCard;

    // View Components
    private TextView tvBusNo, tvRoute, tvETA, tvNextStop, tvStatusTag, tvSpeed, tvDriverName;
    private TextView tvFloatingBusNo, tvFloatingStatus;
    private EditText etHomeSearch, etBusSearch, etBusListSearch;
    private ImageView btnSearch, btnBackFromTrack, ivToolbarProfile;
    
    private BusAdapter nearbyBusAdapter, allBusAdapter;
    private List<Bus> allBuses = new ArrayList<>();
    private String selectedBusId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();
        
        initViews();
        setupNavigation();
        setupDrawer();
        
        map.onCreate(savedInstanceState);
        map.getMapAsync(this);

        fetchAssignedBus();
        loadAllBuses();
    }

    private void initViews() {
        // Core Layouts
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        bottomNav = findViewById(R.id.bottomNav);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        homeContainer = findViewById(R.id.homeContainer);
        busesContainer = findViewById(R.id.busesContainer);
        trackContainer = findViewById(R.id.trackContainer);
        profileContainer = findViewById(R.id.profileContainer);
        bottomNavCard = findViewById(R.id.bottomNavCard);

        // Tracking Screen (Within trackContainer)
        map = findViewById(R.id.mapView);
        bottomSheet = findViewById(R.id.bottomSheet);
        tvBusNo = findViewById(R.id.tvBusNo);
        tvRoute = findViewById(R.id.tvRoute);
        tvETA = findViewById(R.id.tvETA);
        tvNextStop = findViewById(R.id.tvNextStop);
        tvStatusTag = findViewById(R.id.tvStatusTag);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvFloatingBusNo = findViewById(R.id.tvFloatingBusNo);
        tvFloatingStatus = findViewById(R.id.tvFloatingStatus);
        
        etBusSearch = findViewById(R.id.etBusSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnBackFromTrack = findViewById(R.id.btnBackFromTrack);
        
        // Home Screen Views
        etHomeSearch = homeContainer.findViewById(R.id.etHomeSearch);
        ivToolbarProfile = findViewById(R.id.ivToolbarProfile);

        // Available Buses Tab Controls
        etBusListSearch = busesContainer.findViewById(R.id.etBusListSearch);
        View btnFilter = busesContainer.findViewById(R.id.btnFilter);
        View btnSort = busesContainer.findViewById(R.id.btnSort);

        if (etBusListSearch != null) {
            etBusListSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterBuses(s.toString()); }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
        if (btnFilter != null) btnFilter.setOnClickListener(v -> showFilterDialog());
        if (btnSort != null) btnSort.setOnClickListener(v -> showSortDialog());
        
        // Quick Action Buttons
        View btnQuickTrack = homeContainer.findViewById(R.id.btnQuickTrack);
        View btnQuickRoutes = homeContainer.findViewById(R.id.btnQuickRoutes);
        View btnQuickSchedule = homeContainer.findViewById(R.id.btnQuickSchedule);
        View btnSeeAll = homeContainer.findViewById(R.id.btnSeeAllBuses);

        if (btnQuickTrack != null) btnQuickTrack.setOnClickListener(v -> {
            if (sessionManager.getAssignedBusId() != null) {
                apiService.getBusFullDetails(sessionManager.getToken(), sessionManager.getAssignedBusId()).enqueue(new Callback<Bus>() {
                    @Override
                    public void onResponse(Call<Bus> call, Response<Bus> response) {
                        if (response.isSuccessful() && response.body() != null) startTrackingBus(response.body());
                    }
                    @Override public void onFailure(Call<Bus> call, Throwable t) {}
                });
            } else {
                Toast.makeText(this, "No assigned bus to track", Toast.LENGTH_SHORT).show();
            }
        });
        if (btnQuickRoutes != null) btnQuickRoutes.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_buses));
        if (btnSeeAll != null) btnSeeAll.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_buses));
        if (btnQuickSchedule != null) btnQuickSchedule.setOnClickListener(v -> 
            Toast.makeText(this, "Schedule coming soon", Toast.LENGTH_SHORT).show());

        if (ivToolbarProfile != null) ivToolbarProfile.setOnClickListener(v -> showState("profile"));

        // Profile Buttons
        View btnEditProfile = profileContainer.findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // Bottom Sheet Initialization
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Search Handlers
        if (btnSearch != null) btnSearch.setOnClickListener(v -> performSearch(etBusSearch.getText().toString()));
        if (btnBackFromTrack != null) btnBackFromTrack.setOnClickListener(v -> showState("home"));

        setupAdapters();
        setupSearchActions();
    }

    private void setupDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        
        // Update Drawer Header
        View header = navigationView.getHeaderView(0);
        TextView tvName = header.findViewById(R.id.tvDrawerName);
        TextView tvEmail = header.findViewById(R.id.tvDrawerEmail);
        
        if (sessionManager.getUserName() != null) tvName.setText(sessionManager.getUserName());
        if (sessionManager.getUserEmail() != null) tvEmail.setText(sessionManager.getUserEmail());
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showState("home");
            } else if (id == R.id.nav_buses) {
                showState("buses");
            } else if (id == R.id.nav_track) {
                if (selectedBusId != null) {
                    showState("track");
                } else {
                    Toast.makeText(this, "Select a bus to track first", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else if (id == R.id.nav_profile) {
                showState("profile");
            }
            return true;
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        } else if (id == R.id.nav_my_bus) {
            fetchAssignedBus();
        } else if (id == R.id.nav_buses) {
            bottomNav.setSelectedItemId(R.id.nav_buses);
        } else if (id == R.id.nav_track) {
            bottomNav.setSelectedItemId(R.id.nav_track);
        } else if (id == R.id.nav_profile) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
        } else if (id == R.id.nav_logout) {
            logout();
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showState(String state) {
        homeContainer.setVisibility(View.GONE);
        busesContainer.setVisibility(View.GONE);
        trackContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.GONE);
        bottomNavCard.setVisibility(View.VISIBLE);

        switch (state) {
            case "home":
                homeContainer.setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("BusTrack");
                fetchAssignedBus();
                loadAllBuses();
                break;
            case "buses":
                busesContainer.setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Available Buses");
                loadAllBuses();
                break;
            case "track":
                trackContainer.setVisibility(View.VISIBLE);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Tracking Bus");
                break;
            case "profile":
                profileContainer.setVisibility(View.VISIBLE);
                setupProfileData();
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Student Profile");
                break;
        }
    }

    private void setupProfileData() {
        TextView tvName = profileContainer.findViewById(R.id.tvProfileName);
        TextView tvId = profileContainer.findViewById(R.id.tvProfileId);
        
        apiService.getProfile(sessionManager.getToken()).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    tvName.setText(user.getName());
                    tvId.setText("ID: " + user.getId());

                    updateProfileInfo(R.id.infoEmail, "Email", user.getEmail(), android.R.drawable.ic_dialog_email);
                    updateProfileInfo(R.id.infoDept, "Department", user.getDepartment(), android.R.drawable.ic_menu_info_details);
                    updateProfileInfo(R.id.infoSemester, "Semester", user.getSemester(), android.R.drawable.ic_menu_today);
                }
            }
            @Override public void onFailure(Call<User> call, Throwable t) {}
        });
        
        profileContainer.findViewById(R.id.btnLogoutProfile).setOnClickListener(v -> logout());
    }

    private void updateProfileInfo(int includeId, String label, String value, int iconRes) {
        View view = profileContainer.findViewById(includeId);
        if (view == null) return;
        ((TextView) view.findViewById(R.id.tvLabel)).setText(label);
        ((TextView) view.findViewById(R.id.tvValue)).setText(value != null ? value : "Not specified");
        ((ImageView) view.findViewById(R.id.ivIcon)).setImageResource(iconRes);
    }

    private void filterBuses(String query) {
        if (allBuses == null) return;
        List<Bus> filtered = new ArrayList<>();
        for (Bus b : allBuses) {
            String q = query.toLowerCase();
            if (b.getBusNumber().toLowerCase().contains(q) || 
               (b.getRouteId() != null && b.getRouteId().getRouteName().toLowerCase().contains(q))) {
                filtered.add(b);
            }
        }
        allBusAdapter.updateBuses(filtered);
    }

    private void showFilterDialog() {
        String[] options = {"All", "Online Only", "Offline Only"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Filter Buses")
                .setItems(options, (dialog, which) -> {
                    List<Bus> filtered = new ArrayList<>();
                    for (Bus b : allBuses) {
                        boolean online = "active".equalsIgnoreCase(b.getStatus());
                        if (which == 0) filtered.add(b);
                        else if (which == 1 && online) filtered.add(b);
                        else if (which == 2 && !online) filtered.add(b);
                    }
                    allBusAdapter.updateBuses(filtered);
                }).show();
    }

    private void showSortDialog() {
        String[] options = {"Bus Number", "Route Name", "Status"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sort Buses")
                .setItems(options, (dialog, which) -> {
                    List<Bus> sorted = new ArrayList<>(allBuses);
                    java.util.Collections.sort(sorted, (b1, b2) -> {
                        if (which == 0) return b1.getBusNumber().compareTo(b2.getBusNumber());
                        if (which == 1) {
                            String r1 = b1.getRouteId() != null ? b1.getRouteId().getRouteName() : "";
                            String r2 = b2.getRouteId() != null ? b2.getRouteId().getRouteName() : "";
                            return r1.compareTo(r2);
                        }
                        return (b1.getStatus() != null ? b1.getStatus() : "").compareTo(b2.getStatus() != null ? b2.getStatus() : "");
                    });
                    allBusAdapter.updateBuses(sorted);
                }).show();
    }

    private void showEditProfileDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etDept = new EditText(this);
        etDept.setHint("Department");
        layout.addView(etDept);

        EditText etSem = new EditText(this);
        etSem.setHint("Semester");
        layout.addView(etSem);

        // Pre-fill with current values if available
        apiService.getProfile(sessionManager.getToken()).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    etDept.setText(response.body().getDepartment());
                    etSem.setText(response.body().getSemester());
                }
            }
            @Override public void onFailure(Call<User> call, Throwable t) {}
        });

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String dept = etDept.getText().toString();
                    String sem = etSem.getText().toString();
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("department", dept);
                    body.put("semester", sem);
                    
                    apiService.updateProfile(sessionManager.getToken(), body).enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            if (response.isSuccessful()) {
                                setupProfileData();
                                Toast.makeText(StudentDashboardActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onFailure(Call<User> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupSearchActions() {
        if (etHomeSearch != null) {
            etHomeSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(etHomeSearch.getText().toString());
                    return true;
                }
                return false;
            });
        }
    }

    private void setupAdapters() {
        RecyclerView rvNearby = findViewById(R.id.rvNearbyBuses);
        RecyclerView rvAll = findViewById(R.id.rvAllBuses);

        nearbyBusAdapter = new BusAdapter(new ArrayList<>(), this::startTrackingBus);
        allBusAdapter = new BusAdapter(new ArrayList<>(), this::startTrackingBus);

        if (rvNearby != null) {
            rvNearby.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvNearby.setAdapter(nearbyBusAdapter);
        }
        if (rvAll != null) {
            rvAll.setLayoutManager(new LinearLayoutManager(this));
            rvAll.setAdapter(allBusAdapter);
        }
    }

    private void loadAllBuses() {
        apiService.searchBuses(sessionManager.getToken(), "").enqueue(new Callback<List<Bus>>() {
            @Override
            public void onResponse(Call<List<Bus>> call, Response<List<Bus>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allBuses = response.body();
                    allBusAdapter.updateBuses(allBuses);
                    
                    View emptyState = busesContainer.findViewById(R.id.emptyState);
                    if (emptyState != null) {
                        emptyState.setVisibility(allBuses.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    List<Bus> activeBuses = new ArrayList<>();
                    for (Bus b : allBuses) {
                        if ("active".equalsIgnoreCase(b.getStatus())) activeBuses.add(b);
                    }
                    nearbyBusAdapter.updateBuses(activeBuses);
                }
            }
            @Override
            public void onFailure(Call<List<Bus>> call, Throwable t) {}
        });
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        apiService.searchBuses(sessionManager.getToken(), query).enqueue(new Callback<List<Bus>>() {
            @Override
            public void onResponse(Call<List<Bus>> call, Response<List<Bus>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showBusSelectionDialog(response.body());
                } else {
                    Toast.makeText(StudentDashboardActivity.this, "No buses found", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Bus>> call, Throwable t) {
                Toast.makeText(StudentDashboardActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
            }
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    private void showBusSelectionDialog(List<Bus> buses) {
        if (buses.isEmpty()) return;
        String[] busNames = new String[buses.size()];
        for (int i = 0; i < buses.size(); i++) {
            busNames[i] = "Bus " + buses.get(i).getBusNumber() + " (" + buses.get(i).getRouteId().getRouteName() + ")";
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select a Bus to Track")
                .setItems(busNames, (dialog, which) -> startTrackingBus(buses.get(which)))
                .show();
    }

    private void fetchAssignedBus() {
        apiService.getStudentBusInfo(sessionManager.getToken()).enqueue(new Callback<Bus>() {
            @Override
            public void onResponse(Call<Bus> call, Response<Bus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Bus bus = response.body();
                    sessionManager.setAssignedBusId(bus.getId());
                    updateAssignedBusCard(bus);
                } else {
                    View card = homeContainer.findViewById(R.id.assignedBusCard);
                    if (card != null) card.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(Call<Bus> call, Throwable t) {}
        });
    }

    private void updateAssignedBusCard(Bus bus) {
        View card = homeContainer.findViewById(R.id.assignedBusCard);
        if (card == null || bus == null) return;
        card.setVisibility(View.VISIBLE);
        
        ((TextView) card.findViewById(R.id.tvBusNumber)).setText(bus.getBusNumber());
        if (bus.getRouteId() != null) ((TextView) card.findViewById(R.id.tvRoutePath)).setText(bus.getRouteId().getRouteName());
        
        boolean isActive = "active".equalsIgnoreCase(bus.getStatus());
        TextView tvStatus = card.findViewById(R.id.tvStatus);
        tvStatus.setText(isActive ? "ONLINE" : "OFFLINE");
        tvStatus.setTextColor(isActive ? ContextCompat.getColor(this, R.color.success) : ContextCompat.getColor(this, R.color.danger));
        
        MaterialCardView statusCard = card.findViewById(R.id.statusChipCard);
        if (statusCard != null) {
            statusCard.setCardBackgroundColor(isActive ? ContextCompat.getColor(this, R.color.primary_light) : Color.LTGRAY);
        }
        
        card.findViewById(R.id.btnTrackNow).setOnClickListener(v -> startTrackingBus(bus));
        
        // Update profile transport info if profile is visible
        updateProfileInfo(R.id.infoAssignedBus, "Assigned Bus", bus.getBusNumber(), android.R.drawable.ic_menu_directions);
        if (bus.getRouteId() != null)
            updateProfileInfo(R.id.infoAssignedRoute, "Assigned Route", bus.getRouteId().getRouteName(), android.R.drawable.ic_menu_directions);
    }

    private void startTrackingBus(Bus bus) {
        if (bus == null) return;
        showState("track");
        bottomNav.getMenu().findItem(R.id.nav_track).setChecked(true);
        selectedBusId = bus.getId();
        tvBusNo.setText(bus.getBusNumber());
        tvFloatingBusNo.setText(bus.getBusNumber());
        if (bus.getRouteId() != null) {
            tvRoute.setText(bus.getRouteId().getRouteName());
            drawRoute(bus.getRouteId().getStops());
        }
        tvDriverName.setText(bus.getDriverId() != null ? bus.getDriverId().getName() : "N/A");
        updateStatusTag(bus.getStatus());
        if (mSocket != null && mSocket.connected()) joinBusRoom();
        if (bus.getCurrentLocation() != null) {
            updateBusLocation(new LatLng(bus.getCurrentLocation().getLatitude(), bus.getCurrentLocation().getLongitude()));
        }
    }

    private void drawRoute(List<Stop> stops) {
        if (googleMap == null || stops == null) return;
        for (Marker m : stopMarkers) m.remove();
        stopMarkers.clear();
        if (routePolyline != null) routePolyline.remove();
        PolylineOptions polylineOptions = new PolylineOptions().color(ContextCompat.getColor(this, R.color.primary)).width(10f);
        for (Stop stop : stops) {
            LatLng latLng = new LatLng(stop.getLatitude(), stop.getLongitude());
            polylineOptions.add(latLng);
            Marker stopMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng).title(stop.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            if (stopMarker != null) stopMarkers.add(stopMarker);
        }
        routePolyline = googleMap.addPolyline(polylineOptions);
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
                        float speed = (float) data.optDouble("speed", 0.0);
                        runOnUiThread(() -> {
                            updateBusLocation(new LatLng(lat, lng));
                            if (tvSpeed != null) tvSpeed.setText(Math.round(speed * 3.6) + " km/h");
                        });
                    }
                } catch (JSONException e) { Log.e("Socket", "Error parsing location", e); }
            });

            mSocket.on("trip:started", args -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    String busId = data.getString("busId");
                    runOnUiThread(() -> {
                        if (busId.equals(selectedBusId)) updateStatusTag("active");
                        loadAllBuses(); // Refresh all to see online status
                        fetchAssignedBus(); // Refresh assigned
                    });
                } catch (JSONException e) { e.printStackTrace(); }
            });

            mSocket.on("trip:ended", args -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    String busId = data.getString("busId");
                    runOnUiThread(() -> {
                        if (busId.equals(selectedBusId)) updateStatusTag("inactive");
                        loadAllBuses();
                        fetchAssignedBus();
                    });
                } catch (JSONException e) { e.printStackTrace(); }
            });

            mSocket.on("bus:delay", args -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    String busId = data.getString("busId");
                    String delay = data.getString("delay"); // e.g. "5 mins"
                    runOnUiThread(() -> {
                        if (busId.equals(selectedBusId)) {
                            if (tvETA != null) tvETA.setText("Delayed: " + delay);
                            Toast.makeText(this, "Bus " + busId + " is delayed by " + delay, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (JSONException e) { e.printStackTrace(); }
            });

            mSocket.on("assignment:updated", args -> {
                runOnUiThread(() -> {
                    fetchAssignedBus();
                    loadAllBuses();
                });
            });

            mSocket.on("bus:offline", args -> runOnUiThread(() -> updateStatusTag("inactive")));
            mSocket.connect();
        } catch (URISyntaxException e) { Log.e("Socket", "URI Syntax Error", e); }
    }

    private void joinBusRoom() {
        try {
            JSONObject data = new JSONObject();
            data.put("busId", selectedBusId);
            mSocket.emit("join:bus", data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void updateBusLocation(LatLng point) {
        if (googleMap == null || point == null || (point.latitude == 0 && point.longitude == 0)) return;
        if (busMarker == null) {
            busMarker = googleMap.addMarker(new MarkerOptions().position(point).title("Live Bus")
                    .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_directions)));
        } else { busMarker.setPosition(point); }
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(point));
        updateStatusTag("active");
    }

    private void updateStatusTag(String status) {
        boolean isActive = "active".equalsIgnoreCase(status);
        tvStatusTag.setText(isActive ? "LIVE" : "OFFLINE");
        tvFloatingStatus.setText(isActive ? "🟢 LIVE" : "⚪ OFFLINE");
        tvFloatingStatus.setTextColor(isActive ? ContextCompat.getColor(this, R.color.success) : Color.GRAY);
        MaterialCardView tagCard = findViewById(R.id.tagCard);
        if (tagCard != null) tagCard.setCardBackgroundColor(isActive ? ContextCompat.getColor(this, R.color.primary_light) : Color.LTGRAY);
    }

    private void logout() {
        sessionManager.logout();
        finish();
    }

    @Override public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else { super.onBackPressed(); }
    }

    @Override protected void onResume() { super.onResume(); map.onResume(); }
    @Override protected void onPause() { super.onPause(); map.onPause(); }
    @Override protected void onStart() { super.onStart(); map.onStart(); }
    @Override protected void onStop() { super.onStop(); map.onStop(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        map.onDestroy();
        if (mSocket != null) { mSocket.disconnect(); mSocket.off(); }
    }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) { super.onSaveInstanceState(outState); map.onSaveInstanceState(outState); }
    @Override public void onLowMemory() { super.onLowMemory(); map.onLowMemory(); }
}
