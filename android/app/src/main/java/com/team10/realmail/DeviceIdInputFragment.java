package com.team10.realmail;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class DeviceIdInputFragment extends Fragment {

    private static final String PREFS_NAME = "CameraPrefs";
    private static final String DEVICE_ID_KEY = "device_id";

    private TextInputEditText deviceIdEditText;
    private MaterialButton connectButton;
    private MaterialButton cancelButton;

    private OnDeviceIdEnteredListener listener;

    public interface OnDeviceIdEnteredListener {
        void onDeviceIdEntered(String deviceId);

        void onDeviceIdCancelled();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnDeviceIdEnteredListener) {
            listener = (OnDeviceIdEnteredListener) context;
        } else if (getParentFragment() instanceof OnDeviceIdEnteredListener) {
            listener = (OnDeviceIdEnteredListener) getParentFragment();
        } else {
            throw new RuntimeException(context
                    + " must implement OnDeviceIdEnteredListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_id_input, container, false);

        deviceIdEditText = view.findViewById(R.id.deviceIdEditText);
        connectButton = view.findViewById(R.id.connectButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        // Load previously saved device ID if available
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String savedDeviceId = prefs.getString(DEVICE_ID_KEY, "");
            if (!TextUtils.isEmpty(savedDeviceId)) {
                deviceIdEditText.setText(savedDeviceId);
            }
        }

        connectButton.setOnClickListener(v -> {
            String deviceId = deviceIdEditText.getText().toString().trim();
            if (TextUtils.isEmpty(deviceId)) {
                Toast.makeText(getContext(), "Please enter a device ID", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save device ID for future use
            if (getActivity() != null) {
                SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString(DEVICE_ID_KEY, deviceId).apply();
            }

            if (listener != null) {
                listener.onDeviceIdEntered(deviceId);
            }
        });

        cancelButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceIdCancelled();
            }
        });

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}
