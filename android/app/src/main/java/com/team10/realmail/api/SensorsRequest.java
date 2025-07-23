package com.team10.realmail.api;

// Request body model
public class SensorsRequest {
    public String device_id;
    public String user_email;

    public SensorsRequest(String device_id, String user_email) {
        this.device_id = device_id;
        this.user_email = user_email;
    }
}
