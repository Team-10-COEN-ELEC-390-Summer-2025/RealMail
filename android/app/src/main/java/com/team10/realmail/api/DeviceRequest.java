package com.team10.realmail.api;

import com.google.gson.annotations.SerializedName;

public class DeviceRequest {
    @SerializedName("user_email")
    private final String userEmail;

    @SerializedName("device_id")
    private final String deviceId;

    public DeviceRequest(String userEmail, String deviceId) {
        this.userEmail = userEmail;
        this.deviceId = deviceId;
    }

    public DeviceRequest(String userEmail) {
        this.userEmail = userEmail;
        this.deviceId = null;
    }

    public String getUserEmail() {
        return userEmail;
    }
}
