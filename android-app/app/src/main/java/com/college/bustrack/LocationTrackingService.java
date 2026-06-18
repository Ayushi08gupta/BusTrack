package com.college.bustrack;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.college.bustrack.api.ApiClient;
import com.college.bustrack.api.ApiService;
import com.college.bustrack.models.Bus;
import com.college.bustrack.models.LocationUpdateRequest;
import com.college.bustrack.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationTrackingService extends Service {

    private static final String TAG = "LocationTrackingService";
    private static final String CHANNEL_ID = "LocationTrackingChannel";
    private static final int NOTIFICATION_ID = 101;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Socket mSocket;
    
    private SessionManager sessionManager;
    private ApiService apiService;
    private String busId;
    private String driverId;
    private String token;
    private String tripId;

    @Override
    public void onCreate() {
        super.onCreate();
        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();
        busId = sessionManager.getAssignedBusId();
        driverId = sessionManager.getUserId();
        token = sessionManager.getToken();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            mSocket = IO.socket(ApiClient.getBaseUrl());
            mSocket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket connection error", e);
        }

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            tripId = intent.getStringExtra("trip_id");
        }

        Notification notification = getNotification("Live location tracking is active.");
        
        // CRITICAL FIX FOR ANDROID 14 (API 34)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        
        startLocationUpdates();
        emitTripStarted();
        return START_STICKY;
    }

    private void emitTripStarted() {
        if (mSocket != null && busId != null) {
            try {
                JSONObject data = new JSONObject();
                data.put("busId", busId);
                data.put("driverId", driverId);
                data.put("tripId", tripId);
                mSocket.emit("trip:started", data);
            } catch (JSONException e) {
                Log.e(TAG, "JSON error", e);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        // Initial location grab to sync map immediately
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                emitLocationSocket(location.getLatitude(), location.getLongitude(), location.getSpeed(), location.getBearing());
                updateLocationRest(location.getLatitude(), location.getLongitude(), location.getSpeed(), location.getBearing());
            }
        });

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    float speed = location.getSpeed(); // m/s
                    float heading = location.getBearing(); // degrees

                    emitLocationSocket(location.getLatitude(), location.getLongitude(), speed, heading);
                    updateLocationRest(location.getLatitude(), location.getLongitude(), speed, heading);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void emitLocationSocket(double lat, double lng, float speed, float heading) {
        if (mSocket != null && mSocket.connected() && busId != null) {
            try {
                JSONObject data = new JSONObject();
                data.put("busId", busId);
                data.put("driverId", driverId);
                data.put("tripId", tripId);
                data.put("latitude", lat);
                data.put("longitude", lng);
                data.put("speed", speed);
                data.put("heading", heading);
                data.put("status", "active");
                mSocket.emit("location:update", data);
            } catch (JSONException e) {
                Log.e(TAG, "JSON error", e);
            }
        }
    }

    private void updateLocationRest(double lat, double lng, float speed, float heading) {
        if (token != null) {
            LocationUpdateRequest request = new LocationUpdateRequest(busId, driverId, tripId, lat, lng, speed, heading, "active");
            apiService.updateLocation(token, request).enqueue(new Callback<Bus>() {
                @Override
                public void onResponse(Call<Bus> call, Response<Bus> response) {
                    if (response.isSuccessful()) Log.d(TAG, "Location saved");
                }
                @Override
                public void onFailure(Call<Bus> call, Throwable t) {
                    Log.e(TAG, "REST update failed", t);
                }
            });
        }
    }

    private Notification getNotification(String text) {
        Intent notificationIntent = new Intent(this, DriverDashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BusTrack Driver")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Location Tracking", NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        emitTripEnded();
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (mSocket != null) mSocket.disconnect();
    }

    private void emitTripEnded() {
        if (mSocket != null && busId != null) {
            try {
                JSONObject data = new JSONObject();
                data.put("busId", busId);
                data.put("tripId", tripId);
                mSocket.emit("trip:ended", data);
            } catch (JSONException e) {
                Log.e(TAG, "JSON error", e);
            }
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
