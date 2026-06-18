package com.college.bustrack.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.bustrack.R;
import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.GenericResponse;
import com.college.bustrack.models.User;
import com.college.bustrack.utils.SessionManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsersFragment extends Fragment {

    private RecyclerView rvUsers;
    private UserManagementAdapter adapter;
    private List<User> allUsers = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    
    private ApiService apiService;
    private SessionManager sessionManager;
    private ChipGroup chipGroupFilter;
    private ImageButton btnDeleteSelected;
    
    private static final int PICK_FILE_REQUEST = 101;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(requireContext());

        rvUsers = view.findViewById(R.id.rvUsers);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected);
        
        FloatingActionButton fabAddUser = view.findViewById(R.id.fabAddUser);
        FloatingActionButton fabBulkUpload = view.findViewById(R.id.fabBulkUpload);

        setupRecyclerView();
        setupFilters();

        fabAddUser.setOnClickListener(v -> showAddUserDialog());
        fabBulkUpload.setOnClickListener(v -> openFilePicker());
        btnDeleteSelected.setOnClickListener(v -> confirmBulkDelete());

        loadUsers();

        return view;
    }

    private void setupRecyclerView() {
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserManagementAdapter(filteredUsers, new UserManagementAdapter.UserActionListener() {
            @Override
            public void onSelectionChanged() {
                updateDeleteButtonVisibility();
            }

            @Override
            public void onDelete(User user) {
                deleteSingleUser(user);
            }
        });
        rvUsers.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            applyFilter(checkedId);
        });
    }

    private void applyFilter(int checkedId) {
        filteredUsers.clear();
        if (checkedId == R.id.chipStudents) {
            for (User u : allUsers) if ("student".equalsIgnoreCase(u.getRole())) filteredUsers.add(u);
        } else if (checkedId == R.id.chipDrivers) {
            for (User u : allUsers) if ("driver".equalsIgnoreCase(u.getRole())) filteredUsers.add(u);
        } else {
            filteredUsers.addAll(allUsers);
        }
        adapter.notifyDataSetChanged();
        updateDeleteButtonVisibility();
    }

    private void updateDeleteButtonVisibility() {
        int count = 0;
        for (User u : filteredUsers) if (u.isSelected()) count++;
        btnDeleteSelected.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }

    private void loadUsers() {
        apiService.adminGetUsers(sessionManager.getToken()).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allUsers = response.body();
                    applyFilter(chipGroupFilter.getCheckedChipId());
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    private void showAddUserDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_user, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);
        AutoCompleteTextView menuRole = dialogView.findViewById(R.id.menuRole);

        String[] roles = {"Student", "Driver"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, roles);
        menuRole.setAdapter(roleAdapter);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create New User")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String role = menuRole.getText().toString().toLowerCase();
                    if (!name.isEmpty() && !email.isEmpty() && !role.isEmpty()) {
                        createUser(name, email, role);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createUser(String name, String email, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);

        apiService.adminAddUser(sessionManager.getToken(), userData).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "User created and credentials emailed.", Toast.LENGTH_LONG).show();
                    loadUsers();
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {}
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            processBulkUpload(data.getData());
        }
    }

    private void processBulkUpload(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            List<Map<String, Object>> bulkData = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(","); // Expected: name,email,role
                if (parts.length >= 3) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("name", parts[0].trim());
                    user.put("email", parts[1].trim());
                    user.put("role", parts[2].trim().toLowerCase());
                    bulkData.add(user);
                }
            }
            
            if (!bulkData.isEmpty()) {
                apiService.adminBulkAddUsers(sessionManager.getToken(), bulkData).enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Bulk upload successful!", Toast.LENGTH_LONG).show();
                            loadUsers();
                        }
                    }
                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {}
                });
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error reading file", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSingleUser(User user) {
        apiService.adminDeleteUser(sessionManager.getToken(), user.getId()).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) loadUsers();
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {}
        });
    }

    private void confirmBulkDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Bulk Delete")
                .setMessage("Delete selected users?")
                .setPositiveButton("Delete", (d, w) -> {
                    for (User u : filteredUsers) {
                        if (u.isSelected()) deleteSingleUser(u);
                    }
                    Toast.makeText(getContext(), "Processing bulk deletion...", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
