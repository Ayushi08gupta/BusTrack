package com.college.bustrack;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.bustrack.adapter.AssignmentStopAdapter;
import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.GenericResponse;
import com.college.bustrack.models.Bus;
import com.college.bustrack.models.Stop;
import com.college.bustrack.models.User;
import com.college.bustrack.utils.SessionManager;
import com.google.android.gms.common.api.Status;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BusAssignmentActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "BusAssignment";
    private static final int AUTOCOMPLETE_REQUEST_CODE = 501;
    private static final int PICK_EXCEL_REQUEST = 102;
    
    private GoogleMap mMap;
    private List<Stop> routeStops = new ArrayList<>();
    private List<Marker> stopMarkers = new ArrayList<>();
    private Polyline routePolyline;

    private EditText etBusNumber;
    private AutoCompleteTextView menuDriver;
    private RecyclerView rvStops;
    private AssignmentStopAdapter adapter;
    private MaterialButton btnSave, btnBulk;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SessionManager sessionManager;
    private List<User> allDrivers = new ArrayList<>();
    private List<Bus> availableBuses = new ArrayList<>();
    private User selectedDriver;
    private Bus selectedBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_assignment);

        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);

        try {
            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
            }
        } catch (Exception e) {
            Log.e(TAG, "Places initialization failed", e);
        }

        initViews();
        setupMap();
        loadDrivers();
        loadAvailableBuses();
    }

    private void initViews() {
        etBusNumber = findViewById(R.id.etBusNumber);
        menuDriver = findViewById(R.id.menuDriver);
        rvStops = findViewById(R.id.rvStops);
        btnSave = findViewById(R.id.btnSaveAssignment);
        btnBulk = findViewById(R.id.btnBulkUploadExcel);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etBusNumber.setFocusable(true);
        etBusNumber.setFocusableInTouchMode(true);
        etBusNumber.setOnClickListener(null); // Allow typing instead of just dialog
        etBusNumber.setOnLongClickListener(v -> {
            showBusSelectionDialog();
            return true;
        });

        rvStops.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssignmentStopAdapter(routeStops, new AssignmentStopAdapter.OnStopActionListener() {
            @Override
            public void onRemove(int position) {
                routeStops.remove(position);
                updateMapAndList();
            }

            @Override
            public void onSetTime(int position) {
                showTimePicker(position);
            }
        });
        rvStops.setAdapter(adapter);

        // Drag-to-Reorder Logic
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                Collections.swap(routeStops, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                updateMapAndList();
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        });
        itemTouchHelper.attachToRecyclerView(rvStops);

        findViewById(R.id.cvSearchTrigger).setOnClickListener(v -> startSearch());
        btnBulk.setOnClickListener(v -> openFilePicker());
        btnSave.setOnClickListener(v -> saveFullAssignment());

        menuDriver.setOnItemClickListener((parent, view, position, id) -> selectedDriver = allDrivers.get(position));
    }

    private void showBusSelectionDialog() {
        if (availableBuses.isEmpty()) {
            Toast.makeText(this, "No available buses found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] busNumbers = new String[availableBuses.size()];
        for (int i = 0; i < availableBuses.size(); i++) {
            busNumbers[i] = availableBuses.get(i).getBusNumber();
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Bus")
                .setItems(busNumbers, (dialog, which) -> {
                    selectedBus = availableBuses.get(which);
                    etBusNumber.setText(selectedBus.getBusNumber());
                })
                .show();
    }

    private void startSearch() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountry("IN").build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapPreview);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        LatLng center = new LatLng(23.2599, 77.4126); // Bhopal default
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 12f));
        mMap.setOnMapClickListener(this::getAddressAndAddStop);
    }

    private void getAddressAndAddStop(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            String stopName = "Unknown Location";
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                stopName = address.getLocality() != null ? address.getLocality() : address.getAddressLine(0);
            }
            addStopAtLocation(stopName, latLng);
        } catch (IOException e) {
            addStopAtLocation("Custom Stop " + (routeStops.size() + 1), latLng);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            addStopAtLocation(place.getName(), place.getLatLng());
        } else if (requestCode == PICK_EXCEL_REQUEST && resultCode == RESULT_OK && data != null) {
            parseExcelCsv(data.getData());
        }
    }

    private void addStopAtLocation(String name, LatLng latLng) {
        if (latLng == null) return;
        Stop stop = new Stop();
        stop.setName(name);
        stop.setLatitude(latLng.latitude);
        stop.setLongitude(latLng.longitude);
        stop.setEta("Set Time");
        routeStops.add(stop);
        updateMapAndList();
    }

    private void showTimePicker(int position) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, h, m) -> {
            String time = String.format(Locale.US, "%02d:%02d %s", (h > 12 ? h - 12 : (h == 0 ? 12 : h)), m, (h >= 12 ? "PM" : "AM"));
            routeStops.get(position).setEta(time);
            adapter.notifyItemChanged(position);
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
    }

    private void updateMapAndList() {
        adapter.notifyDataSetChanged();
        if (mMap == null) return;
        for (Marker m : stopMarkers) m.remove();
        stopMarkers.clear();
        if (routePolyline != null) routePolyline.remove();
        if (routeStops.isEmpty()) return;

        PolylineOptions poly = new PolylineOptions().color(Color.parseColor("#3F51B5")).width(12f).geodesic(true);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (int i = 0; i < routeStops.size(); i++) {
            Stop s = routeStops.get(i);
            LatLng pos = new LatLng(s.getLatitude(), s.getLongitude());
            poly.add(pos);
            builder.include(pos);
            stopMarkers.add(mMap.addMarker(new MarkerOptions().position(pos).title((i+1) + ". " + s.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));
        }

        if (routeStops.size() >= 2) {
            routePolyline = mMap.addPolyline(poly);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        startActivityForResult(intent, PICK_EXCEL_REQUEST);
    }

    private void parseExcelCsv(Uri uri) {
        showLoading(true);
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                List<Stop> newStops = new ArrayList<>();
                Geocoder geocoder = new Geocoder(this);
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) { firstLine = false; continue; }
                    String[] p = line.split(",");
                    if (p.length >= 4) {
                        Stop s = new Stop();
                        s.setName(p[3].trim());
                        s.setEta(p[5].trim());
                        try {
                            List<Address> addr = geocoder.getFromLocationName(s.getName() + ", Bhopal", 1);
                            if (addr != null && !addr.isEmpty()) {
                                s.setLatitude(addr.get(0).getLatitude());
                                s.setLongitude(addr.get(0).getLongitude());
                            } else { s.setLatitude(23.2599); s.setLongitude(77.4126); }
                        } catch (Exception e) { s.setLatitude(23.2599); s.setLongitude(77.4126); }
                        newStops.add(s);
                    }
                }
                runOnUiThread(() -> {
                    routeStops.clear();
                    routeStops.addAll(newStops);
                    updateMapAndList();
                    showLoading(false);
                    Toast.makeText(this, "Upload complete", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> { showLoading(false); Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show(); });
            }
        }).start();
    }

    private void loadDrivers() {
        apiService.getAvailableDrivers(sessionManager.getToken()).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allDrivers.clear();
                    allDrivers.addAll(response.body());
                    List<String> names = new ArrayList<>();
                    for (User u : allDrivers) names.add(u.getName());
                    menuDriver.setAdapter(new ArrayAdapter<>(BusAssignmentActivity.this, android.R.layout.simple_list_item_1, names));
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(BusAssignmentActivity.this, "Failed to load drivers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAvailableBuses() {
        apiService.getAvailableBuses(sessionManager.getToken()).enqueue(new Callback<List<Bus>>() {
            @Override
            public void onResponse(Call<List<Bus>> call, Response<List<Bus>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    availableBuses.clear();
                    availableBuses.addAll(response.body());
                }
            }
            @Override public void onFailure(Call<List<Bus>> call, Throwable t) {}
        });
    }

    private void saveFullAssignment() {
        String busNumberText = etBusNumber.getText().toString().trim();
        if (busNumberText.isEmpty() || selectedDriver == null || routeStops.isEmpty()) {
            Toast.makeText(this, "Enter Bus No, select Driver and add Stops", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Stop s : routeStops) {
            if ("Set Time".equals(s.getEta())) {
                Toast.makeText(this, "Set arrival time for: " + s.getName(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        showLoading(true);
        Map<String, Object> data = new HashMap<>();
        data.put("busNumber", busNumberText);
        data.put("driverId", selectedDriver.getId());
        data.put("routeName", "Route " + busNumberText);
        data.put("stops", routeStops);

        apiService.adminFullAssignment(sessionManager.getToken(), data).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(BusAssignmentActivity.this, "Assignment Synced!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(BusAssignmentActivity.this, "Save failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(BusAssignmentActivity.this, "Server error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoading);
    }
}
