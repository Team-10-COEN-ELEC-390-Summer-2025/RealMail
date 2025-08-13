# Raspberry Pi Camera Streaming Setup Guide

## Installation on Raspberry Pi

### 1. System Dependencies
```bash
sudo apt update
sudo apt install python3-pip python3-opencv python3-numpy
sudo apt install libcamera-apps libcamera-dev
```

### 2. Python Dependencies
```bash
cd /path/to/your/pi/camera/folder
pip install -r requirements.txt
```

### 3. Enable Camera
```bash
sudo raspi-config
# Navigate to Interfacing Options > Camera > Enable
sudo reboot
```

## Usage

### Start Pi Camera Streaming
```bash
python3 PICameraStream.py --server https://your-server-url:8443 --device-id pi-camera-living-room
```

### Command Line Options
- `--server`: WebRTC server URL (default: https://localhost:8443)
- `--device-id`: Unique identifier for this camera (required)
- `--resolution`: Camera resolution like 1280x720 (default: 1280x720)
- `--fps`: Frames per second (default: 30)

### Examples
```bash
# Basic usage
python3 PICameraStream.py --device-id kitchen-cam

# High resolution
python3 PICameraStream.py --device-id security-cam --resolution 1920x1080 --fps 25

# Connect to remote server
python3 PICameraStream.py --device-id outdoor-cam --server https://myserver.com:8443
```

## Viewing the Stream

### On Web Browser
- Go to: `https://your-server-url:8443/android/pi-camera-living-room`
- Or: `https://your-server-url:8443/viewer/pi-camera-living-room`

## Troubleshooting

### Camera Not Found
```bash
# Test camera
libcamera-hello --timeout 5000

# Check camera connection
vcgencmd get_camera
```

### Permission Issues
```bash
# Add user to video group
sudo usermod -a -G video $USER
```

### Network Issues
- Ensure Pi and viewer are on same network
- Check firewall settings
- Verify server URL is accessible
