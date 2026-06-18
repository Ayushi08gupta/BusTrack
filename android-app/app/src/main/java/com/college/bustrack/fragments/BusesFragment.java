package com.college.bustrack.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.bustrack.R;
import com.college.bustrack.adapter.BusManagementAdapter;
import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.Bus;
import com.college.bustrack.models.GenericResponse;
import com.college.bustrack.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BusesFragment extends Fragment {

    private RecyclerView rvBuses;
    private BusManagementAdapter adapter;
    private List<Bus> busList = new ArrayList<>();
    private ApiService apiService;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_buses, container, false);

        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(requireContext());

        rvBuses = view.findViewById(R.id.rvBuses);
        FloatingActionButton fabAddBus = view.findViewById(R.id.fabAddBus);

        rvBuses.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BusManagementAdapter(busList, this::confirmDeleteBus);
        rvBuses.setAdapter(adapter);

        fabAddBus.setOnClickListener(v -> showAddBusDialog());

        loadBuses();

        return view;
    }

    private void loadBuses() {
        apiService.adminGetBuses(sessionManager.getToken()).enqueue(new Callback<List<Bus>>() {
            @Override
            public void onResponse(@NonNull Call<List<Bus>> call, @NonNull Response<List<Bus>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    busList.clear();
                    busList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Bus>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Failed to load buses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddBusDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_bus_simple, null);
        TextInputEditText etBusNumber = dialogView.findViewById(R.id.etBusNumber);
        TextInputEditText etVehicleNumber = dialogView.findViewById(R.id.etVehicleNumber);
        TextInputEditText etCapacity = dialogView.findViewById(R.id.etCapacity);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add New Bus")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String busNo = etBusNumber.getText().toString().trim();
                    String vehicleNo = etVehicleNumber.getText().toString().trim();
                    String capacityStr = etCapacity.getText().toString().trim();

                    if (busNo.isEmpty() || vehicleNo.isEmpty() || capacityStr.isEmpty()) {
                        Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int capacity = Integer.parseInt(capacityStr);
                        addBus(busNo, vehicleNo, capacity);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Invalid capacity", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addBus(String busNumber, String vehicleNumber, int capacity) {
        Map<String, Object> busData = new HashMap<>();
        busData.put("busNumber", busNumber);
        busData.put("vehicleNumber", vehicleNumber);
        busData.put("capacity", capacity);
        busData.put("status", "inactive");

        apiService.adminAddBus(sessionManager.getToken(), busData).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Bus added successfully", Toast.LENGTH_SHORT).show();
                    loadBuses();
                } else {
                    Toast.makeText(getContext(), "Error adding bus", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteBus(Bus bus) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Bus")
                .setMessage("Are you sure you want to delete Bus #" + bus.getBusNumber() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBus(bus.getId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBus(String busId) {
        apiService.adminDeleteBus(sessionManager.getToken(), busId).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Bus deleted", Toast.LENGTH_SHORT).show();
                    loadBuses();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {}
        });
    }
}
