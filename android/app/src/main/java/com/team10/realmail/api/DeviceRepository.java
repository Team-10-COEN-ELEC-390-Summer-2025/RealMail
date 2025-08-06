package com.team10.realmail.api;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DeviceRepository {

    private DeviceApi deviceApi;
    private static DeviceRepository instance;
    private MutableLiveData<List<Device>> devicesLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();


    private DeviceRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://us-central1-realmail-39ab4.cloudfunctions.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        deviceApi = retrofit.create(DeviceApi.class);
    }

    public static synchronized DeviceRepository getInstance() {
        if (instance == null) {
            instance = new DeviceRepository();
        }
        return instance;
    }

    public LiveData<List<Device>> getDevices(String userEmail) {
        deviceApi.getAllDevicesForUser(new DeviceRequest(userEmail)).enqueue(new Callback<List<Device>>() {
            @Override
            public void onResponse(Call<List<Device>> call, Response<List<Device>> response) {
                if (response.isSuccessful()) {
                    devicesLiveData.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Device>> call, Throwable t) {
                // Handle failure
            }
        });
        return devicesLiveData;
    }

    public void addDevice(DeviceRequest request) {
        isLoading.setValue(true);
        deviceApi.addNewDevice(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    toastMessage.postValue("Device added successfully");
                    getDevices(request.getUserEmail()); // Refresh the list
                } else {
                    toastMessage.postValue("Failed to add device");
                    Log.e("DeviceRepository", "Failed to add device: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLoading.setValue(false);
                toastMessage.postValue("Error adding device");
                Log.e("DeviceRepository", "Error adding device", t);
            }
        });
    }

    public void removeDevice(DeviceRequest request) {
        isLoading.setValue(true);
        deviceApi.removeDevice(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    toastMessage.postValue("Device removed successfully");
                    getDevices(request.getUserEmail()); // Refresh the list
                } else {
                    toastMessage.postValue("Failed to remove device");
                    Log.e("DeviceRepository", "Failed to remove device: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLoading.setValue(false);
                toastMessage.postValue("Error removing device");
                Log.e("DeviceRepository", "Error removing device", t);
            }
        });
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }
}
