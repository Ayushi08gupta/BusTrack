package com.college.bustrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.ChangePasswordRequest;
import com.college.bustrack.models.GenericResponse;
import com.college.bustrack.models.LoginRequest;
import com.college.bustrack.models.LoginResponse;
import com.college.bustrack.models.User;
import com.college.bustrack.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsernameOrEmail, etPassword;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private LinearLayout loginFieldsContainer, changePasswordContainer;
    private MaterialButton btnLogin, btnSubmitNewPassword;
    private TextView tvRegisterLink;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private ApiService apiService;
    private String tempToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();

        if (sessionManager.isLoggedIn() && !sessionManager.isFirstLogin()) {
            navigateBasedOnRole(sessionManager.getUserRole());
            return;
        }

        etUsernameOrEmail = findViewById(R.id.etUsernameOrEmail);
        etPassword = findViewById(R.id.etPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        loginFieldsContainer = findViewById(R.id.loginFieldsContainer);
        changePasswordContainer = findViewById(R.id.changePasswordContainer);
        btnLogin = findViewById(R.id.btnLogin);
        btnSubmitNewPassword = findViewById(R.id.btnSubmitNewPassword);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> performLogin());
        btnSubmitNewPassword.setOnClickListener(v -> performPasswordChange());
        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void performLogin() {
        Editable userEdit = etUsernameOrEmail.getText();
        Editable passEdit = etPassword.getText();

        if (userEdit == null || passEdit == null) return;

        String usernameOrEmail = userEdit.toString().trim();
        String password = passEdit.toString().trim();

        if (usernameOrEmail.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        LoginRequest request = new LoginRequest(usernameOrEmail, password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    User user = loginResponse.getUser();
                    String token = "Bearer " + loginResponse.getToken();

                    sessionManager.saveSession(
                            token, user.getId(), user.getName(), user.getEmail(),
                            user.getRole(), user.isFirstLogin(), user.getAssignedBusId()
                    );

                    if (user.isFirstLogin()) {
                        tempToken = token;
                        loginFieldsContainer.setVisibility(View.GONE);
                        changePasswordContainer.setVisibility(View.VISIBLE);
                    } else {
                        navigateBasedOnRole(user.getRole());
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials or login failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performPasswordChange() {
        Editable newPassEdit = etNewPassword.getText();
        Editable confirmPassEdit = etConfirmPassword.getText();

        if (newPassEdit == null || confirmPassEdit == null) return;

        String newPassword = newPassEdit.toString().trim();
        String confirmPassword = confirmPassEdit.toString().trim();

        if (newPassword.isEmpty() || confirmPassword.isEmpty() || !newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Check your passwords", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        apiService.changePassword(tempToken, java.util.Collections.singletonMap("newPassword", newPassword)).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    sessionManager.setFirstLogin(false);
                    navigateBasedOnRole(sessionManager.getUserRole());
                }
            }
            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                showLoading(false);
            }
        });
    }

    private void navigateBasedOnRole(String role) {
        if (role == null) return;
        Intent intent;
        if (role.equalsIgnoreCase("admin")) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else if (role.equalsIgnoreCase("driver")) {
            intent = new Intent(this, DriverDashboardActivity.class);
        } else {
            intent = new Intent(this, StudentDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
    }
}
