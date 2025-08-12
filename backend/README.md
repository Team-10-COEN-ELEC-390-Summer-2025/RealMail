# RealMail Backend API Documentation

> [!WARNING]  
> Documentation last updated on August 7th, 2025.

This backend provides Firebase Cloud Functions for the RealMail smart mailbox system, handling sensor data ingestion, user authentication, device management, push notifications, device status monitoring, and visual status indicators. All HTTP endpoints expect JSON payloads unless otherwise noted.

## System Architecture

- **Platform:** Firebase Cloud Functions (Node.js)
- **Database:** PostgreSQL with connection pooling
- **Authentication:** Firebase Admin SDK
- **Push Notifications:** Firebase Cloud Messaging (FCM)
- **Scheduled Tasks:** Firebase Functions v2 Scheduler
- **Real-time Status Monitoring:** Device heartbeat tracking with visual indicators

---

## HTTP Endpoints

### 1. `/handleFirebaseJWT`
**Test endpoint for connectivity verification**

- **Method:** `ANY`
- **Purpose:** Dummy endpoint to verify HTTP connectivity and Firebase Functions deployment
- **Request:** Any HTTP method, optional body
- **Response:**
  - `200 OK` with message: `"Hello from Firebase JWT handler!"`
- **Notes:**
  - Includes 1-second simulated processing delay
  - Logs all request details for debugging

---

### 2. `/handleSensorIncomingData`
**Primary sensor data ingestion endpoint**

- **Method:** `POST`
- **Purpose:** Receives sensor data from IoT devices, validates, stores in database, and triggers notifications
- **Request Body:**
  ```json
  {
    "device_id": "string",
    "timeStamp": "ISO8601 string (e.g., '2025-08-07T14:30:00.000Z')",
    "motion_detected": boolean,
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: "Sensor data received and stored successfully"
  - `400 Bad Request`: Invalid sensor data format
  - `500 Internal Server Error`: Database insertion failed
- **Database Table:** `sensors_data`
- **Side Effects:** Triggers push notification to user's mobile app

---

### 3. `/handleDeviceLogs`
**🆕 Device heartbeat and system information logging endpoint**

- **Method:** `POST`
- **Purpose:** Receives device status updates including system health information from Raspberry Pi devices
- **Request Body:**
  ```json
  {
    "device_id": "team10_board",
    "user_email": "alice01@example.com", 
    "timestamp": "2025-08-07T12:00:00Z",
    "status": "online",
    "system_info": {
      "cpu_temp": 45.5,
      "uptime_seconds": 3600,
      "timestamp": "2025-08-07T12:00:00Z"
    }
  }
  ```
- **Response:**
  - `200 OK`: "Device log data received and stored successfully"
  - `400 Bad Request`: Missing required fields (device_id, user_email, timestamp, status)
  - `500 Internal Server Error`: Database error
- **Database Table:** `device_logs`
- **Required Fields:** `device_id`, `user_email`, `timestamp`, `status`
- **Optional Fields:** `system_info.cpu_temp`, `system_info.uptime_seconds`, `system_info.timestamp`

---

### 4. `/getDeviceStatusIndicators`
**🆕 Real-time device status with visual indicators**

- **Method:** `POST`
- **Purpose:** Retrieves current status of all user devices with visual indicators for mobile app UI
- **Request Body:**
  ```json
  {
    "user_email": "alice01@example.com"
  }
  ```
- **Response:**
  ```json
  {
    "user_email": "alice01@example.com",
    "summary": {
      "total_devices": 3,
      "online": 2,
      "warning": 1,
      "offline": 0,
      "last_updated": "2025-08-07T12:00:00.000Z"
    },
    "devices": [
      {
        "device_id": "team10_board",
        "connection_status": "online",
        "visual_indicator": "green",
        "last_seen": "2025-08-07T11:58:00.000Z",
        "minutes_since_last_seen": 2.0,
        "cpu_temp": 45.5,
        "uptime_seconds": 3600,
        "raw_status": "online",
        "health_info": {
          "is_healthy": true,
          "last_heartbeat": "2025-08-07T11:58:00.000Z",
          "uptime_hours": 1
        }
      }
    ]
  }
  ```

**Visual Indicators:**
- 🟢 **Green dot/icon** = Online (last heartbeat < 3 minutes ago)
- 🟡 **Yellow dot/icon** = Warning (heartbeat 3-5 minutes ago)
- 🔴 **Red dot/icon** = Offline (no heartbeat for > 5 minutes)

---

### 5. `/verifyToken`
**Firebase JWT authentication verification**

- **Method:** `POST`
- **Purpose:** Verifies Firebase JWT token and stores user authentication details
- **Request Body:**
  ```json
  {
    "token": "firebase_jwt_token_string",
    "uid": "firebase_user_uid"
  }
  ```
- **Response:**
  - `200 OK`: "JWT token received and saved successfully"
  - `400 Bad Request`: Missing JWT token or user UID
  - `500 Internal Server Error`: Authentication or database error
- **Database Table:** `firebase_auth`

---

### 6. `/updateSensorStatus`
**Device online/offline status updates**

- **Method:** `POST`
- **Purpose:** Updates device online/offline status in the database
- **Request Body:**
  ```json
  {
    "device_id": "string",
    "status": "online|offline",
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: "Sensor status checked and notifications sent successfully"
  - `400 Bad Request`: Invalid sensor status data
  - `500 Internal Server Error`: Database insertion failed
- **Database Table:** `sensors_online_activity`

---

### 7. `/getDeviceRegistrationToken`
**Device registration for push notifications**

- **Method:** `POST`
- **Purpose:** Stores Firebase Cloud Messaging (FCM) registration token for push notifications
- **Request Body:**
  ```json
  {
    "email": "user@example.com",
    "token": "fcm_registration_token"
  }
  ```
- **Response:**
  - `200 OK`: "Device registration token saved successfully"
  - `400 Bad Request`: Missing user email or token
  - `500 Internal Server Error`: Database error
- **Database Table:** `firebase_auth`

---

### 8. `/newDataNotification`
**Push notification sender**

- **Method:** `POST`
- **Purpose:** Sends push notifications to users when new sensor data is received
- **Request Body:**
  ```json
  {
    "device_id": "string",
    "timeStamp": "ISO8601 string",
    "motion_detected": boolean,
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: "Notification sent successfully"
  - `404 Not Found`: No device registration token found
  - `500 Internal Server Error`: Notification sending failed

---

### 9. `/getSensorsWithMotionDetected`
**Motion detection history retrieval**

- **Method:** `POST`
- **Purpose:** Retrieves all sensors with motion detected for a specific user
- **Request Body:**
  ```json
  {
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: Array of sensor records with motion detected
  - `400 Bad Request`: Missing user email
  - `500 Internal Server Error`: Database query failed

---

### 10. `/addNewDevice`
**Device registration**

- **Method:** `POST`
- **Purpose:** Adds a new device to the system for a user
- **Request Body:**
  ```json
  {
    "device_id": "string",
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: "Device added successfully"
  - `400 Bad Request`: Missing device_id or user_email
  - `500 Internal Server Error`: Database insertion failed

---

### 11. `/removeDevice`
**Device deregistration**

- **Method:** `POST`
- **Purpose:** Removes a device from the system
- **Request Body:**
  ```json
  {
    "device_id": "string",
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: "Device removed successfully"
  - `400 Bad Request`: Missing device_id or user_email
  - `500 Internal Server Error`: Database deletion failed

---

### 12. `/getAllDevicesForUser`
**User device listing**

- **Method:** `POST`
- **Purpose:** Retrieves all devices associated with a user
- **Request Body:**
  ```json
  {
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: Array of device objects
  - `400 Bad Request`: Missing user_email
  - `500 Internal Server Error`: Database query failed

---

## Scheduled Functions

### `/checkDeviceStatusIndicators`
**🆕 Automated device status monitoring**

- **Schedule:** Every 2 minutes (`*/2 * * * *`)
- **Time Zone:** America/Toronto
- **Purpose:** Monitors all device heartbeats and sends status updates to mobile apps
- **Process:**
  1. Queries latest device logs from all users
  2. Classifies devices as online/warning/offline based on last heartbeat
  3. Groups devices by user
  4. Sends status payload to each user's mobile app
  5. Logs activity for monitoring

**Status Classification Logic:**
- **Online:** Last heartbeat within 3 minutes
- **Warning:** Last heartbeat 3-5 minutes ago  
- **Offline:** No heartbeat for more than 5 minutes

---

## Database Schema

### `device_logs` Table
```sql
CREATE TABLE device_logs (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL,
    cpu_temp DECIMAL(5,2),
    uptime_seconds BIGINT,
    system_timestamp TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_device_timestamp ON device_logs (device_id, timestamp);
CREATE INDEX idx_user_email ON device_logs (user_email);
```

### Other Tables
- `sensors_data`: Motion sensor readings
- `firebase_auth`: User authentication and FCM tokens
- `sensors_online_activity`: Device status history

---

```

### Environment Variables
- `DB_USER`: Database username
- `DB_HOST`: Database host IP
- `DB_NAME`: Database name
- `DB_PASSWORD`: Database password
- `SERVER_URL`: Firebase Functions base URL

---

## Mobile App Integration

### Reading Device Status API

**To get real-time device status with visual indicators:**

```javascript
// JavaScript/React Native Example
const getDeviceStatus = async (userEmail) => {
  try {
    const response = await fetch('https://us-central1-realmail-39ab4.cloudfunctions.net/getDeviceStatusIndicators', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        user_email: userEmail
      })
    });
    
    const data = await response.json();
    
    // Use data.summary for overview stats
    console.log(`Total devices: ${data.summary.total_devices}`);
    console.log(`Online: ${data.summary.online}, Warning: ${data.summary.warning}, Offline: ${data.summary.offline}`);
    
    // Use data.devices for individual device status
    data.devices.forEach(device => {
      console.log(`${device.device_id}: ${device.visual_indicator} (${device.connection_status})`);
      // Display green/yellow/red indicator based on device.visual_indicator
    });
    
  } catch (error) {
    console.error('Error fetching device status:', error);
  }
};
```

**For Raspberry Pi devices sending heartbeat:**

```python
# Python example for Raspberry Pi
import requests
import json
from datetime import datetime
import psutil

def send_device_heartbeat(device_id, user_email):
    # Get system information
    cpu_temp = get_cpu_temperature()  # Your implementation
    uptime_seconds = int(psutil.boot_time())
    
    payload = {
        "device_id": device_id,
        "user_email": user_email,
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "status": "online",
        "system_info": {
            "cpu_temp": cpu_temp,
            "uptime_seconds": uptime_seconds,
            "timestamp": datetime.utcnow().isoformat() + "Z"
        }
    }
    
    response = requests.post(
        'https://us-central1-realmail-39ab4.cloudfunctions.net/handleDeviceLogs',
        headers={'Content-Type': 'application/json'},
        json=payload
    )
    
    return response.status_code == 200
```

---

## Error Handling

All endpoints return appropriate HTTP status codes:

- **200 OK**: Request successful
- **400 Bad Request**: Invalid request data or missing required fields
- **404 Not Found**: Resource not found (e.g., no registration token)
- **405 Method Not Allowed**: Incorrect HTTP method used
- **500 Internal Server Error**: Server-side error (database, authentication, etc.)

Error responses include descriptive messages to aid in debugging.

---

## Monitoring and Logging

All functions use Firebase Functions Logger with structured logging:
- Request/response logging
- Error tracking with stack traces
- Performance metrics
- Database query logging
- Scheduled function execution tracking

Use Firebase Console to monitor function execution, errors, and performance metrics.

---

## Deployment

```bash
# Install dependencies
cd functions
npm install

# Deploy all functions
firebase deploy --only functions

# Deploy specific function
firebase deploy --only functions:handleDeviceLogs
```
