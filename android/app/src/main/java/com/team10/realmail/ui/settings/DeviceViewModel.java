package com.team10.realmail.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.team10.realmail.api.Device;
import com.team10.realmail.api.DeviceRepository;
import com.team10.realmail.api.DeviceRequest;

import java.util.List;

public class DeviceViewModel extends ViewModel {
    private DeviceRepository deviceRepository;

    public DeviceViewModel() {
        this.deviceRepository = DeviceRepository.getInstance();
    }

    public LiveData<List<Device>> getDevices(String userEmail) {
        return deviceRepository.getDevices(userEmail);
    }

    public void addDevice(String userEmail, String deviceId) {
        deviceRepository.addDevice(new DeviceRequest(userEmail, deviceId));
    }

    public void removeDevice(String userEmail, String deviceId) {
        deviceRepository.removeDevice(new DeviceRequest(userEmail, deviceId));
    }

    public LiveData<Boolean> getIsLoading() {
        return deviceRepository.getIsLoading();
    }

    public LiveData<String> getToastMessage() {
        return deviceRepository.getToastMessage();
    }
}
