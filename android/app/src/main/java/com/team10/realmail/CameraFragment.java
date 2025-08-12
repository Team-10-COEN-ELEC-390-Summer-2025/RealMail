package com.team10.realmail;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class CameraFragment extends Fragment implements DeviceIdInputFragment.OnDeviceIdEnteredListener {

    private static final String PREFS_NAME = "CameraPrefs";
    private static final String DEVICE_ID_KEY = "device_id";
    private static final String BASE_URL = "https://camera-webrtc-204949720800.us-central1.run.app/streamer/";

    private WebView cameraWebView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentDeviceId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        cameraWebView = view.findViewById(R.id.cameraWebView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        setupWebView();
        setupSwipeRefresh();

        // Check if device ID is already saved
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            currentDeviceId = prefs.getString(DEVICE_ID_KEY, "");

            if (TextUtils.isEmpty(currentDeviceId)) {
                // Show device ID input fragment
                showDeviceIdInput();
            } else {
                // Load camera stream with saved device ID
                loadCameraStream(currentDeviceId);
            }
        }

        return view;
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

        // Enable hardware acceleration for better video performance
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        cameraWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Failed to load camera stream: " + description, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!TextUtils.isEmpty(currentDeviceId)) {
                cameraWebView.reload();
            } else {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "No device ID set", Toast.LENGTH_SHORT).show();
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

        getChildFragmentManager().beginTransaction()
                .replace(R.id.cameraContainer, deviceIdFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    private void loadCameraStream(String deviceId) {
        currentDeviceId = deviceId;
        String streamUrl = BASE_URL + deviceId;

        // Clear any child fragments
        Fragment currentFragment = getChildFragmentManager().findFragmentById(R.id.cameraContainer);
        if (currentFragment != null) {
            getChildFragmentManager().beginTransaction().remove(currentFragment).commit();
        }

        swipeRefreshLayout.setRefreshing(true);
        cameraWebView.setVisibility(View.VISIBLE);
        cameraWebView.loadUrl(streamUrl);

        Toast.makeText(getContext(), "Loading camera stream for device: " + deviceId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeviceIdEntered(String deviceId) {
        // Remove the device ID input fragment and show the WebView
        getChildFragmentManager().popBackStack();

        // Load the camera stream
        loadCameraStream(deviceId);
    }

    @Override
    public void onDeviceIdCancelled() {
        // User cancelled device ID input, you might want to navigate back or show a message
        Toast.makeText(getContext(), "Camera access cancelled", Toast.LENGTH_SHORT).show();

        // Optionally navigate to another tab or show a placeholder
        if (getActivity() instanceof HomeActivity) {
            // Switch to Summary tab (position 1)
            // You can implement this by calling the parent activity method
        }
    }

    public void changeDeviceId() {
        // Method to allow changing device ID - can be called from menu or settings
        showDeviceIdInput();
    }

    @Override
    public void onDestroyView() {
        if (cameraWebView != null) {
            cameraWebView.destroy();
        }
        super.onDestroyView();
    }
}
