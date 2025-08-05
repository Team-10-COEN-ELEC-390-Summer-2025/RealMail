package com.team10.realmail.ui.settings;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team10.realmail.R;
import com.team10.realmail.api.Device;

import java.util.ArrayList;

public class DeviceSettingsActivity extends AppCompatActivity {

    private DeviceViewModel deviceViewModel;
    private RecyclerView devicesRecyclerView;
    private Button addDeviceButton;
    private DeviceAdapter adapter;
    private String userEmail;
    private ProgressBar loadingProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_settings);

        devicesRecyclerView = findViewById(R.id.devicesRecyclerView);
        addDeviceButton = findViewById(R.id.addDeviceButton);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);


        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userEmail = user.getEmail();
        }

        adapter = new DeviceAdapter(new ArrayList<>(), this::onRemoveDeviceClick);
        devicesRecyclerView.setAdapter(adapter);

        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);

        if (userEmail != null) {
            deviceViewModel.getDevices(userEmail).observe(this, devices -> {
                if (devices != null) {
                    adapter.setDevices(devices);
                }
            });
        }

        deviceViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                loadingProgressBar.setVisibility(View.VISIBLE);
            } else {
                loadingProgressBar.setVisibility(View.GONE);
            }
        });

        deviceViewModel.getToastMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });


        addDeviceButton.setOnClickListener(v -> showAddDeviceDialog());
    }

    private void onRemoveDeviceClick(Device device) {
        if (userEmail != null) {
            deviceViewModel.removeDevice(userEmail, device.getDeviceId());
        }
    }

    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Device");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter Device ID");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String deviceId = input.getText().toString();
            if (!deviceId.isEmpty() && userEmail != null) {
                deviceViewModel.addDevice(userEmail, deviceId);
            } else {
                Toast.makeText(this, "Device ID cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
