package com.college.bustrack.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.college.bustrack.R;
import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.Bus;
import com.college.bustrack.utils.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FleetMapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ApiService apiService;
    private SessionManager sessionManager;
    private Map<String, Marker> busMarkers = new HashMap<>();
    private CardView loadingCard;
    private TextView tvMapStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fleet_map, container, false);

        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(requireContext());

        loadingCard = view.findViewById(R.id.loadingCard);
        tvMapStatus = view.findViewById(R.id.tvMapStatus);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.fleetMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        
        // Center on a default location (e.g., Bhopal)
        LatLng defaultPos = new LatLng(23.2599, 77.4126);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 12f));

        mMap.setOnMarkerClickListener(marker -> {
            Bus bus = (Bus) marker.getTag();
            if (bus != null) {
                String info = "Bus: " + bus.getBusNumber() + 
                             "\nDriver: " + (bus.getDriverId() != null ? bus.getDriverId().getName() : "Unassigned") +
                             "\nRoute: " + (bus.getRouteId() != null ? bus.getRouteId().getRouteName() : "N/A");
                Toast.makeText(getContext(), info, Toast.LENGTH_LONG).show();
            }
            return false;
        });

        fetchActiveBuses();
    }

    private void fetchActiveBuses() {
        if (loadingCard != null) loadingCard.setVisibility(View.VISIBLE);
        
        apiService.getActiveBuses(sessionManager.getToken()).enqueue(new Callback<List<Bus>>() {
            @Override
            public void onResponse(Call<List<Bus>> call, Response<List<Bus>> response) {
                if (loadingCard != null) loadingCard.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    updateBusMarkers(response.body());
                } else {
                    if (tvMapStatus != null) tvMapStatus.setText("No active buses found.");
                }
            }

            @Override
            public void onFailure(Call<List<Bus>> call, Throwable t) {
                if (loadingCard != null) loadingCard.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load live fleet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBusMarkers(List<Bus> buses) {
        if (mMap == null) return;

        for (Bus bus : buses) {
            if (bus.getCurrentLocation() != null) {
                LatLng pos = new LatLng(bus.getCurrentLocation().getLatitude(), bus.getCurrentLocation().getLongitude());
                
                Marker marker = busMarkers.get(bus.getId());
                if (marker == null) {
                    marker = mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title("Bus " + bus.getBusNumber())
                            .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_directions)));
                    busMarkers.put(bus.getId(), marker);
                } else {
                    marker.setPosition(pos);
                }
                marker.setTag(bus);
            }
        }
        
        if (buses.isEmpty() && tvMapStatus != null) {
            tvMapStatus.setText("No buses are currently on a trip.");
        }
    }
}
