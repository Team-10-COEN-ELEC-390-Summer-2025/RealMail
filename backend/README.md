> [!WARNING]  
> Documentation below isLast updated on July 15th 2025. 


# RealMail Backend API Documentation

This backend provides Firebase Cloud Functions for RealMail, handling authentication, sensor data, device registration, and push notifications. All endpoints are HTTP-triggered and expect JSON payloads unless otherwise noted.

---

## Endpoints

### 1. `/handleFirebaseJWT`
- **Method:** `ANY`
- **Purpose:** Dummy endpoint to test HTTP connectivity.
- **Request:** Any request (body optional)
- **Response:**
  - `200 OK` with message: `"Hello from Firebase JWT handler!"`
- **Notes:**
  - Logs request method, URL, and body (if present).

---

### 2. `/handleSensorIncomingData`
- **Method:** `POST`
- **Purpose:** Receives sensor data from devices, validates, stores in DB, and triggers notification.
- **Request Body:**
  ```json
  {
    "device_id": "string",
    "timeStamp": "ISO8601 string",
    "motion_detected": true,
    "user_email": "string"
  }
  ```
- **Responses:**
  - `200 OK` on success
  - `400 Bad Request` if data is missing/invalid
  - `405 Method Not Allowed` if not POST
  - `500 Internal Server Error` on DB or notification failure
- **Notes:**
  - Stores data in `public.sensors_data` table.
  - Triggers `/newDataNotification` after storing data.

---

### 3. `/verifyToken`
- **Method:** `POST`
- **Purpose:** Receives and verifies a Firebase JWT and user UID, saves to DB.
- **Request Body or Query:**
  - `token`: JWT string
  - `uid`: Firebase user UID
- **Responses:**
  - `200 OK` on success
  - `400 Bad Request` if missing token or UID
  - `405 Method Not Allowed` if not POST
  - `500 Internal Server Error` on DB or verification failure
- **Notes:**
  - Updates or inserts into `public.firebase_auth` table.

---

### 4. `/getDeviceRegistrationToken`
- **Method:** `POST`
- **Purpose:** Receives and stores a device registration token for push notifications.
- **Request Body or Query:**
  - `email`: User email
  - `token`: Device registration token
- **Responses:**
  - `200 OK` on success
  - `400 Bad Request` if missing email or token
  - `405 Method Not Allowed` if not POST
  - `500 Internal Server Error` on DB failure
- **Notes:**
  - Updates or inserts into `public.firebase_auth` table.

---

### 5. `/newDataNotification`
- **Method:** `POST`
- **Purpose:** Sends a push notification to the user's device when new sensor data is received.
- **Request Body:**
  ```json
  {
    "device_id": "string",
    "timeStamp": "ISO8601 string",
    "motion_detected": true,
    "user_email": "string"
  }
  ```
- **Responses:**
  - `200 OK` if notification sent
  - `404 Not Found` if device registration token not found
  - `500 Internal Server Error` on DB or notification failure
- **Notes:**
  - Looks up device registration token in `public.firebase_auth`.
  - Uses Firebase Cloud Messaging to send notification.

---

## General Notes
- All endpoints expect `Content-Type: application/json` for POST requests.
- All errors are logged using Firebase logger.
- Database: PostgreSQL, tables `public.sensors_data` and `public.firebase_auth`.
- Environment variables required for DB and Firebase setup.

---

For more details, see the code in `functions/src/index.ts`.
