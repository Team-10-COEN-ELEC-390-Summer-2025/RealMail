package com.team10.realmail.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DeviceStatusResponse {
    @SerializedName("user_email")
    private String userEmail;

    @SerializedName("summary")
    private Summary summary;

    @SerializedName("devices")
    private List<DeviceStatus> devices;

    public static class Summary {
        @SerializedName("total_devices")
        private int totalDevices;

        @SerializedName("online")
        private int online;

        @SerializedName("warning")
        private int warning;

        @SerializedName("offline")
        private int offline;

        @SerializedName("last_updated")
        private String lastUpdated;

        public int getTotalDevices() {
            return totalDevices;
        }

        public int getOnline() {
            return online;
        }

        public int getWarning() {
            return warning;
        }

        public int getOffline() {
            return offline;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }
    }

    public DeviceStatusResponse() {
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public List<DeviceStatus> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceStatus> devices) {
        this.devices = devices;
    }

    // Compatibility methods for existing code
    public long getTimestamp() {
        // Could parse lastUpdated if needed
        return System.currentTimeMillis();
    }

    public void setTimestamp(long timestamp) {
        // Not used by API response
    }

    public int getTotalDevices() {
        return summary != null ? summary.getTotalDevices() : 0;
    }

    public void setTotalDevices(int totalDevices) {
        // Not used by API response
    }

    public int getOnlineCount() {
        return summary != null ? summary.getOnline() : 0;
    }

    public void setOnlineCount(int onlineCount) {
        // Not used by API response
    }

    public int getWarningCount() {
        return summary != null ? summary.getWarning() : 0;
    }

    public void setWarningCount(int warningCount) {
        // Not used by API response
    }

    public int getOfflineCount() {
        return summary != null ? summary.getOffline() : 0;
    }

    public void setOfflineCount(int offlineCount) {
        // Not used by API response
    }
}
