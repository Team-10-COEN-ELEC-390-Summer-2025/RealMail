package com.team10.realmail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class CameraStreamActivity extends AppCompatActivity implements DeviceIdInputFragment.OnDeviceIdEnteredListener {

    private static final String PREFS_NAME = "CameraPrefs";
    private static final String DEVICE_ID_KEY = "device_id";
    private static final String BASE_URL = "https://camera-webrtc-204949720800.us-central1.run.app/streamer/";

    public static final String EXTRA_DEVICE_ID = "device_id";

    private WebView cameraWebView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout loadingContainer;
    private TextView loadingText;
    private Toolbar toolbar;
    private String currentDeviceId;

    public static Intent createIntent(Context context, String deviceId) {
        Intent intent = new Intent(context, CameraStreamActivity.class);
        if (!TextUtils.isEmpty(deviceId)) {
            intent.putExtra(EXTRA_DEVICE_ID, deviceId);
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera_stream);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cameraContainer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupToolbar();
        setupWebView();
        setupSwipeRefresh();

        // Get device ID from intent or prompt user
        String deviceId = getIntent().getStringExtra(EXTRA_DEVICE_ID);
        if (TextUtils.isEmpty(deviceId)) {
            // Check if device ID is saved
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            deviceId = prefs.getString(DEVICE_ID_KEY, "");
        }

        if (TextUtils.isEmpty(deviceId)) {
            showDeviceIdInput();
        } else {
            loadCameraStream(deviceId);
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        cameraWebView = findViewById(R.id.cameraWebView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        loadingContainer = findViewById(R.id.loadingContainer);
        loadingText = findViewById(R.id.loadingText);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Camera Stream");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupWebView() {
        WebSettings webSettings = cameraWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setDefaultTextEncodingName("utf-8");

        // Better performance for video streaming
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        cameraWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                showLoading("Loading camera stream...");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hideLoading();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                hideLoading();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(CameraStreamActivity.this, "Failed to load camera stream: " + description, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!TextUtils.isEmpty(currentDeviceId)) {
                cameraWebView.reload();
            } else {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "No device ID set", Toast.LENGTH_SHORT).show();
            }
        });

        // Set refresh colors to match app theme
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }

    private void showDeviceIdInput() {
        DeviceIdInputFragment deviceIdFragment = new DeviceIdInputFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.cameraContainer, deviceIdFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    private void loadCameraStream(String deviceId) {
        currentDeviceId = deviceId;
        String streamUrl = BASE_URL + deviceId;

        // Save device ID for future use
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(DEVICE_ID_KEY, deviceId).apply();

        // Clear any fragments
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.cameraContainer);
        if (currentFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
        }

        // Update toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Camera - " + deviceId);
        }

        cameraWebView.setVisibility(View.VISIBLE);
        cameraWebView.loadUrl(streamUrl);

        Toast.makeText(this, "Connecting to device: " + deviceId, Toast.LENGTH_SHORT).show();
    }

    private void showLoading(String message) {
        loadingText.setText(message);
        loadingContainer.setVisibility(View.VISIBLE);
        cameraWebView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingContainer.setVisibility(View.GONE);
        cameraWebView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDeviceIdEntered(String deviceId) {
        loadCameraStream(deviceId);
    }

    @Override
    public void onDeviceIdCancelled() {
        Toast.makeText(this, "Camera access cancelled", Toast.LENGTH_SHORT).show();
        finish(); // Close the activity and return to previous screen
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            clearCameraData();
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        clearCameraData();
        super.onBackPressed();
    }

    private void clearCameraData() {
        // Clear WebView data and cache
        if (cameraWebView != null) {
            cameraWebView.clearCache(true);
            cameraWebView.clearHistory();
            cameraWebView.loadUrl("about:blank");
        }

        // Clear saved device ID so user has to enter it again next time
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove(DEVICE_ID_KEY).apply();

        android.util.Log.d("CameraStream", "Cleared camera data and device ID");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraWebView != null) {
            cameraWebView.onPause();
            // Clear WebView when pausing to prevent background activity
            cameraWebView.loadUrl("about:blank");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraWebView != null) {
            cameraWebView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        clearCameraData();
        if (cameraWebView != null) {
            cameraWebView.destroy();
        }
        super.onDestroy();
    }
}
