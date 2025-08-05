package com.team10.realmail.utils;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;

public class UsbDebugHelper {
    private static final String TAG = "UsbDebugHelper";

    public static String getUsbDebugInfo(Context context) {
        StringBuilder debugInfo = new StringBuilder();

        try {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

            if (usbManager == null) {
                debugInfo.append("❌ USB Manager is null - USB not supported\n");
                return debugInfo.toString();
            }

            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            debugInfo.append("🔍 USB Debug Information:\n");
            debugInfo.append("═══════════════════════════════\n");
            debugInfo.append("Total USB devices: ").append(deviceList.size()).append("\n\n");

            if (deviceList.isEmpty()) {
                debugInfo.append("❌ No USB devices detected\n");
                debugInfo.append("📋 Troubleshooting checklist:\n");
                debugInfo.append("   • Is the USB cable a DATA cable (not just charging)?\n");
                debugInfo.append("   • Is the Pi powered on and booted?\n");
                debugInfo.append("   • Is USB gadget mode properly configured?\n");
                debugInfo.append("   • Try a different USB cable\n");
                return debugInfo.toString();
            }

            int deviceIndex = 1;
            for (UsbDevice device : deviceList.values()) {
                debugInfo.append("📱 Device #").append(deviceIndex).append(":\n");
                debugInfo.append("   Name: ").append(device.getDeviceName()).append("\n");
                debugInfo.append("   Vendor ID: 0x").append(String.format("%04X", device.getVendorId())).append("\n");
                debugInfo.append("   Product ID: 0x").append(String.format("%04X", device.getProductId())).append("\n");
                debugInfo.append("   Device Class: ").append(device.getDeviceClass()).append(" (").append(getDeviceClassName(device.getDeviceClass())).append(")\n");
                debugInfo.append("   Device Subclass: ").append(device.getDeviceSubclass()).append("\n");
                debugInfo.append("   Protocol: ").append(device.getDeviceProtocol()).append("\n");
                debugInfo.append("   Interface Count: ").append(device.getInterfaceCount()).append("\n");
                debugInfo.append("   Permission: ").append(usbManager.hasPermission(device) ? "✅ Granted" : "❌ Not granted").append("\n");

                // Check if this looks like a Raspberry Pi
                boolean isLikelyRaspberryPi = isLikelyRaspberryPi(device);
                debugInfo.append("   Raspberry Pi?: ").append(isLikelyRaspberryPi ? "✅ Likely" : "❓ Unknown").append("\n");

                if (isLikelyRaspberryPi) {
                    debugInfo.append("   🎯 This device looks like a Raspberry Pi!\n");
                }

                debugInfo.append("\n");
                deviceIndex++;
            }

            // Add specific Raspberry Pi troubleshooting
            debugInfo.append("🔧 Raspberry Pi USB Gadget Setup:\n");
            debugInfo.append("═══════════════════════════════════\n");
            debugInfo.append("Required files on SD card boot partition:\n");
            debugInfo.append("1. config.txt should contain:\n");
            debugInfo.append("   dtoverlay=dwc2\n\n");
            debugInfo.append("2. cmdline.txt should contain:\n");
            debugInfo.append("   modules-load=dwc2,g_serial\n");
            debugInfo.append("   (add after rootwait)\n\n");
            debugInfo.append("3. Create empty 'ssh' file (no extension)\n\n");

            debugInfo.append("💡 Expected Raspberry Pi USB identifiers:\n");
            debugInfo.append("   • Linux Foundation (1d6b:0104)\n");
            debugInfo.append("   • Linux USB Serial Gadget (0525:a4a7)\n");
            debugInfo.append("   • Device Class 2 (Communications)\n");

        } catch (Exception e) {
            debugInfo.append("❌ Error getting USB info: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Error getting USB debug info", e);
        }

        return debugInfo.toString();
    }

    private static boolean isLikelyRaspberryPi(UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();
        int deviceClass = device.getDeviceClass();

        // Check for common Raspberry Pi USB gadget identifiers
        return (vid == 0x1d6b && (pid == 0x0104 || pid == 0x0002)) ||  // Linux Foundation
                (vid == 0x0525 && (pid == 0xa4a7 || pid == 0xa4a6)) ||     // Linux USB Serial Gadget
                (deviceClass == 2) ||  // Communications device class
                (deviceClass == 9);    // Hub class (sometimes shows up)
    }

    private static String getDeviceClassName(int deviceClass) {
        switch (deviceClass) {
            case 0:
                return "Per Interface";
            case 1:
                return "Audio";
            case 2:
                return "Communications/CDC";
            case 3:
                return "HID";
            case 5:
                return "Physical";
            case 6:
                return "Still Imaging";
            case 7:
                return "Printer";
            case 8:
                return "Mass Storage";
            case 9:
                return "Hub";
            case 10:
                return "CDC Data";
            case 11:
                return "Smart Card";
            case 13:
                return "Content Security";
            case 14:
                return "Video";
            case 15:
                return "Personal Healthcare";
            case 16:
                return "Audio/Video";
            case 17:
                return "Billboard";
            case 18:
                return "USB Type-C Bridge";
            case 220:
                return "Diagnostic";
            case 224:
                return "Wireless Controller";
            case 239:
                return "Miscellaneous";
            case 254:
                return "Application Specific";
            case 255:
                return "Vendor Specific";
            default:
                return "Unknown (" + deviceClass + ")";
        }
    }
}
