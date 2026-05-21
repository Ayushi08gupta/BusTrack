package com.college.bustrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.GenericResponse;
import com.college.bustrack.models.LoginRequest;
import com.college.bustrack.models.LoginResponse;
import com.college.bustrack.models.User;
import com.college.bustrack.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnLogin, btnSubmitNewPassword;
    private TextView tvForgotPassword;
    private ProgressBar progressBar;
    private LinearLayout loginFieldsContainer, changePasswordContainer;
    
    private SessionManager sessionManager;
    private ApiService apiService;
    private String tempToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();

        if (sessionManager.isLoggedIn()) {
            navigateBasedOnRole(sessionManager.getUserRole());
            return;
        }

        // Login Views
        etEmail = findViewById(R.id.etUsernameOrEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvRegisterLink); 
        progressBar = findViewById(R.id.progressBar);
        loginFieldsContainer = findViewById(R.id.loginFieldsContainer);
        
        // Change Password Views
        changePasswordContainer = findViewById(R.id.changePasswordContainer);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmitNewPassword = findViewById(R.id.btnSubmitNewPassword);

        if (tvForgotPassword != null) {
            tvForgotPassword.setText("Forgot Password?");
            tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        }
        
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> performLogin());
        }

        if (btnSubmitNewPassword != null) {
            btnSubmitNewPassword.setOnClickListener(v -> updatePassword());
        }
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        LoginRequest loginRequest = new LoginRequest(email, password);
        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    User user = loginResponse.getUser();
                    if (user == null) return;

                    tempToken = "Bearer " + loginResponse.getToken();

                    if (user.isFirstLogin()) {
                        showChangePasswordUI();
                    } else {
                        saveSessionAndNavigate(loginResponse);
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showChangePasswordUI() {
        if (loginFieldsContainer != null) loginFieldsContainer.setVisibility(View.GONE);
        if (changePasswordContainer != null) changePasswordContainer.setVisibility(View.VISIBLE);
        Toast.makeText(this, "First login: Please set a new password", Toast.LENGTH_LONG).show();
    }

    private void updatePassword() {
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (newPass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        Map<String, String> body = new HashMap<>();
        body.put("newPassword", newPass);

        apiService.changePassword(tempToken, body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Password updated! Please login again.", Toast.LENGTH_LONG).show();
                    changePasswordContainer.setVisibility(View.GONE);
                    loginFieldsContainer.setVisibility(View.VISIBLE);
                    etPassword.setText("");
                } else {
                    Toast.makeText(LoginActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSessionAndNavigate(LoginResponse loginResponse) {
        User user = loginResponse.getUser();
        sessionManager.saveSession(
                "Bearer " + loginResponse.getToken(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isFirstLogin(),
                user.getAssignedBusId()
        );
        navigateBasedOnRole(user.getRole());
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Forgot Password");
        
        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        builder.setView(customLayout);
        
        builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
            TextInputEditText etResetEmail = customLayout.findViewById(R.id.etResetEmail);
            if (etResetEmail != null) {
                sendResetLink(etResetEmail.getText().toString().trim());
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void sendResetLink(String email) {
        if (email.isEmpty()) return;
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        apiService.forgotPassword(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Reset link sent to " + email, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error sending reset link", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateBasedOnRole(String role) {
        if (role == null) return;
        Intent intent;
        switch (role.toUpperCase()) {
            case "ADMIN":
                intent = new Intent(this, AdminDashboardActivity.class);
                break;
            case "DRIVER":
                intent = new Intent(this, DriverDashboardActivity.class);
                break;
            case "STUDENT":
            default:
                intent = new Intent(this, StudentDashboardActivity.class);
                break;
        }
        startActivity(intent);
        finish();
    }
}
