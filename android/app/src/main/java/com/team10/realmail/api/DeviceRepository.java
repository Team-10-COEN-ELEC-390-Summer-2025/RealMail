package com.team10.realmail.api;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DeviceRepository {

    private static final String TAG = "DeviceRepository";
    private static final int STATUS_CHECK_INTERVAL_MINUTES = 2;

    private DeviceApi deviceApi;
    private static DeviceRepository instance;
    private MutableLiveData<List<Device>> devicesLiveData = new MutableLiveData<>();
    private MutableLiveData<DeviceStatusResponse> deviceStatusLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();

    private ScheduledExecutorService statusCheckScheduler;
    private String currentUserEmail;
    private boolean isStatusMonitoringEnabled = false;

    private DeviceRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://us-central1-realmail-39ab4.cloudfunctions.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        deviceApi = retrofit.create(DeviceApi.class);
        statusCheckScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public static synchronized DeviceRepository getInstance() {
        if (instance == null) {
            instance = new DeviceRepository();
        }
        return instance;
    }

    public LiveData<List<Device>> getDevicesLiveData() {
        return devicesLiveData;
    }

    public LiveData<DeviceStatusResponse> getDeviceStatusLiveData() {
        return deviceStatusLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoading;
    }

    public LiveData<String> getToastMessageLiveData() {
        return toastMessage;
    }

    public void getDevices(String userEmail) {
        isLoading.setValue(true);
        deviceApi.getAllDevicesForUser(new DeviceRequest(userEmail)).enqueue(new Callback<List<Device>>() {
            @Override
            public void onResponse(Call<List<Device>> call, Response<List<Device>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    devicesLiveData.setValue(response.body());
                } else {
                    Log.e(TAG, "Failed to get devices: " + response.code());
                    toastMessage.setValue("Failed to load devices");
                }
            }

            @Override
            public void onFailure(Call<List<Device>> call, Throwable t) {
                isLoading.setValue(false);
                Log.e(TAG, "Network error getting devices", t);
                toastMessage.setValue("Network error loading devices");
            }
        });
    }

    public void addNewDevice(String userEmail, String deviceId) {
        isLoading.setValue(true);
        deviceApi.addNewDevice(new DeviceRequest(userEmail, deviceId)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    toastMessage.setValue("Device added successfully");
                    // Refresh the device list
                    getDevices(userEmail);
                } else {
                    Log.e(TAG, "Failed to add device: " + response.code());
                    toastMessage.setValue("Failed to add device");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLoading.setValue(false);
                Log.e(TAG, "Network error adding device", t);
                toastMessage.setValue("Network error adding device");
            }
        });
    }

    public void removeDevice(String userEmail, String deviceId) {
        isLoading.setValue(true);
        deviceApi.removeDevice(new DeviceRequest(userEmail, deviceId)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    toastMessage.setValue("Device removed successfully");
                    // Refresh the device list
                    getDevices(userEmail);
                } else {
                    Log.e(TAG, "Failed to remove device: " + response.code());
                    toastMessage.setValue("Failed to remove device");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLoading.setValue(false);
                Log.e(TAG, "Network error removing device", t);
                toastMessage.setValue("Network error removing device");
            }
        });
    }

    /**
     * Check device status for a specific user
     */
    public void checkDeviceStatus(String userEmail) {
        deviceApi.checkDeviceStatus(new DeviceRequest(userEmail)).enqueue(new Callback<DeviceStatusResponse>() {
            @Override
            public void onResponse(Call<DeviceStatusResponse> call, Response<DeviceStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeviceStatusResponse statusResponse = response.body();
                    deviceStatusLiveData.setValue(statusResponse);

                    // Update the devices list with status information
                    List<DeviceStatus> deviceStatuses = statusResponse.getDevices();
                    if (deviceStatuses != null && !deviceStatuses.isEmpty()) {
                        // Convert DeviceStatus to Device objects and update the devices list
                        updateDevicesWithStatus(deviceStatuses);
                    }

                    Log.d(TAG, String.format("Device status updated - Online: %d, Warning: %d, Offline: %d",
                            statusResponse.getOnlineCount(),
                            statusResponse.getWarningCount(),
                            statusResponse.getOfflineCount()));
                } else {
                    Log.e(TAG, "Failed to check device status: " + response.code());
                    toastMessage.setValue("Failed to check device status");
                }
            }

            @Override
            public void onFailure(Call<DeviceStatusResponse> call, Throwable t) {
                Log.e(TAG, "Network error checking device status", t);
                toastMessage.setValue("Network error checking device status");
            }
        });
    }

    /**
     * Update devices list with status information from DeviceStatus objects
     */
    private void updateDevicesWithStatus(List<DeviceStatus> deviceStatuses) {
        List<Device> currentDevices = devicesLiveData.getValue();
        if (currentDevices != null) {
            for (Device device : currentDevices) {
                for (DeviceStatus status : deviceStatuses) {
                    if (device.getDeviceId().equals(status.getDeviceId())) {
                        device.setStatus(status.getStatus());
                        device.setLastHeartbeat(status.getLastHeartbeat());
                        device.setLastHeartbeatTimestamp(status.getLastHeartbeatTimestamp());
                        break;
                    }
                }
            }
            // Trigger update of the LiveData
            devicesLiveData.setValue(currentDevices);
        }
    }

    /**
     * Start automatic device status monitoring every 2 minutes
     */
    public void startDeviceStatusMonitoring(String userEmail) {
        this.currentUserEmail = userEmail;
        this.isStatusMonitoringEnabled = true;

        Log.d(TAG, "Starting device status monitoring for user: " + userEmail);

        // Initial status check
        checkDeviceStatus(userEmail);

        // Schedule periodic status checks every 2 minutes
        statusCheckScheduler.scheduleAtFixedRate(() -> {
            if (isStatusMonitoringEnabled && currentUserEmail != null) {
                Log.d(TAG, "Performing scheduled device status check");
                checkDeviceStatus(currentUserEmail);
            }
        }, STATUS_CHECK_INTERVAL_MINUTES, STATUS_CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Stop automatic device status monitoring
     */
    public void stopDeviceStatusMonitoring() {
        Log.d(TAG, "Stopping device status monitoring");
        this.isStatusMonitoringEnabled = false;
        this.currentUserEmail = null;

        if (statusCheckScheduler != null && !statusCheckScheduler.isShutdown()) {
            statusCheckScheduler.shutdown();
            statusCheckScheduler = Executors.newSingleThreadScheduledExecutor();
        }
    }

    /**
     * Check if device status monitoring is currently active
     */
    public boolean isStatusMonitoringEnabled() {
        return isStatusMonitoringEnabled;
    }

    /**
     * Get the summary of device statuses
     */
    public String getDeviceStatusSummary() {
        DeviceStatusResponse status = deviceStatusLiveData.getValue();
        if (status == null) {
            return "No status data available";
        }

        return String.format("Total: %d | Online: %d | Warning: %d | Offline: %d",
                status.getTotalDevices(),
                status.getOnlineCount(),
                status.getWarningCount(),
                status.getOfflineCount());
    }

    /**
     * Clean up resources when the repository is no longer needed
     */
    public void cleanup() {
        stopDeviceStatusMonitoring();
        if (statusCheckScheduler != null) {
            statusCheckScheduler.shutdownNow();
        }
    }
}
