package com.team10.realmail.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface DeviceApi {
    @Headers({"Content-Type: application/json"})
    @POST("getAllDevicesForUser")
    Call<List<Device>> getAllDevicesForUser(@Body DeviceRequest request);

    @Headers({"Content-Type: application/json"})
    @POST("addNewDevice")
    Call<Void> addNewDevice(@Body DeviceRequest request);

    @Headers({"Content-Type: application/json"})
    @POST("removeDevice")
    Call<Void> removeDevice(@Body DeviceRequest request);
}
