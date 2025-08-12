package com.team10.realmail.api;

import com.google.gson.annotations.SerializedName;

public class DeviceStatus {
    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("connection_status")
    private String connectionStatus; // "offline", "online", "warning"

    @SerializedName("visual_indicator")
    private String visualIndicator; // "red", "green", "yellow"

    @SerializedName("last_seen")
    private String lastSeen;

    @SerializedName("minutes_since_last_seen")
    private String minutesSinceLastSeen;

    @SerializedName("cpu_temp")
    private String cpuTemp;

    @SerializedName("uptime_seconds")
    private String uptimeSeconds;

    @SerializedName("raw_status")
    private String rawStatus;

    @SerializedName("health_info")
    private HealthInfo healthInfo;

    public static class HealthInfo {
        @SerializedName("is_healthy")
        private boolean isHealthy;

        @SerializedName("last_heartbeat")
        private String lastHeartbeat;

        @SerializedName("uptime_hours")
        private int uptimeHours;

        public boolean isHealthy() {
            return isHealthy;
        }

        public String getLastHeartbeat() {
            return lastHeartbeat;
        }

        public int getUptimeHours() {
            return uptimeHours;
        }
    }

    public DeviceStatus() {
    }

    // Getters
    public String getDeviceId() {
        return deviceId;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public String getStatus() {
        return connectionStatus; // Alias for compatibility
    }

    public String getVisualIndicator() {
        return visualIndicator;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public String getLastHeartbeat() {
        return lastSeen; // Alias for compatibility
    }

    public String getMinutesSinceLastSeen() {
        return minutesSinceLastSeen;
    }

    public String getCpuTemp() {
        return cpuTemp;
    }

    public String getUptimeSeconds() {
        return uptimeSeconds;
    }

    public String getRawStatus() {
        return rawStatus;
    }

    public HealthInfo getHealthInfo() {
        return healthInfo;
    }

    // Setters
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public void setVisualIndicator(String visualIndicator) {
        this.visualIndicator = visualIndicator;
    }

    public void setLastSeen(String lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void setMinutesSinceLastSeen(String minutesSinceLastSeen) {
        this.minutesSinceLastSeen = minutesSinceLastSeen;
    }

    public void setCpuTemp(String cpuTemp) {
        this.cpuTemp = cpuTemp;
    }

    public void setUptimeSeconds(String uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public void setRawStatus(String rawStatus) {
        this.rawStatus = rawStatus;
    }

    public void setHealthInfo(HealthInfo healthInfo) {
        this.healthInfo = healthInfo;
    }

    // Helper methods for compatibility
    public boolean isOnline() {
        return "online".equals(connectionStatus);
    }

    public boolean isWarning() {
        return "warning".equals(connectionStatus);
    }

    public boolean isOffline() {
        return "offline".equals(connectionStatus);
    }

    public long getLastHeartbeatTimestamp() {
        // Convert ISO string to timestamp if needed
        // For now, return 0 since timestamp not provided in API
        return 0;
    }
}
