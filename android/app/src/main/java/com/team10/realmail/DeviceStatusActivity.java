package com.team10.realmail;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
    private Toolbar toolbar;
    private TextView emptyStateText;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device_status);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize services
        deviceStatusService = DeviceStatusService.getInstance(this);
        deviceRepository = DeviceRepository.getInstance();

        // Get user email from Firebase Auth
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            userEmail = auth.getCurrentUser().getEmail();
        }

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupSwipeRefresh();
        setupObservers();

        // Start monitoring if user is logged in
        if (userEmail != null) {
            deviceStatusService.startMonitoring(userEmail);
            // Manually trigger an initial fetch of devices
            deviceRepository.getDevices(userEmail);
        } else {
            statusSummaryText.setText("Please log in to view device status");
        }
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Device Status");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void initializeViews() {
        devicesRecyclerView = findViewById(R.id.devices_recyclerview);
        statusSummaryText = findViewById(R.id.status_summary_text);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        emptyStateText = findViewById(R.id.empty_state_text);
    }

    private void setupRecyclerView() {
        deviceAdapter = new DeviceStatusAdapter(this, new ArrayList<>());
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        devicesRecyclerView.setAdapter(deviceAdapter);

        // Add temporary test data to see if RecyclerView is working
        android.util.Log.d("DeviceStatus", "Setting up RecyclerView with test data");

        // Create a test device to verify the UI is working
        java.util.List<com.team10.realmail.api.Device> testDevices = new java.util.ArrayList<>();
        // Note: We'll need to check if we can create Device objects directly
        // If not, we'll need to find another way to test
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (userEmail != null) {
                deviceStatusService.checkDeviceStatusNow(userEmail);
            } else {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "Please log in to refresh device status", Toast.LENGTH_SHORT).show();
            }
        });

        // Set refresh colors to match app theme
        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.orange, null),
                getResources().getColor(R.color.mustard, null),
                getResources().getColor(R.color.DarkBrown, null)
        );
    }

    private void setupObservers() {
        // Observe device list changes
        deviceRepository.getDevicesLiveData().observe(this, devices -> {
            // Add debugging to see if devices are being received
            if (devices != null) {
                android.util.Log.d("DeviceStatus", "Received " + devices.size() + " devices");
                for (int i = 0; i < Math.min(devices.size(), 3); i++) {
                    android.util.Log.d("DeviceStatus", "Device " + i + ": " + devices.get(i).getDeviceId());
                }
                deviceAdapter.updateDevices(devices);

                // Show/hide empty state
                if (devices.isEmpty()) {
                    emptyStateText.setVisibility(android.view.View.VISIBLE);
                    devicesRecyclerView.setVisibility(android.view.View.GONE);
                } else {
                    emptyStateText.setVisibility(android.view.View.GONE);
                    devicesRecyclerView.setVisibility(android.view.View.VISIBLE);
                }
            } else {
                android.util.Log.d("DeviceStatus", "Received null devices list");
                // Show empty state for null data
                emptyStateText.setVisibility(android.view.View.VISIBLE);
                emptyStateText.setText("Unable to load devices.\nPull down to refresh.");
                devicesRecyclerView.setVisibility(android.view.View.GONE);
            }
        });

        // Observe device status updates
        deviceStatusService.getDeviceStatusLiveData().observe(this, statusResponse -> {
            android.util.Log.d("DeviceStatus", "Received status response: " + (statusResponse != null));
            if (statusResponse != null) {
                android.util.Log.d("DeviceStatus", "Total devices in status: " + statusResponse.getTotalDevices());
                updateStatusSummary(statusResponse);
            }
        });

        // Observe loading state
        deviceRepository.getIsLoadingLiveData().observe(this, isLoading -> {
            android.util.Log.d("DeviceStatus", "Loading state: " + isLoading);
            swipeRefreshLayout.setRefreshing(isLoading != null && isLoading);
        });

        // Observe toast messages
        deviceRepository.getToastMessageLiveData().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                android.util.Log.d("DeviceStatus", "Toast message: " + message);
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
