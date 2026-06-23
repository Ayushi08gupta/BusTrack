package com.college.bustrack.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.bustrack.BusAssignmentActivity;
import com.college.bustrack.R;
import com.college.bustrack.RouteManagementActivity;
import com.college.bustrack.adapter.AssignmentAdapter;
import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.GenericResponse;
import com.college.bustrack.models.Bus;
import com.college.bustrack.utils.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoutesFragment extends Fragment {

    private RecyclerView rvAssignments;
    private AssignmentAdapter adapter;
    private ApiService apiService;
    private SessionManager sessionManager;
    private List<Bus> busList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routes, container, false);

        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(requireContext());

        rvAssignments = view.findViewById(R.id.rvAssignments);
        ExtendedFloatingActionButton fabNewAssignment = view.findViewById(R.id.fabNewAssignment);
        FloatingActionButton fabRouteDesigner = view.findViewById(R.id.fabRouteDesigner);

        rvAssignments.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AssignmentAdapter(busList);
        adapter.setOnAssignmentActionListener(this::showDeleteConfirmation);
        rvAssignments.setAdapter(adapter);

        fabRouteDesigner.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), RouteManagementActivity.class));
        });

        // Launches the professional Map-based assignment screen
        fabNewAssignment.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), BusAssignmentActivity.class));
        });

        return view;
    }

    private void showDeleteConfirmation(Bus bus) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Assignment")
                .setMessage("Are you sure you want to remove this assignment? This will unassign Driver: " + 
                        (bus.getDriverId() != null ? bus.getDriverId().getName() : "Unknown") + 
                        " and the route from Bus " + bus.getBusNumber() + ".")
                .setPositiveButton("Delete", (dialog, which) -> deleteAssignment(bus))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAssignment(Bus bus) {
        if (bus == null || (bus.getId() == null && bus.getBusNumber() == null)) {
            Toast.makeText(getContext(), "Error: Invalid Bus Data", Toast.LENGTH_SHORT).show();
            return;
        }

        String identifier = bus.getId() != null ? bus.getId() : bus.getBusNumber();
        android.util.Log.d("RoutesFragment", "Deleting assignment for: " + identifier);

        apiService.adminDeleteAssignment(sessionManager.getToken(), identifier).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Assignment removed successfully", Toast.LENGTH_SHORT).show();
                    loadActiveBuses();
                } else {
                    String errorMsg = "Delete failed (" + response.code() + ")";
                    if (response.errorBody() != null) {
                        try {
                            String serverMsg = response.errorBody().string();
                            if (serverMsg.contains("message")) {
                                // Try to extract just the message if it's JSON
                                errorMsg += ": " + serverMsg;
                            } else {
                                errorMsg += ": " + serverMsg;
                            }
                        } catch (Exception e) {
                            errorMsg += " (Error reading details)";
                        }
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadActiveBuses();
    }

    private void loadActiveBuses() {
        apiService.getActiveBuses(sessionManager.getToken()).enqueue(new Callback<List<Bus>>() {
            @Override
            public void onResponse(Call<List<Bus>> call, Response<List<Bus>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    busList.clear();
                    busList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Bus>> call, Throwable t) {}
        });
    }
}
