# RealMail Backend API Documentation

> [!WARNING]  
> Documentation last updated on August 12th, 2025.

This backend provides Firebase Cloud Functions for the RealMail smart mailbox system, handling sensor data ingestion, user authentication, device management, push notifications, device status monitoring, real-time streaming, and visual status indicators. All HTTP endpoints expect JSON payloads unless otherwise noted.

## System Architecture

- **Platform:** Firebase Cloud Functions (Node.js 22)
- **Database:** PostgreSQL with connection pooling
- **Authentication:** Firebase Admin SDK
- **Push Notifications:** Firebase Cloud Messaging (FCM)
- **Scheduled Tasks:** Firebase Functions v2 Scheduler (runs every 2 minutes)
- **Real-time Status Monitoring:** Device heartbeat tracking with visual indicators
- **Video Streaming:** Real-time camera streaming from IoT devices
- **Dependencies:** TypeScript, node-fetch, socket.io, cors, dotenv

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
    "motion_detected": "boolean",
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: "Sensor data received and stored successfully"
  - `400 Bad Request`: Invalid sensor data
  - `405 Method Not Allowed`: Non-POST method
  - `500 Internal Server Error`: Database or notification failure
- **Database:** Inserts into `sensors_data` table
- **Side Effects:** Automatically triggers push notification to user's mobile app

---

### 3. `/verifyToken`
**JWT authentication token verification**

- **Method:** `POST`
- **Purpose:** Verifies Firebase JWT tokens and stores user authentication data
- **Request Body/Query Parameters:**
  ```json
  {
    "token": "string (Firebase JWT token)",
    "uid": "string (Firebase user UID)"
  }
  ```
- **Response:**
  - `200 OK`: "JWT token received and saved successfully"
  - `400 Bad Request`: Missing token or UID
  - `405 Method Not Allowed`: Non-POST method
  - `500 Internal Server Error`: Authentication failure
- **Database:** Upserts into `firebase_auth` table
- **Notes:** Uses Firebase Admin SDK for user verification

---

### 4. `/updateSensorStatus`
**Device online/offline status updates**

- **Method:** `POST`
- **Purpose:** Records device connectivity status for monitoring
- **Request Body:**
  ```json
  {
    "device_id": "string",
    "status": "string ('online' or 'offline')",
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: "Sensor status checked and notifications sent successfully"
  - `400 Bad Request`: Invalid status data
  - `405 Method Not Allowed`: Non-POST method
  - `500 Internal Server Error`: Database insertion failure
- **Database:** Inserts into `sensors_online_activity` table with Eastern Time timestamp

---

### 5. `/getDeviceRegistrationToken`
**FCM token registration for push notifications**

- **Method:** `POST`
- **Purpose:** Stores Firebase Cloud Messaging tokens for push notifications
- **Request Body/Query Parameters:**
  ```json
  {
    "email": "string (user email)",
    "token": "string (FCM registration token)"
  }
  ```
- **Response:**
  - `200 OK`: "Device registration token saved successfully"
  - `400 Bad Request`: Missing email or token
  - `405 Method Not Allowed`: Non-POST method
  - `500 Internal Server Error`: Database save failure
- **Database:** Updates `firebase_auth` table with upsert operation

---

### 6. `/newDataNotification`
**Push notification sender**

- **Method:** `POST`
- **Purpose:** Sends push notifications to mobile app when new sensor data is received
- **Request Body:**
  ```json
  {
    "device_id": "string",
    "timeStamp": "ISO8601 string",
    "motion_detected": "boolean",
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: "Notification sent successfully"
  - `404 Not Found`: No FCM token found for user
  - `500 Internal Server Error`: Messaging service failure
- **Features:** 
  - Formats timestamps for Eastern Time display
  - Includes device data in notification payload
  - Uses Firebase Cloud Messaging

---

### 7. `/getSensorsWithMotionDetected`
**Historical motion detection data**

- **Method:** `POST`
- **Purpose:** Retrieves all sensor records with motion detected for a specific user
- **Request Body:**
  ```json
  {
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: Array of sensor records with motion detected (ordered by timestamp DESC)
  - `400 Bad Request`: Missing user email
  - `405 Method Not Allowed`: Non-POST method
  - `500 Internal Server Error`: Database query failure
- **Database:** Queries `sensors_data` table

---

### 8. `/addNewDevice`
**Device registration**

- **Method:** `POST`
- **Purpose:** Registers a new IoT device for a user
- **Request Body:**
  ```json
  {
    "device_id": "string (unique identifier)",
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: "Device added successfully"
  - `400 Bad Request`: Missing device_id or user_email
  - `405 Method Not Allowed`: Non-POST method
  - `500 Internal Server Error`: Database insertion failure
- **Database:** Inserts into `sensors_data` table

---

### 9. `/removeDevice`
**Device removal**

- **Method:** `POST`
- **Purpose:** Removes a device from user's account
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
  - `405 Method Not Allowed`: Non-POST method
  - `500 Internal Server Error`: Database deletion failure
- **Database:** Deletes from `sensors_data` table

---

### 10. `/getAllDevicesForUser`
**User's device list**

- **Method:** `POST`
- **Purpose:** Retrieves all devices associated with a user
- **Request Body:**
  ```json
  {
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: Array of device objects with `device_id`
  - `400 Bad Request`: Missing user_email
  - `405 Method Not Allowed`: Non-POST method
  - `500 Internal Server Error`: Database query failure
- **Database:** Queries distinct device_ids from `sensors_data` table

---

### 11. `/handleDeviceLogs`
**Device system health logging**

- **Method:** `POST`
- **Purpose:** Receives and stores system health data from Raspberry Pi devices
- **Request Body:**
  ```json
  {
    "device_id": "string",
    "user_email": "string",
    "timestamp": "ISO8601 string",
    "status": "string",
    "system_info": {
      "cpu_temp": "number (optional)",
      "uptime_seconds": "number (optional)",
      "timestamp": "ISO8601 string (optional)"
    }
  }
  ```
- **Response:**
  - `200 OK`: "Device log data received and stored successfully"
  - `400 Bad Request`: Missing required fields
  - `405 Method Not Allowed`: Non-POST method
  - `500 Internal Server Error`: Database insertion failure
- **Database:** Inserts into `device_logs` table
- **Features:** Tracks CPU temperature, uptime, and system timestamps

---

### 12. `/getDeviceStatusIndicators`
**Real-time device status dashboard**

- **Method:** `POST`
- **Purpose:** Provides comprehensive device status with visual indicators for dashboard display
- **Request Body:**
  ```json
  {
    "user_email": "string"
  }
  ```
- **Response:**
  - `200 OK`: Detailed status response with summary and device array
  - `400 Bad Request`: Missing user_email
  - `405 Method Not Allowed`: Non-POST method
  - `500 Internal Server Error`: Database query failure

**Response Format:**
```json
{
  "user_email": "string",
  "summary": {
    "total_devices": "number",
    "online": "number",
    "warning": "number", 
    "offline": "number",
    "last_updated": "ISO8601 string"
  },
  "devices": [
    {
      "device_id": "string",
      "connection_status": "string ('online'|'warning'|'offline')",
      "visual_indicator": "string ('green'|'yellow'|'red')",
      "last_seen": "ISO8601 string",
      "minutes_since_last_seen": "number",
      "cpu_temp": "number",
      "uptime_seconds": "number", 
      "raw_status": "string",
      "health_info": {
        "is_healthy": "boolean",
        "last_heartbeat": "ISO8601 string",
        "uptime_hours": "number"
      }
    }
  ]
}
```

**Status Logic:**
- **Online (Green)**: Last seen within 3 minutes
- **Warning (Yellow)**: Last seen 3-5 minutes ago  
- **Offline (Red)**: Last seen over 5 minutes ago

---

### 13. `/startStreaming/{device_id}`
**Start camera streaming**

- **Method:** `POST`
- **Purpose:** Initiates real-time camera streaming from a specific IoT device
- **URL Parameters:** `device_id` in path
- **Headers:** CORS enabled for cross-origin requests
- **Response:**
  - `200 OK`: Streaming started successfully
  - `400 Bad Request`: Missing device ID
  - `404 Not Found`: Device not found
  - `502 Bad Gateway`: Device streaming service failure
  - `500 Internal Server Error`: Server error
- **Features:**
  - Looks up device IP from database
  - Calls device's streaming API at port 8080
  - 10-second timeout for device communication
- **Database:** Queries `devices` table for IP address

---

### 14. `/stopStreaming/{device_id}`
**Stop camera streaming**

- **Method:** `POST`
- **Purpose:** Stops camera streaming for a specific device
- **URL Parameters:** `device_id` in path
- **Headers:** CORS enabled for cross-origin requests
- **Response:**
  - `200 OK`: Streaming stopped successfully
  - `400 Bad Request`: Missing device ID or IP
  - `404 Not Found`: Device not found
  - `502 Bad Gateway`: Device streaming service failure
  - `500 Internal Server Error`: Server error
- **Features:** Similar to start streaming but calls stop endpoint

---

### 15. `/getStreamingStatus/{device_id}`
**Check streaming status**

- **Method:** `GET`
- **Purpose:** Retrieves current streaming status for a device
- **URL Parameters:** `device_id` in path
- **Headers:** CORS enabled for cross-origin requests
- **Response:**
  - `200 OK`: Status retrieved (even if device offline)
  - `400 Bad Request`: Missing device ID

**Response Format:**
```json
{
  "success": true,
  "device_id": "string",
  "device_ip": "string",
  "streaming": "boolean",
  "camera_active": "boolean",
  "error": "string (if applicable)"
}
```

- **Features:**
  - 5-second timeout for status check
  - Graceful handling of offline devices
  - Returns status even when device unreachable

---

## Scheduled Functions

### `checkDeviceStatusIndicators`
**Automatic device health monitoring**

- **Schedule:** Runs every 2 minutes (`*/2 * * * *`)
- **Timezone:** America/New_York
- **Purpose:** Monitors device connectivity and health status
- **Function:** 
  - Queries latest device logs
  - Classifies devices as online/warning/offline
  - Processes status indicators for dashboard updates
- **Database:** Queries `device_logs` table with complex status classification logic

---

## Database Schema

The API interacts with the following PostgreSQL tables:

- **`sensors_data`**: Stores motion detection events and device-user relationships
- **`firebase_auth`**: Manages user authentication and FCM tokens  
- **`sensors_online_activity`**: Tracks device online/offline status changes
- **`device_logs`**: Stores system health data (CPU temp, uptime, etc.)
- **`devices`**: Contains device configuration and IP addresses for streaming

---

## Security & Performance

- **Connection Pooling:** PostgreSQL connection pool for efficient database access
- **Error Handling:** Comprehensive error logging and user-friendly responses
- **Timezone Support:** All timestamps use America/New_York timezone
- **CORS Support:** Cross-origin requests enabled for streaming endpoints
- **Timeout Management:** Configurable timeouts for external API calls
- **Input Validation:** Strict validation of all request parameters
- **Firebase Integration:** Uses Firebase Admin SDK for secure authentication

---

## Deployment

- **Runtime:** Node.js 22
- **Build Process:** TypeScript compilation with ESLint
- **Commands:**
  - `npm run build`: Compile TypeScript and run linting
  - `npm run deploy`: Deploy to Firebase Functions
  - `npm run serve`: Local development with Firebase emulators

---
