package com.team10.realmail;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.team10.realmail.api.DeviceRepository;
import com.team10.realmail.api.DeviceStatusResponse;
import com.team10.realmail.api.DeviceStatusService;

import java.util.ArrayList;
import java.util.Locale;

public class DeviceStatusActivity extends AppCompatActivity {

    private DeviceStatusService deviceStatusService;
    private DeviceRepository deviceRepository;
    private DeviceStatusAdapter deviceAdapter;
    private RecyclerView devicesRecyclerView;
    private TextView statusSummaryText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_status);

        // Initialize services
        deviceStatusService = DeviceStatusService.getInstance(this);
        deviceRepository = DeviceRepository.getInstance();

        // Get user email from Firebase Auth
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            userEmail = auth.getCurrentUser().getEmail();
        }

        initializeViews();
        setupRecyclerView();
        setupObservers();

        // Start monitoring if user is logged in
        if (userEmail != null) {
            deviceStatusService.startMonitoring(userEmail);
            // Also load the initial device list
            deviceRepository.getDevices(userEmail);
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        devicesRecyclerView = findViewById(R.id.devices_recyclerview);
        statusSummaryText = findViewById(R.id.status_summary_text);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (userEmail != null) {
                deviceStatusService.checkDeviceStatusNow(userEmail);
                deviceRepository.getDevices(userEmail);
            }
        });
    }

    private void setupRecyclerView() {
        deviceAdapter = new DeviceStatusAdapter(this, new ArrayList<>());
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        devicesRecyclerView.setAdapter(deviceAdapter);

        // Set up device removal listener
        deviceAdapter.setOnDeviceRemoveListener((device, position) -> {
            if (userEmail != null) {
                deviceRepository.removeDevice(userEmail, device.getDeviceId());
            }
        });
    }

    private void setupObservers() {
        // Observe device list changes
        deviceRepository.getDevicesLiveData().observe(this, devices -> {
            if (devices != null) {
                deviceAdapter.updateDevices(devices);
            }
        });

        // Observe device status updates
        deviceStatusService.getDeviceStatusLiveData().observe(this, statusResponse -> {
            if (statusResponse != null) {
                updateStatusSummary(statusResponse);
            }
        });

        // Observe loading state
        deviceRepository.getIsLoadingLiveData().observe(this, isLoading ->
                swipeRefreshLayout.setRefreshing(isLoading != null && isLoading));

        // Observe toast messages
        deviceRepository.getToastMessageLiveData().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(DeviceStatusActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatusSummary(DeviceStatusResponse statusResponse) {
        String summary = String.format(Locale.getDefault(), "Total: %d | Online: %d | Warning: %d | Offline: %d",
                statusResponse.getTotalDevices(),
                statusResponse.getOnlineCount(),
                statusResponse.getWarningCount(),
                statusResponse.getOfflineCount());
        statusSummaryText.setText(summary);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop monitoring when activity is destroyed
        if (deviceStatusService != null) {
            deviceStatusService.stopMonitoring();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume monitoring when activity comes back to foreground
        if (deviceStatusService != null && userEmail != null) {
            deviceStatusService.resumeMonitoringIfEnabled();
        }
    }
}
