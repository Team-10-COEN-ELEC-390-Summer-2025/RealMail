# Raspberry Pi Heartbeat Service

A dedicated service that continuously monitors and reports the online status of your Raspberry Pi device to your Firebase backend. This service sends periodic "heartbeat" signals containing device status and system information.

## 📋 Overview

The heartbeat service ensures your backend system knows when devices are online/offline by:
- Sending periodic status updates to your Firebase Cloud Functions
- Including system metrics (CPU temperature, uptime)
- Automatically restarting on failures
- Starting automatically on boot

## 🗂️ File Structure

```
heartbeat_service/
├── device_info.txt          # Device configuration file
├── heartbeat_service.py     # Main Python service
├── heartbeat.service        # Systemd service file
├── setup_heartbeat.sh       # Automated setup script
└── HEARTBEAT_README.md      # This documentation
```

## 📤 Data Sent to Backend

The service sends JSON data every 60 seconds (configurable) with this structure:

```json
{
  "device_id": "raspberrypi-001",
  "user_email": "user@example.com", 
  "timestamp": "2025-08-07T14:30:45.123456",
  "status": "online",
  "cpu_temp": 45.2,
  "uptime_seconds": 86400,
  "system_timestamp": "2025-08-07T14:30:45.123456"
}
```

**Field Descriptions:**
- `device_id`: Unique identifier from device_info.txt
- `user_email`: Associated user email from device_info.txt
- `timestamp`: When heartbeat was sent (ISO 8601 format)
- `status`: Always "online" (indicates device is active)
- `cpu_temp`: CPU temperature in Celsius (null if unavailable)
- `uptime_seconds`: System uptime in seconds (null if unavailable)
- `system_timestamp`: System info collection timestamp

## ⚙️ Configuration

Edit `device_info.txt` with your specific values:

```bash
# Device Configuration File
DEVICE_ID=your-unique-device-id
USER_EMAIL=your-user@email.com
HEARTBEAT_URL=https://your-region-your-project.cloudfunctions.net/handleDeviceHeartbeat
HEARTBEAT_INTERVAL=60
```

**Configuration Parameters:**
- `DEVICE_ID`: Unique identifier for your device (e.g., "kitchen-sensor-001")
- `USER_EMAIL`: Email address of the device owner
- `HEARTBEAT_URL`: Your Firebase Cloud Function endpoint URL
- `HEARTBEAT_INTERVAL`: Seconds between heartbeats (default: 60)

## 🚀 Setup Instructions

### Step 1: Transfer Files to Raspberry Pi

Copy the entire `heartbeat_service` folder to your Raspberry Pi:

```bash
# Using SCP (replace with your Pi's IP)
scp -r ./heartbeat_service team10@your-pi-ip:/home/team10/RealMail/hardware/

# Or using rsync
rsync -av ./heartbeat_service/ team10@your-pi-ip:/home/team10/RealMail/hardware/heartbeat_service/
```

### Step 2: Configure Device Settings

On the Raspberry Pi:

```bash
cd /home/team10/RealMail/hardware/heartbeat_service
nano device_info.txt
```

Update with your actual values:
```
DEVICE_ID=my-raspi-kitchen-001
USER_EMAIL=myemail@gmail.com
HEARTBEAT_URL=https://us-central1-yourproject.cloudfunctions.net/handleDeviceHeartbeat
HEARTBEAT_INTERVAL=60
```

### Step 3: Run Automated Setup

**Important:** Run as the `team10` user, not as root:

```bash
# Switch to team10 user if you're root
su - team10
cd /home/team10/RealMail/hardware/heartbeat_service

# Make setup script executable (if needed)
chmod +x setup_heartbeat.sh

# Run the setup
./setup_heartbeat.sh
```

The setup script will:
- ✅ Install Python dependencies (`requests`)
- ✅ Create log files with proper permissions
- ✅ Install and enable systemd service
- ✅ Configure auto-start on boot
- ✅ Start the service immediately

### Step 4: Verify Installation

Check if the service is running:

```bash
# Check service status
sudo systemctl status heartbeat.service

# View live logs
sudo journalctl -u heartbeat.service -f

# Check log file
tail -f /var/log/heartbeat_service.log
```

## 🔧 Service Management

### Common Commands

```bash
# View service status
sudo systemctl status heartbeat.service

# Start service
sudo systemctl start heartbeat.service

# Stop service
sudo systemctl stop heartbeat.service

# Restart service (after config changes)
sudo systemctl restart heartbeat.service

# Enable auto-start on boot
sudo systemctl enable heartbeat.service

# Disable auto-start on boot
sudo systemctl disable heartbeat.service
```

### Log Files

```bash
# View systemd service logs (live)
sudo journalctl -u heartbeat.service -f

# View systemd service logs (last 50 lines)
sudo journalctl -u heartbeat.service -n 50

# View Python script log file
tail -f /var/log/heartbeat_service.log

# View Python script log file (last 100 lines)
tail -n 100 /var/log/heartbeat_service.log
```

## 🔥 Firebase Cloud Function

Add this function to your Firebase Cloud Functions:

```typescript
/**
 * Handles incoming heartbeat data from Raspberry Pi devices.
 * Stores device status and system information in the database.
 */
export const handleDeviceHeartbeat = onRequest(async (req, res) => {
    logger.info("Received device heartbeat", {method: req.method, url: req.url});
    
    if (req.method !== "POST") {
        res.status(405).send("Method Not Allowed");
        logger.warn("Method not allowed", {method: req.method});
        return;
    }

    const heartbeatData = req.body;

    // Validate required fields
    if (!heartbeatData || !heartbeatData.device_id || !heartbeatData.user_email || 
        !heartbeatData.timestamp || !heartbeatData.status) {
        res.status(400).send("Bad Request: Missing required heartbeat data fields");
        logger.error("Invalid heartbeat data", {data: heartbeatData});
        return;
    }

    try {
        // Store heartbeat data in device_logs table
        await pool.query(`
            INSERT INTO device_logs (
                device_id, 
                user_email, 
                timestamp, 
                status, 
                cpu_temp, 
                uptime_seconds, 
                system_timestamp
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7)
        `, [
            heartbeatData.device_id,
            heartbeatData.user_email,
            heartbeatData.timestamp,
            heartbeatData.status,
            heartbeatData.cpu_temp || null,
            heartbeatData.uptime_seconds || null,
            heartbeatData.system_timestamp || null
        ]);

        logger.info("Heartbeat data stored successfully", {
            device_id: heartbeatData.device_id,
            user_email: heartbeatData.user_email,
            status: heartbeatData.status
        });

        res.status(200).send("Heartbeat received and stored successfully");

    } catch (error) {
        logger.error("Database insertion failed for heartbeat", {
            error: error instanceof Error ? error.message : String(error),
            data: heartbeatData
        });
        res.status(500).send("Internal Server Error: Failed to store heartbeat data");
    }
});
```

## 🛠️ Troubleshooting

### Service Won't Start

```bash
# Check service status for error details
sudo systemctl status heartbeat.service

# View detailed logs
sudo journalctl -u heartbeat.service -n 50

# Check if Python script has correct permissions
ls -la /home/team10/RealMail/hardware/heartbeat_service/heartbeat_service.py

# Test Python script manually
cd /home/team10/RealMail/hardware/heartbeat_service
python3 heartbeat_service.py
```

### Configuration Issues

```bash
# Verify configuration file exists and is readable
cat /home/team10/RealMail/hardware/heartbeat_service/device_info.txt

# Check for syntax errors in config
grep -v '^#' device_info.txt | grep '='
```

### Network Issues

```bash
# Test network connectivity
ping google.com

# Test if your Firebase URL is reachable
curl -X POST your-firebase-url -H "Content-Type: application/json" -d '{"test": true}'
```

### Dependency Issues

```bash
# Reinstall Python requests library
pip3 install --upgrade requests

# Check Python version
python3 --version
```

## 📊 Monitoring Device Status

Your backend can determine device status by:

1. **Online**: Recent heartbeat within the last 2-3 minutes
2. **Offline**: No heartbeat received for more than 3-5 minutes
3. **System Health**: Monitor CPU temperature and uptime trends

Example query to check online devices:
```sql
SELECT device_id, user_email, MAX(timestamp) as last_seen
FROM device_logs 
WHERE timestamp > NOW() - INTERVAL '3 minutes'
GROUP BY device_id, user_email;
```

## 🔄 Updating Configuration

After changing `device_info.txt`:

```bash
# Restart the service to pick up new configuration
sudo systemctl restart heartbeat.service

# Verify new settings are loaded
sudo journalctl -u heartbeat.service -n 10
```

## ⚡ Auto-Start on Boot

The service is automatically configured to start on boot. To verify:

```bash
# Check if service is enabled
sudo systemctl is-enabled heartbeat.service

# Should return: enabled
```

## 📝 Notes

- The service runs as the `team10` user for security
- Logs are written to `/var/log/heartbeat_service.log`
- Service will automatically restart if it crashes
- Network failures are handled gracefully with retry logic
- All timestamps are in ISO 8601 format with timezone information
