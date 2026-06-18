package com.college.bustrack;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.GenericResponse;
import com.college.bustrack.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportIssueActivity extends AppCompatActivity {

    private AutoCompleteTextView menuIssueType;
    private TextInputEditText etDescription;
    private MaterialButton btnSubmit;
    
    private ApiService apiService;
    private SessionManager sessionManager;
    private String busId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);
        busId = getIntent().getStringExtra("BUS_ID");

        menuIssueType = findViewById(R.id.menuIssueType);
        etDescription = findViewById(R.id.etDescription);
        btnSubmit = findViewById(R.id.btnSubmitIssue);

        String[] types = {"Delay", "Behavior", "Maintenance", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, types);
        menuIssueType.setAdapter(adapter);

        btnSubmit.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        String type = menuIssueType.getText().toString();
        String description = etDescription.getText().toString().trim();

        if (type.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put("description", description);
        data.put("busId", busId);

        apiService.submitComplaint(sessionManager.getToken(), data).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ReportIssueActivity.this, "Issue reported successfully", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(ReportIssueActivity.this, "Failed to report issue", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(ReportIssueActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
