package com.team10.realmail;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team10.realmail.api.DeviceApi;
import com.team10.realmail.api.DeviceRequest;
import com.team10.realmail.utils.NetworkScanner;
import com.team10.realmail.utils.SSHHelper;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DeviceSetupActivity extends AppCompatActivity {

    // UI Components for new 4-step flow
    private LinearLayout step1Layout, step2Layout, step3Layout, step4Layout;
    private TextInputEditText deviceNameInput, homeWifiSsidInput, homeWifiPasswordInput;
    private Button btnNextStep1, btnCreateHotspot, btnScanSetupNetwork, btnConnectHomePi, btnFinishSetup;
    private ProgressBar setupProgress;
    private TextView setupStatusText, piConnectionInfo, homeWifiStatusText;

    // Setup flow data
    private String deviceName;
    private String detectedPiIP;
    private String homeWifiSsid;
    private String homeWifiPassword;
    private String userEmail;
    private SSHHelper sshHelper;

    // API
    private DeviceApi deviceApi;
    private static final String BASE_URL = "https://us-central1-realmail-39ab4.cloudfunctions.net/";
    private static final int PERMISSIONS_REQUEST_CODE = 100;

    // Setup constants
    private static final String SETUP_HOTSPOT_SSID = "setmeup123";
    private static final String SETUP_HOTSPOT_PASSWORD = "setmeup123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_setup);

        initializeComponents();
        setupToolbar();
        setupClickListeners();
        checkAndRequestAllPermissions();

        // Get user email
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userEmail = user.getEmail();
        }

        // Initialize services
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        deviceApi = retrofit.create(DeviceApi.class);

        sshHelper = new SSHHelper();

        // Start with step 1
        showStep(1);
    }

    private void initializeComponents() {
        // Step layouts
        step1Layout = findViewById(R.id.step1_layout);
        step2Layout = findViewById(R.id.step2_layout);
        step3Layout = findViewById(R.id.step3_layout);
        step4Layout = findViewById(R.id.step4_layout);

        // Input fields
        deviceNameInput = findViewById(R.id.device_name_input);
        homeWifiSsidInput = findViewById(R.id.home_wifi_ssid_input);
        homeWifiPasswordInput = findViewById(R.id.home_wifi_password_input);

        // Buttons
        btnNextStep1 = findViewById(R.id.btn_next_step1);
        btnCreateHotspot = findViewById(R.id.btn_create_hotspot);
        btnScanSetupNetwork = findViewById(R.id.btn_scan_setup_network);
        btnConnectHomePi = findViewById(R.id.btn_connect_home_pi);
        btnFinishSetup = findViewById(R.id.btn_finish_setup);

        // Status displays
        setupProgress = findViewById(R.id.setup_progress);
        setupStatusText = findViewById(R.id.setup_status_text);
        piConnectionInfo = findViewById(R.id.pi_connection_info);
        homeWifiStatusText = findViewById(R.id.home_wifi_status_text);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnNextStep1.setOnClickListener(v -> validateAndProceedToStep2());
        btnCreateHotspot.setOnClickListener(v -> showHotspotInstructions());
        btnScanSetupNetwork.setOnClickListener(v -> scanForPiOnSetupNetwork());
        btnConnectHomePi.setOnClickListener(v -> configureHomeWifi());
        btnFinishSetup.setOnClickListener(v -> finishSetup());
    }

    private void validateAndProceedToStep2() {
        deviceName = deviceNameInput.getText().toString().trim();

        if (TextUtils.isEmpty(deviceName)) {
            deviceNameInput.setError("Please enter a device name");
            return;
        }

        if (deviceName.length() < 3) {
            deviceNameInput.setError("Device name must be at least 3 characters long");
            return;
        }

        showStep(2);
    }

    /**
     * STEP 2: Show setup instructions with USB and WiFi options
     */
    private void showHotspotInstructions() {
        new AlertDialog.Builder(this)
                .setTitle("Choose Setup Method")
                .setMessage("Select how you want to connect to your Raspberry Pi:\n\n" +
                        "OPTION 1 - USB Connection (Recommended):\n" +
                        "• Connect Pi to phone via USB cable\n" +
                        "• Direct connection, no network needed\n" +
                        "• Most reliable method\n\n" +
                        "OPTION 2 - WiFi Network:\n" +
                        "• Connect Pi to same WiFi as phone\n" +
                        "• Or use mobile hotspot 'setmeup123'\n" +
                        "• Requires network configuration\n\n" +
                        "Which method do you want to use?")
                .setPositiveButton("USB Setup", (dialog, which) -> showUsbSetupInstructions())
                .setNegativeButton("WiFi Setup", (dialog, which) -> showWifiSetupInstructions())
                .show();
    }

    /**
     * Show USB setup instructions
     */
    private void showUsbSetupInstructions() {
        new AlertDialog.Builder(this)
                .setTitle("USB Setup Instructions")
                .setMessage("Follow these steps for USB setup:\n\n" +
                        "1. Connect your Raspberry Pi to your phone using a USB cable\n" +
                        "2. Make sure USB debugging/OTG is enabled on your phone\n" +
                        "3. Power on the Raspberry Pi\n" +
                        "4. Wait 1-2 minutes for the Pi to boot up\n" +
                        "5. Tap 'Scan via USB' below\n\n" +
                        "Note: Your Pi should have SSH enabled and default credentials configured.")
                .setPositiveButton("Got it", null)
                .show();

        // Update UI for USB mode
        updateUIForSetupMode(true);
    }

    /**
     * Show WiFi setup instructions
     */
    private void showWifiSetupInstructions() {
        new AlertDialog.Builder(this)
                .setTitle("WiFi Setup Instructions")
                .setMessage("Choose your WiFi setup method:\n\n" +
                        "METHOD 1 - Same WiFi Network (Easiest):\n" +
                        "• Connect Pi to your home WiFi\n" +
                        "• Keep phone on same network\n\n" +
                        "METHOD 2 - Mobile Hotspot:\n" +
                        "• Create hotspot: setmeup123 / setmeup123\n" +
                        "• Note: This will disable your WiFi\n\n" +
                        "METHOD 3 - Ethernet:\n" +
                        "• Connect Pi via Ethernet to router\n" +
                        "• Keep phone on WiFi\n\n" +
                        "Then power on your Pi and tap 'Scan via WiFi'")
                .setPositiveButton("Open Network Settings", (dialog, which) -> {
                    try {
                        startActivity(new android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                    } catch (Exception e) {
                        Toast.makeText(this, "Please open Settings > Network manually", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("I'm Ready", null)
                .show();

        // Update UI for WiFi mode
        updateUIForSetupMode(false);
    }

    /**
     * Update UI based on setup mode (USB vs WiFi)
     */
    private void updateUIForSetupMode(boolean isUsbMode) {
        if (isUsbMode) {
            btnCreateHotspot.setText("USB Setup Instructions");
            btnScanSetupNetwork.setText("Scan via USB");
            setupStatusText.setText("Ready for USB setup");
        } else {
            btnCreateHotspot.setText("WiFi Setup Instructions");
            btnScanSetupNetwork.setText("Scan via WiFi");
            setupStatusText.setText("Ready for WiFi setup");
        }
    }

    /**
     * STEP 2: Scan for Raspberry Pi on any available network
     */
    private void scanForPiOnSetupNetwork() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }

        // Get current network info for display, but don't restrict scanning
        String currentSSID = getCurrentWiFiSSID();
        String networkInfo = currentSSID != null ? currentSSID : "Mobile Data/Hotspot";

        setupProgress.setVisibility(View.VISIBLE);
        setupStatusText.setText("Scanning for Raspberry Pi on " + networkInfo + "...");
        btnScanSetupNetwork.setEnabled(false);

        NetworkScanner.scanForRaspberryPi(this, new NetworkScanner.NetworkScanCallback() {
            @Override
            public void onDeviceFound(NetworkScanner.DetectedDevice device) {
                runOnUiThread(() -> {
                    setupStatusText.setText("Found Pi at " + device.ipAddress + " - Testing SSH...");
                });

                // Test SSH connection to confirm it's our Pi
                sshHelper.testConnection(device.ipAddress, new SSHHelper.SSHCallback() {
                    @Override
                    public void onSuccess(String result) {
                        runOnUiThread(() -> {
                            detectedPiIP = device.ipAddress;
                            piConnectionInfo.setText("✓ Raspberry Pi connected at " + detectedPiIP);
                            piConnectionInfo.setVisibility(View.VISIBLE);
                            setupProgress.setVisibility(View.GONE);
                            setupStatusText.setText("Pi found and SSH connection verified!");
                            btnScanSetupNetwork.setEnabled(true);

                            // Check if Pi is already on a home network (not setup network)
                            if (currentSSID != null && !currentSSID.equals(SETUP_HOTSPOT_SSID)) {
                                // Pi is already on a home network, skip to final step
                                new AlertDialog.Builder(DeviceSetupActivity.this)
                                        .setTitle("Pi Already Configured!")
                                        .setMessage("Great! Your Raspberry Pi is already connected to your home network.\n\n" +
                                                "Network: " + currentSSID + "\n" +
                                                "Pi IP: " + detectedPiIP + "\n\n" +
                                                "Skipping WiFi configuration...")
                                        .setPositiveButton("Continue to Registration", (dialog, which) -> {
                                            homeWifiSsid = currentSSID; // Set for display purposes
                                            showStep(4);
                                        })
                                        .setCancelable(false)
                                        .show();
                            } else {
                                // Pi is on setup network, proceed to WiFi configuration
                                showStep(3);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            setupStatusText.setText("Found device but SSH failed. Continuing scan...");
                        });
                    }

                    @Override
                    public void onProgress(String status) {
                        runOnUiThread(() -> setupStatusText.setText(status));
                    }
                });
            }

            @Override
            public void onScanComplete(List<NetworkScanner.DetectedDevice> devices) {
                runOnUiThread(() -> {
                    if (detectedPiIP == null) {
                        setupProgress.setVisibility(View.GONE);
                        setupStatusText.setText("No Raspberry Pi found with SSH access.");
                        btnScanSetupNetwork.setEnabled(true);

                        new AlertDialog.Builder(DeviceSetupActivity.this)
                                .setTitle("Pi Not Found")
                                .setMessage("Troubleshooting tips:\n\n" +
                                        "• Make sure Pi is powered on (wait 2 minutes after boot)\n" +
                                        "• Verify Pi and phone are on same network\n" +
                                        "• Check SSH is enabled on Pi\n" +
                                        "• Try using mobile hotspot 'setmeup123'\n" +
                                        "• Connect Pi via Ethernet cable\n\n" +
                                        "Current network: " + networkInfo)
                                .setPositiveButton("Try Again", (dialog, which) -> scanForPiOnSetupNetwork())
                                .setNegativeButton("Back", null)
                                .show();
                    }
                });
            }

            @Override
            public void onScanProgress(String status) {
                runOnUiThread(() -> setupStatusText.setText(status));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setupProgress.setVisibility(View.GONE);
                    setupStatusText.setText("Scan failed: " + error);
                    btnScanSetupNetwork.setEnabled(true);
                });
            }
        });
    }

    /**
     * STEP 3: Configure home WiFi on the Pi via SSH
     */
    private void configureHomeWifi() {
        homeWifiSsid = homeWifiSsidInput.getText().toString().trim();
        homeWifiPassword = homeWifiPasswordInput.getText().toString().trim();

        if (TextUtils.isEmpty(homeWifiSsid) || TextUtils.isEmpty(homeWifiPassword)) {
            Toast.makeText(this, "Please enter your home WiFi details", Toast.LENGTH_SHORT).show();
            return;
        }

        setupProgress.setVisibility(View.VISIBLE);
        setupStatusText.setText("Testing WiFi credentials and configuring Raspberry Pi...");
        btnConnectHomePi.setEnabled(false);

        sshHelper.addWiFiNetworkWithTest(detectedPiIP, homeWifiSsid, homeWifiPassword, new SSHHelper.SSHCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    setupStatusText.setText("Home WiFi configured! Pi will reboot and connect to your home network.");

                    new AlertDialog.Builder(DeviceSetupActivity.this)
                            .setTitle("WiFi Configured Successfully")
                            .setMessage("The Raspberry Pi has been configured with your home WiFi settings.\n\n" +
                                    "Next steps:\n" +
                                    "1. Turn off your mobile hotspot\n" +
                                    "2. Connect your phone to the same home WiFi network: " + homeWifiSsid + "\n" +
                                    "3. Wait 1-2 minutes for the Pi to connect\n" +
                                    "4. Tap 'Find Pi on Home Network'")
                            .setPositiveButton("Continue", (dialog, which) -> {
                                showStep(4);
                                setupProgress.setVisibility(View.GONE);
                                btnConnectHomePi.setEnabled(true);
                            })
                            .setCancelable(false)
                            .show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setupProgress.setVisibility(View.GONE);
                    setupStatusText.setText("Failed to configure WiFi: " + error);
                    btnConnectHomePi.setEnabled(true);
                    Toast.makeText(DeviceSetupActivity.this, "WiFi configuration failed. Please try again.", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onProgress(String status) {
                runOnUiThread(() -> setupStatusText.setText(status));
            }
        });
    }

    /**
     * STEP 4: Find Pi on home network and complete setup
     */
    private void findPiOnHomeNetwork() {
        // Skip WiFi network validation - scan on any network
        setupProgress.setVisibility(View.VISIBLE);
        setupStatusText.setText("Scanning for Raspberry Pi...");

        NetworkScanner.scanForRaspberryPi(this, new NetworkScanner.NetworkScanCallback() {
            @Override
            public void onDeviceFound(NetworkScanner.DetectedDevice device) {
                // Test SSH to verify it's our Pi
                sshHelper.testConnection(device.ipAddress, new SSHHelper.SSHCallback() {
                    @Override
                    public void onSuccess(String result) {
                        runOnUiThread(() -> {
                            detectedPiIP = device.ipAddress;
                            homeWifiStatusText.setText("✓ Raspberry Pi found at " + detectedPiIP);
                            setupProgress.setVisibility(View.GONE);
                            setupStatusText.setText("Pi successfully connected!");
                            btnFinishSetup.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        // Continue scanning
                    }

                    @Override
                    public void onProgress(String status) {
                        runOnUiThread(() -> setupStatusText.setText(status));
                    }
                });
            }

            @Override
            public void onScanComplete(List<NetworkScanner.DetectedDevice> devices) {
                runOnUiThread(() -> {
                    if (detectedPiIP == null || !homeWifiStatusText.getText().toString().contains("✓")) {
                        setupProgress.setVisibility(View.GONE);
                        setupStatusText.setText("Pi not found. It may still be connecting...");

                        new AlertDialog.Builder(DeviceSetupActivity.this)
                                .setTitle("Pi Not Found")
                                .setMessage("The Pi might still be connecting. Please:\n" +
                                        "• Wait another minute\n" +
                                        "• Make sure the Pi has power\n" +
                                        "• Check that SSH is enabled")
                                .setPositiveButton("Try Again", (dialog, which) -> findPiOnHomeNetwork())
                                .setNegativeButton("Back to Step 3", (dialog, which) -> showStep(3))
                                .show();
                    }
                });
            }

            @Override
            public void onScanProgress(String status) {
                runOnUiThread(() -> setupStatusText.setText(status));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setupProgress.setVisibility(View.GONE);
                    setupStatusText.setText("Network scan failed: " + error);
                });
            }
        });
    }

    private void finishSetup() {
        if (TextUtils.isEmpty(deviceName) || userEmail == null) {
            Toast.makeText(this, "Setup incomplete. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        setupProgress.setVisibility(View.VISIBLE);
        setupStatusText.setText("Registering device with server...");

        DeviceRequest request = new DeviceRequest(userEmail, deviceName);
        Call<Void> call = deviceApi.addNewDevice(request);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                runOnUiThread(() -> {
                    setupProgress.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        new AlertDialog.Builder(DeviceSetupActivity.this)
                                .setTitle("Setup Complete!")
                                .setMessage("Your Raspberry Pi has been successfully configured and registered.\n\n" +
                                        "Device: " + deviceName + "\n" +
                                        "IP Address: " + detectedPiIP + "\n" +
                                        "Network: " + homeWifiSsid)
                                .setPositiveButton("Finish", (dialog, which) -> {
                                    setResult(RESULT_OK);
                                    finish();
                                })
                                .setCancelable(false)
                                .show();
                    } else {
                        setupStatusText.setText("Failed to register device. Please try again.");
                        Toast.makeText(DeviceSetupActivity.this, "Registration failed: " + response.message(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                runOnUiThread(() -> {
                    setupProgress.setVisibility(View.GONE);
                    setupStatusText.setText("Registration failed: " + t.getMessage());
                    Toast.makeText(DeviceSetupActivity.this, "Registration failed. Please check your connection.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showStep(int stepNumber) {
        // Hide all steps
        step1Layout.setVisibility(View.GONE);
        step2Layout.setVisibility(View.GONE);
        step3Layout.setVisibility(View.GONE);
        step4Layout.setVisibility(View.GONE);

        // Show the requested step
        switch (stepNumber) {
            case 1:
                step1Layout.setVisibility(View.VISIBLE);
                setTitle("Step 1: Device Name");
                break;
            case 2:
                step2Layout.setVisibility(View.VISIBLE);
                setTitle("Step 2: Setup Network");
                break;
            case 3:
                step3Layout.setVisibility(View.VISIBLE);
                setTitle("Step 3: Configure Home WiFi");
                break;
            case 4:
                step4Layout.setVisibility(View.VISIBLE);
                setTitle("Step 4: Final Connection");
                // Auto-start the home network scan
                new Handler().postDelayed(this::findPiOnHomeNetwork, 1000);
                break;
        }
    }

    private String getCurrentWiFiSSID() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if (wifiInfo != null && wifiInfo.getSSID() != null) {
                return wifiInfo.getSSID().replace("\"", ""); // Remove quotes
            }
        } catch (Exception e) {
            Log.e("DeviceSetup", "Error getting WiFi SSID", e);
        }
        return null;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Location Permission Required")
                    .setMessage("Location permission is needed to detect your WiFi network name for Pi setup.")
                    .setPositiveButton("Grant Permission", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this,
                                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSIONS_REQUEST_CODE);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_CODE);
        }
    }

    private void checkAndRequestAllPermissions() {
        String[] requiredPermissions = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_WIFI_STATE,
                android.Manifest.permission.CHANGE_WIFI_STATE,
                android.Manifest.permission.ACCESS_NETWORK_STATE
        };

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSIONS_REQUEST_CODE);
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions are required for WiFi setup", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sshHelper != null) {
            sshHelper.shutdown();
        }
    }
}
