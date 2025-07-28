package com.team10.realmail.api;

import com.google.gson.annotations.SerializedName;

public class Device {
    @SerializedName("device_id")
    private String deviceId;

    public Device(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}

