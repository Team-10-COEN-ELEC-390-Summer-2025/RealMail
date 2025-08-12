package com.team10.realmail.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.team10.realmail.api.Device;
import com.team10.realmail.api.DeviceRepository;

import java.util.List;

public class DeviceViewModel extends ViewModel {
    private final DeviceRepository deviceRepository;

    public DeviceViewModel() {
        this.deviceRepository = DeviceRepository.getInstance();
    }

    public LiveData<List<Device>> getDevices(String userEmail) {
        deviceRepository.getDevices(userEmail);
        return deviceRepository.getDevicesLiveData();
    }

    public void addDevice(String userEmail, String deviceId) {
        deviceRepository.addNewDevice(userEmail, deviceId);
    }

    public void removeDevice(String userEmail, String deviceId) {
        deviceRepository.removeDevice(userEmail, deviceId);
    }

    public LiveData<Boolean> getIsLoading() {
        return deviceRepository.getIsLoadingLiveData();
    }

    public LiveData<String> getToastMessage() {
        return deviceRepository.getToastMessageLiveData();
    }
}
