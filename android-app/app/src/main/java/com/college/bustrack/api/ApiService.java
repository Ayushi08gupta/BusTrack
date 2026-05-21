package com.college.bustrack.api;

import com.college.bustrack.models.Bus;
import com.college.bustrack.models.GenericResponse;
import com.college.bustrack.models.LocationUpdateRequest;
import com.college.bustrack.models.LoginRequest;
import com.college.bustrack.models.LoginResponse;
import com.college.bustrack.models.RegisterRequest;
import com.college.bustrack.models.Route;
import com.college.bustrack.models.User;

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

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<GenericResponse> register(@Body RegisterRequest request);

    @POST("api/auth/forgot-password")
    Call<GenericResponse> forgotPassword(@Body Map<String, String> email);

    @POST("api/auth/reset-password/{token}")
    Call<GenericResponse> resetPassword(@Path("token") String token, @Body Map<String, String> password);

    @POST("api/auth/change-password")
    Call<GenericResponse> changePassword(
            @Header("Authorization") String token,
            @Body Map<String, String> password
    );

    @GET("api/student/bus-info")
    Call<Bus> getStudentBusInfo(@Header("Authorization") String token);

    @GET("api/student/search-buses")
    Call<List<Bus>> searchBuses(
            @Header("Authorization") String token,
            @Query("query") String query
    );

    @GET("api/driver/assigned-bus")
    Call<Bus> getDriverAssignedBus(@Header("Authorization") String token);

    @POST("api/driver/location-update")
    Call<Bus> updateLocation(
            @Header("Authorization") String token,
            @Body LocationUpdateRequest request
    );

    @GET("api/routes")
    Call<List<Route>> getRoutes(@Header("Authorization") String token);

    // Admin User Management
    @GET("api/admin/users")
    Call<List<User>> adminGetUsers(@Header("Authorization") String token);

    @POST("api/admin/users")
    Call<GenericResponse> adminAddUser(@Header("Authorization") String token, @Body Map<String, Object> userData);

    @PUT("api/admin/users/{id}")
    Call<User> adminUpdateUser(@Header("Authorization") String token, @Path("id") String id, @Body Map<String, Object> userData);

    @PATCH("api/admin/users/{id}/status")
    Call<GenericResponse> adminToggleUserStatus(@Header("Authorization") String token, @Path("id") String id, @Body Map<String, Object> status);

    @DELETE("api/admin/users/{id}")
    Call<GenericResponse> adminDeleteUser(@Header("Authorization") String token, @Path("id") String id);

    // FIXED: Changed :routeId to {routeId}
    @POST("api/admin/routes/{routeId}/stops")
    Call<GenericResponse> adminAddStop(@Header("Authorization") String token, @Path("routeId") String routeId, @Body Map<String, Object> stopData);
}
