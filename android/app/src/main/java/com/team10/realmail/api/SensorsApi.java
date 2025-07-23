package com.team10.realmail.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.util.List;

public interface SensorsApi {
    @Headers({"Content-Type: application/json"})
    @POST("getSensorsWithMotionDetected")
    Call<List<SensorsData>> getSensorsWithMotionDetected(@Body SensorsRequest request);
}
