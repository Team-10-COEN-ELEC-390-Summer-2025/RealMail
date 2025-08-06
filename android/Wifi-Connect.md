# WiFi Connect Documentation

## Overview

This document outlines the complete WiFi setup and device configuration process for the RealMail
Android app, including Raspberry Pi setup, network configuration, and device registration.

## Architecture

### Components

- **Android App**: Device setup UI and network scanning
- **Raspberry Pi**: Mail sensor device with SSH access
- **Firebase Backend**: Device registration via `/addNewDevice` API
- **SSH Helper**: Automated Pi configuration via SSH commands

## Setup Process Flow

### 1. Initial Raspberry Pi Configuration

#### Prerequisites on Raspberry Pi

```bash
# Enable SSH on Raspberry Pi
sudo systemctl enable ssh
sudo systemctl start ssh

# Default credentials used in app
Username: team10
Password: poop
```

#### WPA Supplicant Base Configuration

```bash
# File: /etc/wpa_supplicant/wpa_supplicant.conf
ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
update_config=1
country=US

# Setup network for initial configuration
network={
    ssid="setmeup123"
    psk="setmeup123"
    key_mgmt=WPA-PSK
    priority=100
}
```

### 2. Android App Setup Flow

#### Step 1: Device Name Input

- User enters a unique device name
- This becomes the `device_id` in the API call
- Validation: minimum 3 characters

#### Step 2: Network Detection & Connection Methods

##### Option A: USB Setup (Recommended)

- Direct USB connection between phone and Pi
- No network configuration needed
- Most reliable method

##### Option B: WiFi Setup

Three sub-methods:

1. **Same WiFi Network**: Pi and phone on same home WiFi
2. **Mobile Hotspot**: Create hotspot `setmeup123/setmeup123`
3. **Ethernet**: Pi connected via ethernet, phone on WiFi

#### Step 3: Pi Discovery & SSH Connection

The app scans for Raspberry Pi devices and tests SSH connectivity:

```java
// Smart network detection logic
if(currentSSID !=null&&!currentSSID.

equals(SETUP_HOTSPOT_SSID)){
        // Pi already on home network - skip to final step
        // Show dialog: "Pi Already Configured!"
        }else{
        // Pi on setup network - proceed to WiFi configuration
        }
```

#### Step 4: WiFi Configuration (if needed)

If Pi is on setup network, configure home WiFi via SSH:

```bash
# Commands executed via SSH on Raspberry Pi
# Test network connectivity first
nmcli dev wifi connect "$SSID" password "$PASSWORD" ifname wlan0

# Add network to wpa_supplicant with priority 100
sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf << EOF
network={
    ssid="$HOME_WIFI_SSID"
    psk="$HOME_WIFI_PASSWORD"
    key_mgmt=WPA-PSK
    priority=100
}
EOF

# Restart networking to apply changes
sudo systemctl restart dhcpcd
sudo wpa_cli -i wlan0 reconfigure
```

#### Step 5: Final Registration

- Scan for Pi on home network
- Register device with backend API

## SSH Helper Implementation

### Key Methods

```java
public class SSHHelper {
    // Test SSH connection to Pi
    public void testConnection(String ipAddress, SSHCallback callback)

    // Add WiFi network with connectivity test
    public void addWiFiNetworkWithTest(String piIP, String ssid, String password, SSHCallback callback)
}
```

### SSH Commands Used

```bash
# Network connectivity test
nmcli dev wifi connect "$SSID" password "$PASSWORD" ifname wlan0

# Add WiFi network to wpa_supplicant
echo "network={" | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf
echo "    ssid=\"$SSID\"" | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf
echo "    psk=\"$PASSWORD\"" | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf
echo "    key_mgmt=WPA-PSK" | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf
echo "    priority=100" | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf
echo "}" | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf

# Restart networking services
sudo systemctl restart dhcpcd
sudo wpa_cli -i wlan0 reconfigure
```

## API Integration

### Device Registration Endpoint

```javascript
// Firebase Cloud Function: /addNewDevice
export const addNewDevice = onRequest(async (req, res) => {
    const {device_id, user_email} = req.body;
    // device_id: User-provided device name
    // user_email: Firebase authenticated user email
    
    await pool.query(`INSERT INTO sensors_data (device_id, linked_user_email)
                      VALUES ($1, $2)`, [device_id, user_email]);
});
```

### Android API Call

```java
// In DeviceSetupActivity.java - finishSetup() method
DeviceRequest request = new DeviceRequest(userEmail, deviceName);
Call<Void> call = deviceApi.addNewDevice(request);
```

### Request Structure

```json
{
  "user_email": "user@example.com",
  "device_id": "MyPiDevice"
}
```

## Network Configuration Details

### Priority System

Networks are configured with priority levels:

- **Setup Network** (`setmeup123`): Priority 100
- **Home Networks**: Priority 100 (highest priority for auto-connection)

### Example Final Configuration

```bash
# /etc/wpa_supplicant/wpa_supplicant.conf
ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
update_config=1
country=US

network={
    ssid="setmeup123"
    psk="setmeup123"
    key_mgmt=WPA-PSK
    priority=100
}

network={
    ssid="HomeWiFi"
    psk="HomePassword123"
    key_mgmt=WPA-PSK
    priority=100
}
```

## Smart Detection Features

### Intelligent Flow Control

1. **Network Detection**: App detects current network when scanning for Pi
2. **Smart Skipping**: If Pi found on home network, skip WiFi configuration
3. **User Feedback**: Clear dialogs explaining why steps are skipped
4. **Error Recovery**: Fallback options if setup fails

### Benefits

- Faster setup for pre-configured devices
- Reduced user confusion
- Better error handling
- Support for multiple setup scenarios

## Troubleshooting

### Common Issues

1. **Pi Not Found**: Check SSH is enabled and Pi is powered on
2. **SSH Connection Failed**: Verify credentials (team10/poop)
3. **WiFi Configuration Failed**: Check network credentials and signal strength
4. **API Registration Failed**: Verify internet connection and Firebase auth

### Debug Commands on Raspberry Pi

```bash
# Check WiFi status
sudo wpa_cli -i wlan0 status

# View available networks
sudo wpa_cli -i wlan0 scan_results

# Check network configuration
cat /etc/wpa_supplicant/wpa_supplicant.conf

# Restart networking
sudo systemctl restart dhcpcd
sudo systemctl restart wpa_supplicant

# Check IP address
ip addr show wlan0
```

### Log Locations

- **Android Logs**: Use `Log.d("DeviceSetup", "message")` for debugging
- **Pi System Logs**: `/var/log/syslog` and `journalctl -u wpa_supplicant`

## Security Considerations

### Default Credentials

- SSH username: `team10`
- SSH password: `poop`
- **Note**: Change default credentials in production deployment

### Network Security

- All WiFi networks use WPA-PSK encryption
- SSH connections use standard authentication
- API calls use Firebase authentication

## Development Notes

### Key Classes

- `DeviceSetupActivity.java`: Main setup flow UI
- `SSHHelper.java`: SSH command execution
- `NetworkScanner.java`: Pi discovery on network
- `DeviceApi.java`: API interface definitions
- `DeviceRequest.java`: API request model

### Constants Used

```java
private static final String SETUP_HOTSPOT_SSID = "setmeup123";
private static final String BASE_URL = "https://us-central1-realmail-39ab4.cloudfunctions.net/";
```

### Permissions Required

```xml

<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /><uses-permission
android:name="android.permission.ACCESS_COARSE_LOCATION" /><uses-permission
android:name="android.permission.ACCESS_WIFI_STATE" /><uses-permission
android:name="android.permission.CHANGE_WIFI_STATE" /><uses-permission
android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Future Improvements

### Potential Enhancements

1. **QR Code Setup**: Generate QR codes for WiFi configuration
2. **Bulk Device Setup**: Support multiple Pi devices
3. **Network Profiles**: Save and reuse network configurations
4. **Advanced Diagnostics**: More detailed network troubleshooting
5. **Bluetooth Setup**: Alternative to WiFi for initial configuration

### Performance Optimizations

1. **Parallel Scanning**: Scan multiple IP ranges simultaneously
2. **Connection Caching**: Remember successful Pi connections
3. **Timeout Optimization**: Adjust timeouts based on network conditions

## Conclusion

This implementation provides a robust, user-friendly device setup flow that handles multiple
connection methods, automatically detects optimal configuration paths, and provides clear feedback
throughout the process. The combination of smart network detection, SSH automation, and proper error
handling makes the setup process reliable across various network environments.

