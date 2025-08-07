package com.team10.realmail.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class NetworkScanner {
    private static final String TAG = "NetworkScanner";
    private static final int SSH_PORT = 22;
    private static final int TIMEOUT_MS = 1000;

    public static class DetectedDevice {
        public final String ipAddress;
        public final String deviceName;
        public final boolean sshAvailable;

        public DetectedDevice(String ipAddress, String deviceName, boolean sshAvailable) {
            this.ipAddress = ipAddress;
            this.deviceName = deviceName;
            this.sshAvailable = sshAvailable;
        }
    }

    public interface NetworkScanCallback {
        void onDeviceFound(DetectedDevice device);

        void onScanComplete(List<DetectedDevice> devices);

        void onScanProgress(String status);

        void onError(String error);
    }

    /**
     * Generate wpa_supplicant.conf content for the given network
     */
    public static String generateWpaSupplicantConfig(String ssid, String password) {
        return "country=US\n" +
                "ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev\n" +
                "update_config=1\n" +
                "\n" +
                "network={\n" +
                "    ssid=\"" + ssid + "\"\n" +
                "    psk=\"" + password + "\"\n" +
                "    key_mgmt=WPA-PSK\n" +
                "}\n";
    }

    /**
     * Get current hotspot information
     */
    public static String getCurrentHotspotInfo(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if (wifiInfo != null && wifiInfo.getSSID() != null) {
                String ssid = wifiInfo.getSSID().replace("\"", "");
                String ipAddress = intToIp(wifiInfo.getIpAddress());
                return "SSID: " + ssid + "\nIP: " + ipAddress;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting hotspot info", e);
        }
        return "Not connected to WiFi";
    }

    /**
     * Scan the local network for Raspberry Pi devices
     */
    public static void scanForRaspberryPi(Context context, NetworkScanCallback callback) {
        ExecutorService executor = Executors.newFixedThreadPool(50);

        try {
            String networkBase = getNetworkBase(context);
            if (networkBase == null) {
                callback.onError("Not connected to WiFi network");
                return;
            }

            callback.onScanProgress("Scanning network " + networkBase + "...");

            List<Future<DetectedDevice>> futures = new ArrayList<>();
            List<DetectedDevice> foundDevices = new ArrayList<>();

            // Scan common Raspberry Pi IP ranges (usually .1 to .254)
            for (int i = 1; i <= 254; i++) {
                final String targetIP = networkBase + "." + i;

                Future<DetectedDevice> future = executor.submit(new Callable<DetectedDevice>() {
                    @Override
                    public DetectedDevice call() throws Exception {
                        return scanSingleIP(targetIP);
                    }
                });
                futures.add(future);
            }

            // Collect results
            for (Future<DetectedDevice> future : futures) {
                try {
                    DetectedDevice device = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    if (device != null) {
                        foundDevices.add(device);
                        callback.onDeviceFound(device);
                    }
                } catch (Exception e) {
                    // Ignore timeouts and connection failures
                }
            }

            callback.onScanComplete(foundDevices);

        } catch (Exception e) {
            callback.onError("Scan failed: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    private static DetectedDevice scanSingleIP(String ipAddress) {
        try {
            // First check if host is reachable
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ipAddress, SSH_PORT), TIMEOUT_MS);
            socket.close();

            // If SSH port is open, likely a Raspberry Pi
            String deviceName = "Raspberry Pi";

            // Try to get more specific device info if possible
            // (In a real implementation, you might try to connect and get hostname)

            return new DetectedDevice(ipAddress, deviceName, true);

        } catch (IOException e) {
            // Host not reachable or SSH not available
            return null;
        }
    }

    private static String getNetworkBase(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if (wifiInfo != null) {
                int ipAddress = wifiInfo.getIpAddress();
                String ip = intToIp(ipAddress);

                // Extract network base (e.g., "192.168.1" from "192.168.1.100")
                String[] parts = ip.split("\\.");
                if (parts.length >= 3) {
                    return parts[0] + "." + parts[1] + "." + parts[2];
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting network base", e);
        }
        return null;
    }

    private static String intToIp(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >> 24) & 0xFF);
    }

    /**
     * Test SSH connection to a specific IP
     */
    public static boolean testSSHConnection(String ipAddress) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ipAddress, SSH_PORT), TIMEOUT_MS * 3);
            socket.close();
            return true;
        } catch (IOException e) {
            Log.d(TAG, "SSH connection failed to " + ipAddress + ": " + e.getMessage());
            return false;
        }
    }
}
