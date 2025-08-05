package com.team10.realmail.utils;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

public class UsbPiHelper {
    private static final String TAG = "UsbPiHelper";
    private static final String ACTION_USB_PERMISSION = "com.team10.realmail.USB_PERMISSION";
    private static final int BAUD_RATE = 115200;

    private Context context;
    private UsbManager usbManager;
    private UsbSerialPort serialPort;
    private SerialInputOutputManager serialIoManager;

    public interface UsbConnectionCallback {
        void onConnected();

        void onDisconnected();

        void onError(String error);

        void onDataReceived(String data);

        void onFileTransferProgress(int progress);

        void onFileTransferComplete(boolean success);
    }

    private UsbConnectionCallback callback;

    public UsbPiHelper(Context context) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    public void setCallback(UsbConnectionCallback callback) {
        this.callback = callback;
    }

    /**
     * Find and connect to Raspberry Pi via USB serial
     */
    public void connectToRaspberryPi() {
        Log.d(TAG, "Starting USB connection process...");

        // Check if USB service is available
        if (usbManager == null) {
            Log.e(TAG, "USB Manager is null");
            if (callback != null) callback.onError("USB service not available");
            return;
        }

        // List all USB devices first for debugging
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Log.d(TAG, "Found " + deviceList.size() + " USB devices");

        for (UsbDevice device : deviceList.values()) {
            Log.d(TAG, "USB Device: " + device.getDeviceName() +
                    " VID: " + Integer.toHexString(device.getVendorId()) +
                    " PID: " + Integer.toHexString(device.getProductId()) +
                    " Class: " + device.getDeviceClass() +
                    " Subclass: " + device.getDeviceSubclass());
        }

        // Find all available drivers
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        Log.d(TAG, "Found " + availableDrivers.size() + " serial drivers");

        if (availableDrivers.isEmpty()) {
            // Try with custom prober for Raspberry Pi
            UsbSerialProber customProber = new UsbSerialProber(getCustomProbeTable());
            availableDrivers = customProber.findAllDrivers(usbManager);
            Log.d(TAG, "Found " + availableDrivers.size() + " drivers with custom prober");
        }

        if (availableDrivers.isEmpty()) {
            String deviceInfo = "No USB serial devices found.\n";
            deviceInfo += "Connected USB devices: " + deviceList.size() + "\n";
            for (UsbDevice device : deviceList.values()) {
                deviceInfo += "- " + device.getDeviceName() + " (VID:" +
                        Integer.toHexString(device.getVendorId()) + " PID:" +
                        Integer.toHexString(device.getProductId()) + ")\n";
            }

            if (callback != null) callback.onError(deviceInfo);
            return;
        }

        // Use the first available driver (should be the Raspberry Pi)
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();

        Log.d(TAG, "Using driver: " + driver.getClass().getSimpleName() +
                " for device: " + device.getDeviceName());

        // Check if we have permission
        if (!usbManager.hasPermission(device)) {
            Log.d(TAG, "No permission for device, requesting...");
            requestUsbPermission(device);
            return;
        }

        // Connect to the device
        connectToDevice(driver);
    }

    /**
     * Custom probe table for Raspberry Pi Zero USB gadget mode
     */
    private ProbeTable getCustomProbeTable() {
        ProbeTable customTable = new ProbeTable();

        // Raspberry Pi Foundation VID
        customTable.addProduct(0x1d6b, 0x0104, CdcAcmSerialDriver.class); // Standard Linux gadget
        customTable.addProduct(0x1d6b, 0x0002, CdcAcmSerialDriver.class);  // USB 2.0 root hub
        customTable.addProduct(0x0525, 0xa4a7, CdcAcmSerialDriver.class);  // Linux-USB Serial Gadget (with CDC ACM)
        customTable.addProduct(0x0525, 0xa4a6, CdcAcmSerialDriver.class);  // Linux-USB Serial Gadget

        // Try FTDI driver as well
        customTable.addProduct(0x1d6b, 0x0104, FtdiSerialDriver.class);
        customTable.addProduct(0x0525, 0xa4a7, FtdiSerialDriver.class);

        return customTable;
    }

    private void requestUsbPermission(UsbDevice device) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);

        // Register receiver for permission result
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(usbPermissionReceiver, filter);

        usbManager.requestPermission(device, permissionIntent);
    }

    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // Permission granted, find the driver and connect
                            List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
                            for (UsbSerialDriver driver : drivers) {
                                if (driver.getDevice().equals(device)) {
                                    connectToDevice(driver);
                                    break;
                                }
                            }
                        }
                    } else {
                        if (callback != null) callback.onError("USB permission denied");
                    }
                }
                context.unregisterReceiver(this);
            }
        }
    };

    private void connectToDevice(UsbSerialDriver driver) {
        try {
            UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
            if (connection == null) {
                if (callback != null) callback.onError("Failed to open USB connection");
                return;
            }

            serialPort = driver.getPorts().get(0);
            serialPort.open(connection);
            serialPort.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            // Set up serial I/O manager
            serialIoManager = new SerialInputOutputManager(serialPort, new SerialInputOutputManager.Listener() {
                @Override
                public void onNewData(byte[] data) {
                    String receivedData = new String(data);
                    Log.d(TAG, "Received: " + receivedData);
                    if (callback != null) callback.onDataReceived(receivedData);
                }

                @Override
                public void onRunError(Exception e) {
                    Log.e(TAG, "Serial error", e);
                    if (callback != null)
                        callback.onError("Serial communication error: " + e.getMessage());
                }
            });

            Executors.newSingleThreadExecutor().submit(serialIoManager);

            if (callback != null) callback.onConnected();
            Log.d(TAG, "Connected to Raspberry Pi via USB");

        } catch (IOException e) {
            Log.e(TAG, "Error connecting to device", e);
            if (callback != null) callback.onError("Connection failed: " + e.getMessage());
        }
    }

    /**
     * Send a command to the Raspberry Pi
     */
    public void sendCommand(String command) {
        if (serialPort == null) {
            if (callback != null) callback.onError("No USB connection");
            return;
        }

        try {
            byte[] data = (command + "\n").getBytes();
            serialPort.write(data, 1000);
            Log.d(TAG, "Sent command: " + command);
        } catch (IOException e) {
            Log.e(TAG, "Error sending command", e);
            if (callback != null) callback.onError("Failed to send command: " + e.getMessage());
        }
    }

    /**
     * Transfer wpa_supplicant.conf file to Raspberry Pi
     */
    public void transferWpaSupplicantConfig(String ssid, String password) {
        if (serialPort == null) {
            if (callback != null) callback.onError("No USB connection");
            return;
        }

        if (callback != null) callback.onFileTransferProgress(10);

        // Generate the wpa_supplicant.conf content
        String wpaConfig = generateWpaSupplicantConfig(ssid, password);

        try {
            if (callback != null) callback.onFileTransferProgress(25);

            // Create the file on the Pi
            sendCommand("sudo rm -f /boot/wpa_supplicant.conf");
            Thread.sleep(500);

            if (callback != null) callback.onFileTransferProgress(50);

            // Write the config content using cat with heredoc
            String createFileCommand = "sudo cat > /boot/wpa_supplicant.conf << 'EOF'\n" + wpaConfig + "\nEOF";
            sendCommand(createFileCommand);
            Thread.sleep(1000);

            if (callback != null) callback.onFileTransferProgress(75);

            // Set proper permissions
            sendCommand("sudo chmod 600 /boot/wpa_supplicant.conf");
            Thread.sleep(500);

            // Also create SSH file if it doesn't exist
            sendCommand("sudo touch /boot/ssh");
            Thread.sleep(500);

            if (callback != null) callback.onFileTransferProgress(100);

            Log.d(TAG, "WiFi config transferred successfully");
            if (callback != null) callback.onFileTransferComplete(true);

        } catch (Exception e) {
            Log.e(TAG, "Error transferring WiFi config", e);
            if (callback != null) {
                callback.onFileTransferComplete(false);
                callback.onError("File transfer failed: " + e.getMessage());
            }
        }
    }

    /**
     * Reboot the Raspberry Pi
     */
    public void rebootRaspberryPi() {
        if (serialPort == null) {
            if (callback != null) callback.onError("No USB connection");
            return;
        }

        Log.d(TAG, "Rebooting Raspberry Pi...");
        sendCommand("sudo reboot");

        // Close connection as Pi will restart
        disconnect();
    }

    private String generateWpaSupplicantConfig(String ssid, String password) {
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
     * Test if Raspberry Pi is connected and responsive
     */
    public void testConnection() {
        if (serialPort == null) {
            if (callback != null) callback.onError("No USB connection");
            return;
        }

        // Send a simple command to test connectivity
        sendCommand("echo 'USB_CONNECTION_TEST'");
    }

    /**
     * Disconnect from the Raspberry Pi
     */
    public void disconnect() {
        if (serialIoManager != null) {
            serialIoManager.stop();
            serialIoManager = null;
        }

        if (serialPort != null) {
            try {
                serialPort.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing serial port", e);
            }
            serialPort = null;
        }

        if (callback != null) callback.onDisconnected();
        Log.d(TAG, "Disconnected from Raspberry Pi");
    }
}
