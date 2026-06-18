package com.college.bustrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.bustrack.adapter.SelectedStopAdapter;
import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.AssignRequest;
import com.college.bustrack.models.Bus;
import com.college.bustrack.models.GenericResponse;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RouteManagementActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "RouteManagement";
    private GoogleMap mMap;
    private final List<Stop> selectedStops = new ArrayList<>();
    private final List<Marker> stopMarkers = new ArrayList<>();
    private Polyline routePolyline;

    private SelectedStopAdapter adapter;
    private AutoCompleteTextView menuSelectBus, menuSelectDriver;
    private MaterialButton btnSaveRoute;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SessionManager sessionManager;
    
    private List<Bus> busList = new ArrayList<>();
    private List<User> driverList = new ArrayList<>();
    private String selectedBusId, selectedDriverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_management);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        }

        initViews();
        setupMap();
        setupAutocomplete();
        fetchBuses();
        fetchDrivers();
    }

    private void initViews() {
        RecyclerView rvSelectedStops = findViewById(R.id.rvSelectedStops);
        menuSelectBus = findViewById(R.id.menuSelectBus);
        menuSelectDriver = findViewById(R.id.menuSelectDriver);
        btnSaveRoute = findViewById(R.id.btnSaveRoute);
        progressBar = findViewById(R.id.progressBar);

        if (rvSelectedStops != null) {
            rvSelectedStops.setLayoutManager(new LinearLayoutManager(this));
            adapter = new SelectedStopAdapter(selectedStops, this::removeStop);
            rvSelectedStops.setAdapter(adapter);
        }

        btnSaveRoute.setOnClickListener(v -> validateAndSave());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupAutocomplete() {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            autocompleteFragment.setHint("Search for stops");
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    if (place.getLatLng() != null) {
                        addStopAtLocation(place.getLatLng(), place.getName());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15f));
                    }
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.e(TAG, "Places error: " + status);
                }
            });
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng defaultLocation = new LatLng(23.2599, 77.4126);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

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
            addStopAtLocation(latLng, stopName);
        } catch (IOException e) {
            addStopAtLocation(latLng, "Stop " + (selectedStops.size() + 1));
        }
    }

    private void addStopAtLocation(LatLng latLng, String name) {
        Stop stop = new Stop();
        stop.setName(name);
        stop.setLatitude(latLng.latitude);
        stop.setLongitude(latLng.longitude);
        selectedStops.add(stop);
        
        if (adapter != null) {
            adapter.notifyItemInserted(selectedStops.size() - 1);
        }
        updateMapElements();
    }

    private void removeStop(int position) {
        if (position >= 0 && position < selectedStops.size()) {
            selectedStops.remove(position);
            if (adapter != null) adapter.notifyDataSetChanged();
            updateMapElements();
        }
    }

    private void updateMapElements() {
        if (mMap == null) return;

        for (Marker m : stopMarkers) m.remove();
        stopMarkers.clear();
        if (routePolyline != null) routePolyline.remove();

        PolylineOptions polyOptions = new PolylineOptions().color(Color.BLUE).width(10f);

        for (int i = 0; i < selectedStops.size(); i++) {
            Stop stop = selectedStops.get(i);
            LatLng pos = new LatLng(stop.getLatitude(), stop.getLongitude());
            polyOptions.add(pos);

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title("Stop " + (i + 1) + ": " + stop.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            if (marker != null) stopMarkers.add(marker);
        }

        if (selectedStops.size() >= 2) routePolyline = mMap.addPolyline(polyOptions);
    }

    private void fetchBuses() {
        apiService.adminGetBuses(sessionManager.getToken()).enqueue(new Callback<List<Bus>>() {
            @Override
            public void onResponse(@NonNull Call<List<Bus>> call, @NonNull Response<List<Bus>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    busList = response.body();
                    String[] busNumbers = new String[busList.size()];
                    for (int i = 0; i < busList.size(); i++) busNumbers[i] = "Bus " + busList.get(i).getBusNumber();
                    ArrayAdapter<String> busAdapter = new ArrayAdapter<>(RouteManagementActivity.this, android.R.layout.simple_list_item_1, busNumbers);
                    menuSelectBus.setAdapter(busAdapter);
                    menuSelectBus.setOnItemClickListener((p, v, pos, id) -> selectedBusId = busList.get(pos).getId());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Bus>> call, @NonNull Throwable t) {
                Toast.makeText(RouteManagementActivity.this, "Error fetching buses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchDrivers() {
        apiService.adminGetUsers(sessionManager.getToken()).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    driverList.clear();
                    List<String> names = new ArrayList<>();
                    for (User user : response.body()) {
                        if ("driver".equalsIgnoreCase(user.getRole())) {
                            driverList.add(user);
                            names.add(user.getName());
                        }
                    }
                    ArrayAdapter<String> da = new ArrayAdapter<>(RouteManagementActivity.this, android.R.layout.simple_list_item_1, names);
                    menuSelectDriver.setAdapter(da);
                    menuSelectDriver.setOnItemClickListener((p, v, pos, id) -> selectedDriverId = driverList.get(pos).getId());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {}
        });
    }

    private void validateAndSave() {
        if (selectedBusId == null || selectedDriverId == null) {
            Toast.makeText(this, "Please select both Bus and Driver", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedStops.size() < 2) {
            Toast.makeText(this, "Please add at least 2 stops", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        AssignRequest request = new AssignRequest(selectedDriverId, selectedBusId, null);
        apiService.adminAssign(sessionManager.getToken(), request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    saveStopsSequentially(0);
                } else {
                    showLoading(false);
                    handleError(response);
                }
            }
            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(RouteManagementActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveStopsSequentially(int index) {
        if (index >= selectedStops.size()) {
            showLoading(false);
            Toast.makeText(this, "Route saved successfully!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Stop stop = selectedStops.get(index);
        Map<String, Object> stopData = new HashMap<>();
        stopData.put("name", stop.getName());
        stopData.put("latitude", stop.getLatitude());
        stopData.put("longitude", stop.getLongitude());
        stopData.put("order", index + 1);

        apiService.adminAddStopToBus(sessionManager.getToken(), selectedBusId, stopData).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                if (response.isSuccessful()) saveStopsSequentially(index + 1);
                else {
                    showLoading(false);
                    handleError(response);
                }
            }
            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(RouteManagementActivity.this, "Failed saving stop " + (index + 1), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleError(Response<?> response) {
        try {
            JSONObject jObjError = new JSONObject(response.errorBody().string());
            Toast.makeText(this, jObjError.getString("message"), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Operation failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveRoute.setEnabled(!isLoading);
    }
}
