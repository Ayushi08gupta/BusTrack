package com.college.bustrack.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.bustrack.R;
import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.Complaint;
import com.college.bustrack.models.GenericResponse;
import com.college.bustrack.utils.SessionManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComplaintsFragment extends Fragment {

    private RecyclerView rvComplaints;
    private ComplaintAdapter adapter;
    private ChipGroup chipGroupStatus;
    private ProgressBar progressBar;
    private List<Complaint> complaintsList = new ArrayList<>();
    
    private ApiService apiService;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_complaints, container, false);

        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(requireContext());

        rvComplaints = view.findViewById(R.id.rvComplaints);
        chipGroupStatus = view.findViewById(R.id.chipGroupStatus);
        progressBar = view.findViewById(R.id.progressBar);

        rvComplaints.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ComplaintAdapter(complaintsList, this::showActionDialog);
        rvComplaints.setAdapter(adapter);

        setupFilters();
        loadComplaints("pending");

        return view;
    }

    private void setupFilters() {
        chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipPending) loadComplaints("pending");
            else if (checkedId == R.id.chipInProgress) loadComplaints("in-progress");
            else if (checkedId == R.id.chipResolved) loadComplaints("resolved");
        });
    }

    private void loadComplaints(String status) {
        showLoading(true);
        apiService.adminGetComplaints(sessionManager.getToken(), status).enqueue(new Callback<List<Complaint>>() {
            @Override
            public void onResponse(Call<List<Complaint>> call, Response<List<Complaint>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    complaintsList.clear();
                    complaintsList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<Complaint>> call, Throwable t) {
                showLoading(false);
            }
        });
    }

    private void showActionDialog(Complaint complaint) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_complaint_action, null);
        TextInputEditText etStaff = dialogView.findViewById(R.id.etAssignedStaff);
        TextInputEditText etRemarks = dialogView.findViewById(R.id.etRemarks);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Resolve Issue")
                .setView(dialogView)
                .setPositiveButton("Mark Resolved", (dialog, which) -> {
                    updateStatus(complaint, "resolved", etStaff.getText().toString(), etRemarks.getText().toString());
                })
                .setNeutralButton("In Progress", (dialog, which) -> {
                    updateStatus(complaint, "in-progress", etStaff.getText().toString(), etRemarks.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateStatus(Complaint complaint, String status, String staff, String remarks) {
        showLoading(true);
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", status);
        updateData.put("assignedStaff", staff);
        updateData.put("remarks", remarks);

        apiService.adminUpdateComplaint(sessionManager.getToken(), complaint.getId(), updateData).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Complaint " + status, Toast.LENGTH_SHORT).show();
                    loadComplaints("pending"); // Refresh list
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                showLoading(false);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
