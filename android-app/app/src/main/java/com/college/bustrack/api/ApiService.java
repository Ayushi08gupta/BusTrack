package com.college.bustrack.api;

import com.college.bustrack.models.Bus;
import com.college.bustrack.models.Complaint;
import com.college.bustrack.models.GenericResponse;
import com.college.bustrack.models.LocationUpdateRequest;
import com.college.bustrack.models.LoginRequest;
import com.college.bustrack.models.LoginResponse;
import com.college.bustrack.models.RegisterRequest;
import com.college.bustrack.models.Route;
import com.college.bustrack.models.User;
import com.college.bustrack.models.Trip;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // --- Authentication ---
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<GenericResponse> register(@Body RegisterRequest request);

    @POST("api/auth/forgot-password")
    Call<GenericResponse> forgotPassword(@Body Map<String, String> body);

    @POST("api/auth/change-password")
    Call<GenericResponse> changePassword(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );

    // --- Admin User Management ---
    @GET("api/admin/users")
    Call<List<User>> adminGetUsers(@Header("Authorization") String token);

    @POST("api/admin/users")
    Call<GenericResponse> adminAddUser(@Header("Authorization") String token, @Body Map<String, Object> userData);

    @POST("api/admin/users/bulk")
    Call<GenericResponse> adminBulkAddUsers(@Header("Authorization") String token, @Body List<Map<String, Object>> usersData);

    @PATCH("api/admin/users/{id}/status")
    Call<GenericResponse> adminToggleUserStatus(@Header("Authorization") String token, @Path("id") String id, @Body Map<String, Object> status);

    @DELETE("api/admin/users/{id}")
    Call<GenericResponse> adminDeleteUser(@Header("Authorization") String token, @Path("id") String id);

    // --- Admin Bus & Route Management ---
    @GET("api/admin/buses")
    Call<List<Bus>> adminGetBuses(@Header("Authorization") String token);

    @POST("api/admin/buses")
    Call<GenericResponse> adminAddBus(@Header("Authorization") String token, @Body Map<String, Object> busData);

    @DELETE("api/admin/buses/{id}")
    Call<GenericResponse> adminDeleteBus(@Header("Authorization") String token, @Path("id") String id);

    @POST("api/admin/assign")
    Call<GenericResponse> adminAssign(@Header("Authorization") String token, @Body com.college.bustrack.models.AssignRequest body);

    @POST("api/admin/bus/{busId}/stops")
    Call<GenericResponse> adminAddStopToBus(@Header("Authorization") String token, @Path("busId") String busId, @Body Map<String, Object> stopData);

    @POST("api/admin/full-assignment")
    Call<GenericResponse> adminFullAssignment(@Header("Authorization") String token, @Body Map<String, Object> assignmentData);

    // --- Student Section ---
    @GET("api/student/bus-info")
    Call<Bus> getStudentBusInfo(@Header("Authorization") String token);

    @GET("api/student/search-buses")
    Call<List<Bus>> searchBuses(@Header("Authorization") String token, @Query("query") String query);

    @GET("api/bus/{id}")
    Call<Bus> getBusFullDetails(@Header("Authorization") String token, @Path("id") String busId);

    // --- Driver Section ---
    @GET("api/driver/assigned-bus")
    Call<Bus> getDriverAssignedBus(@Header("Authorization") String token);

    @POST("api/driver/location-update")
    Call<Bus> updateLocation(@Header("Authorization") String token, @Body LocationUpdateRequest request);

    @POST("api/driver/start-journey")
    Call<Trip> startJourney(@Header("Authorization") String token, @Body Map<String, String> body);

    @POST("api/driver/stop-journey")
    Call<GenericResponse> stopJourney(@Header("Authorization") String token, @Body Map<String, String> body);

    // --- Admin Complaint Management ---
    @GET("api/admin/complaints")
    Call<List<Complaint>> adminGetComplaints(@Header("Authorization") String token, @Query("status") String status);

    @PATCH("api/admin/complaints/{id}")
    Call<GenericResponse> adminUpdateComplaint(@Header("Authorization") String token, @Path("id") String id, @Body Map<String, Object> updateData);

    @POST("api/issues/report")
    Call<GenericResponse> submitComplaint(
            @Header("Authorization") String token,
            @Body Map<String, Object> data
    );

    @GET("api/admin/buses/active")
    Call<List<Bus>> getActiveBuses(@Header("Authorization") String token);
}
