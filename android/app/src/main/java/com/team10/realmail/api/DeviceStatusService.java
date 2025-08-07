package com.team10.realmail.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.List;

public class DeviceStatusService {

    private static final String TAG = "DeviceStatusService";
    private static final String PREFS_NAME = "device_status_prefs";
    private static final String PREF_MONITORING_ENABLED = "monitoring_enabled";
    private static final String PREF_USER_EMAIL = "user_email";

    private static DeviceStatusService instance;
    private DeviceRepository deviceRepository;
    private Context context;
    private SharedPreferences preferences;

    private DeviceStatusService(Context context) {
        this.context = context.getApplicationContext();
        this.deviceRepository = DeviceRepository.getInstance();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized DeviceStatusService getInstance(Context context) {
        if (instance == null) {
            instance = new DeviceStatusService(context);
        }
        return instance;
    }

    /**
     * Start device status monitoring for a user
     */
    public void startMonitoring(String userEmail) {
        Log.d(TAG, "Starting device status monitoring for: " + userEmail);

        // Save preferences
        preferences.edit()
                .putBoolean(PREF_MONITORING_ENABLED, true)
                .putString(PREF_USER_EMAIL, userEmail)
                .apply();

        // Start monitoring in repository
        deviceRepository.startDeviceStatusMonitoring(userEmail);
    }

    /**
     * Stop device status monitoring
     */
    public void stopMonitoring() {
        Log.d(TAG, "Stopping device status monitoring");

        // Clear preferences
        preferences.edit()
                .putBoolean(PREF_MONITORING_ENABLED, false)
                .remove(PREF_USER_EMAIL)
                .apply();

        // Stop monitoring in repository
        deviceRepository.stopDeviceStatusMonitoring();
    }

    /**
     * Resume monitoring if it was previously enabled (for app restart scenarios)
     */
    public void resumeMonitoringIfEnabled() {
        boolean wasEnabled = preferences.getBoolean(PREF_MONITORING_ENABLED, false);
        String userEmail = preferences.getString(PREF_USER_EMAIL, null);

        if (wasEnabled && userEmail != null) {
            Log.d(TAG, "Resuming device status monitoring for: " + userEmail);
            deviceRepository.startDeviceStatusMonitoring(userEmail);
        }
    }

    /**
     * Manually trigger a device status check
     */
    public void checkDeviceStatusNow(String userEmail) {
        deviceRepository.checkDeviceStatus(userEmail);
    }

    /**
     * Get live data for device status updates
     */
    public LiveData<DeviceStatusResponse> getDeviceStatusLiveData() {
        return deviceRepository.getDeviceStatusLiveData();
    }

    /**
     * Get live data for devices list with status information
     */
    public LiveData<List<Device>> getDevicesLiveData() {
        return deviceRepository.getDevicesLiveData();
    }

    /**
     * Get live data for loading state
     */
    public LiveData<Boolean> getIsLoadingLiveData() {
        return deviceRepository.getIsLoadingLiveData();
    }

    /**
     * Get live data for toast messages
     */
    public LiveData<String> getToastMessageLiveData() {
        return deviceRepository.getToastMessageLiveData();
    }

    /**
     * Check if monitoring is currently enabled
     */
    public boolean isMonitoringEnabled() {
        return deviceRepository.isStatusMonitoringEnabled();
    }

    /**
     * Get a summary of device statuses
     */
    public String getStatusSummary() {
        return deviceRepository.getDeviceStatusSummary();
    }

    /**
     * Get device status classification based on last heartbeat timestamp
     */
    public static String getDeviceStatusFromTimestamp(long lastHeartbeatTimestamp) {
        long currentTime = System.currentTimeMillis();
        long timeDifferenceMinutes = (currentTime - lastHeartbeatTimestamp) / (1000 * 60);

        if (timeDifferenceMinutes <= 3) {
            return "online";
        } else if (timeDifferenceMinutes <= 5) {
            return "warning";
        } else {
            return "offline";
        }
    }

    /**
     * Get color resource ID for visual indicator
     */
    public static int getStatusColor(String status) {
        // Handle null values safely
        if (status == null) {
            return android.R.color.darker_gray;
        }

        // Handle both connection_status and visual_indicator
        switch (status) {
            case "online":
            case "green":
                return android.R.color.holo_green_light;
            case "warning":
            case "yellow":
                return android.R.color.holo_orange_light;
            case "offline":
            case "red":
                return android.R.color.holo_red_light;
            default:
                return android.R.color.darker_gray;
        }
    }

    /**
     * Get color resource ID for visual indicator from Device object
     */
    public static int getDeviceStatusColor(Device device) {
        if (device == null) {
            return android.R.color.darker_gray;
        }

        // Prefer visual_indicator if available and not null
        if (device.getVisualIndicator() != null && !device.getVisualIndicator().isEmpty()) {
            return getStatusColor(device.getVisualIndicator());
        }

        // Fallback to connection_status (with null check)
        String connectionStatus = device.getConnectionStatus();
        return getStatusColor(connectionStatus != null ? connectionStatus : "offline");
    }

    /**
     * Get status display text
     */
    public static String getStatusDisplayText(String status) {
        // Handle null values safely
        if (status == null) {
            return "Unknown";
        }

        switch (status) {
            case "online":
                return "Online";
            case "warning":
                return "Warning";
            case "offline":
                return "Offline";
            default:
                return "Unknown";
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        deviceRepository.cleanup();
    }
}
