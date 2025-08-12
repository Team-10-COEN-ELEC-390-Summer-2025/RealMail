package com.team10.realmail;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.team10.realmail.api.Device;
import com.team10.realmail.api.DeviceStatusService;

import java.util.List;

public class DeviceStatusAdapter extends RecyclerView.Adapter<DeviceStatusAdapter.DeviceViewHolder> {

    private List<Device> devices;
    private final Context context;

    public DeviceStatusAdapter(Context context, List<Device> devices) {
        this.context = context;
        this.devices = devices;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_with_status, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = devices.get(position);

        // Safely handle null device
        if (device == null) {
            holder.deviceName.setText("Unknown Device");
            holder.statusText.setText("Unknown");
            holder.lastSeenText.setText("No data");
            return;
        }

        // Set device ID with null check
        String deviceId = device.getDeviceId();
        holder.deviceName.setText(deviceId != null ? deviceId : "Unknown Device");

        // Set status indicator using connection_status with null safety
        String connectionStatus = device.getConnectionStatus() != null ? device.getConnectionStatus() : "offline";
        holder.statusText.setText(DeviceStatusService.getStatusDisplayText(connectionStatus));

        // Set status color using the improved method that handles nulls
        int statusColor = ContextCompat.getColor(context, DeviceStatusService.getDeviceStatusColor(device));
        holder.statusIndicator.setColorFilter(statusColor);
        holder.statusText.setTextColor(statusColor);

        // Set last seen time with comprehensive null checks and proper date formatting
        if (device.getLastSeen() != null && !device.getLastSeen().isEmpty()) {
            // Show minutes since last seen if available (prioritize this over formatted date)
            if (device.getMinutesSinceLastSeen() != null && !device.getMinutesSinceLastSeen().isEmpty()) {
                String lastSeenText = context.getString(R.string.last_seen_minutes_format,
                        device.getMinutesSinceLastSeen());
                holder.lastSeenText.setText(lastSeenText);
            } else {
                // Format the ISO date string to readable format
                String formattedDate = formatLastSeenDate(device.getLastSeen());
                String lastSeenText = context.getString(R.string.last_seen_format, formattedDate);
                holder.lastSeenText.setText(lastSeenText);
            }
            holder.lastSeenText.setVisibility(View.VISIBLE);
        } else {
            holder.lastSeenText.setText(context.getString(R.string.no_heartbeat_data));
            holder.lastSeenText.setVisibility(View.VISIBLE);
        }

        // Long click for device details
        holder.itemView.setOnLongClickListener(v -> {
            showDeviceDetails(device);
            return true;
        });
    }

    private void showDeviceDetails(Device device) {
        StringBuilder details = new StringBuilder();
        details.append(context.getString(R.string.device_id_label, device.getDeviceId())).append("\n");
        details.append(context.getString(R.string.status_label, DeviceStatusService.getStatusDisplayText(device.getConnectionStatus()))).append("\n");

        if (device.getLastSeen() != null && !device.getLastSeen().isEmpty()) {
            details.append(context.getString(R.string.last_seen_label, device.getLastSeen())).append("\n");
        }

        if (device.getMinutesSinceLastSeen() != null) {
            details.append(context.getString(R.string.minutes_since_last_seen_label, device.getMinutesSinceLastSeen())).append("\n");
        }

        if (device.getCpuTemp() != null) {
            details.append(context.getString(R.string.cpu_temp_label, device.getCpuTemp())).append("\n");
        }

        if (device.getUptimeSeconds() != null) {
            details.append(context.getString(R.string.uptime_label, device.getUptimeSeconds()));
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.device_details_title)
                .setMessage(details.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Format ISO 8601 date string to readable format
     * Converts "2025-08-07T10:15:30.000Z" to "Aug 07, 10:15 AM"
     */
    private String formatLastSeenDate(String isoDateString) {
        return DateFormatter.formatDateForNotification(isoDateString);
    }

    @Override
    public int getItemCount() {
        return devices != null ? devices.size() : 0;
    }

    public void updateDevices(List<Device> newDevices) {
        this.devices = newDevices;
        notifyDataSetChanged();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView statusText;
        TextView lastSeenText;
        ImageView statusIndicator;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            statusText = itemView.findViewById(R.id.status_text);
            lastSeenText = itemView.findViewById(R.id.last_seen_text);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }
    }
}
