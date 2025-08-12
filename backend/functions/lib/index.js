"use strict";
/**
 * Import function triggers from their respective submodules:
 *
 * import {onCall} from "firebase-functions/v2/https";
 * import {onDocumentWritten} from "firebase-functions/v2/firestore";
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.getStreamingStatus = exports.stopStreaming = exports.startStreaming = exports.checkDeviceStatusIndicators = exports.getDeviceStatusIndicators = exports.handleDeviceLogs = exports.getAllDevicesForUser = exports.removeDevice = exports.addNewDevice = exports.getSensorsWithMotionDetected = exports.newDataNotification = exports.getDeviceRegistrationToken = exports.updateSensorStatus = exports.verifyToken = exports.handleSensorIncomingData = exports.handleFirebaseJWT = void 0;
const https_1 = require("firebase-functions/https");
const logger = __importStar(require("firebase-functions/logger"));
const admin = __importStar(require("firebase-admin"));
const auth_1 = require("firebase-admin/auth");
const messaging_1 = require("firebase-admin/messaging");
require("dotenv/config");
const pg_1 = require("pg");
const scheduler_1 = require("firebase-functions/v2/scheduler");
const node_fetch_1 = __importDefault(require("node-fetch"));
console.log("Creating new database connection pool.");
console.log("DB_NAME:", process.env.DB_NAME); // Add this for debugging
const pool = new pg_1.Pool({
    user: process.env.DB_USER,
    host: process.env.DB_HOST,
    database: process.env.DB_NAME,
    password: process.env.DB_PASSWORD,
    port: 5432,
});
admin.initializeApp();
// https://firebase.google.com/docs/functions/tips
/*
onInit( async () => {
    // init pool
    pool = new Pool({
        user: process.env.DB_USER,
        host: process.env.DB_HOST,
        database: process.env.DB_NAME,
        password: process.env.DB_PASSWORD,
        port: 5432,
    });
    try {
        const result = await pool.query("SELECT NOW()"); // test the connection
        console.log("Database connected successfully:", result.rows[0]);
    } catch (error) {
        console.error("Database connection failed:", error);
        throw new Error("Database connection failed");
    }
});
*/
// a dummy function to test the http
exports.handleFirebaseJWT = (0, https_1.onRequest)((req, res) => {
    logger.info("Received request", { method: req.method, url: req.url });
    // Simulate some processing
    setTimeout(() => {
        res.status(200).send("Hello from Firebase JWT handler!");
        logger.info("Response sent successfully");
    }, 1000);
    // Log the request body if present
    if (req.body) {
        logger.debug("Request body:", req.body);
    }
});
/**
 * Handles incoming sensor data from devices.
 * Validates the request and stores the data in the database.
 *
 * @param req - The HTTP request object
 * @param res - The HTTP response object
 */
exports.handleSensorIncomingData = (0, https_1.onRequest)(async (req, res) => {
    logger.info("Received sensor data", { method: req.method, url: req.url });
    if (req.method !== "POST") {
        res.status(405).send("Method Not Allowed");
        logger.warn("Method not allowed", { method: req.method });
        return;
    }
    const sensorData = req.body;
    /* sensordata schema is below
        device_id: string
        timeStamp: iso 8601 string
        motion_detected: boolean
        user_email: string
        */
    if (!sensorData || !sensorData.device_id || !sensorData.timeStamp || typeof sensorData.motion_detected !== "boolean" || !sensorData.user_email) {
        res.status(400).send("Bad Request: Invalid sensor data");
        logger.error("Invalid sensor data", { data: sensorData });
        return;
    }
    // store data in the database.
    try {
        await pool.query(`INSERT INTO public.sensors_data (device_id, timestamp, motion_detected, linked_user_email)
                          VALUES ($1, $2, $3,
                                  $4)`, [sensorData.device_id, sensorData.timeStamp, sensorData.motion_detected, sensorData.user_email,]);
    }
    catch (error) {
        logger.error("Database insertion failed", { data: sensorData });
        res.status(500).send("Internal Server Error: Failed to store sensor data");
        return;
    }
    // once sensor data is stored, send notification to the app
    // try {
    //     const response = await fetch(process.env.SERVER_URL + "/newDataNotification", {
    //         method: "POST",
    //         headers: { "Content-Type": "application/json" },
    //         body: JSON.stringify({
    //             device_id: sensorData.device_id,
    //             timeStamp: sensorData.timeStamp,
    //             motion_detected: sensorData.motion_detected,
    //             user_email: sensorData.user_email,
    //         })
    //     });
    //     const data = await response.json();
    //     console.log("Notification response:", data);
    //
    // } catch (error) {
    //     logger.error("Error sending notification", { error: error instanceof Error ? error.message : String(error) });
    //     res.status(500).send("Internal Server Error: Failed to send notification");
    //     return;
    // }
    sendNotificationToDevice({
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({
            device_id: sensorData.device_id,
            timeStamp: sensorData.timeStamp,
            motion_detected: sensorData.motion_detected,
            user_email: sensorData.user_email,
        })
    }).then(r => {
        // do nothing
    });
    // return success response
    res.status(200).send("Sensor data received and stored successfully");
});
/**
 * Formats a date to a human-readable string
 * @param date - The date to format
 * @returns A formatted date string like "August 7, 2025 at 6:21 PM"
 */
function formatDateForNotification(date) {
    const options = {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
        timeZone: 'America/New_York', // Set the timezone to Eastern Time
        timeZoneName: 'short'
    };
    return date.toLocaleDateString('en-US', options);
}
async function sendNotificationToDevice(options) {
    // once sensor data is stored, send notification to the app
    try {
        const response = await (0, node_fetch_1.default)(process.env.SERVER_URL + "/newDataNotification", Object.assign(Object.assign({}, options), { body: options.body || undefined }));
        const data = await response.json();
        console.log("Notification response:", data);
    }
    catch (error) {
        logger.error("Error sending notification", { error: error instanceof Error ? error.message : String(error) });
    }
    return;
}
// handle JWT authentication token from Android app
// https://firebase.google.com/docs/auth/admin/manage-users
// https://firebase.google.com/docs/cloud-messaging/send-message
exports.verifyToken = (0, https_1.onRequest)(async (req, res) => {
    logger.info("Received JWT authentication request", { method: req.method, url: req.url });
    const jwt_token = req.query.token || req.body.token;
    const user_uid = req.query.uid || req.body.uid;
    if (req.method !== "POST") {
        logger.warn("Method not allowed", { method: req.method });
        res.status(405).send("Method Not Allowed");
        return;
    }
    if (!jwt_token) {
        logger.error("Missing JWT token in request");
        res.status(400).send("Bad Request: Missing JWT token");
        return;
    }
    if (!user_uid) {
        logger.error("Missing user UID in request");
        res.status(400).send("Bad Request: Missing user UID");
        return;
    }
    try {
        const userRecord = await (0, auth_1.getAuth)().getUser(user_uid);
        // Corrected ON CONFLICT to use (user_uid) instead of (user_id)
        await pool.query(`INSERT INTO public.firebase_auth (user_uid, firebase_auth_email, jwt)
                          VALUES ($1, $2, $3) ON CONFLICT (user_uid) DO
        UPDATE
            SET firebase_auth_email = EXCLUDED.firebase_auth_email, jwt = EXCLUDED.jwt`, [userRecord.uid, userRecord.email, jwt_token]);
        res.status(200).send("JWT token received and saved successfully");
    }
    catch (error) {
        // Log the actual error and send a response to prevent timeout
        if (error instanceof Error) {
            logger.error("Unable to save authentication details in the db", { error: error.message, stack: error.stack });
        }
        else {
            logger.error("Unable to save authentication details in the db", { error: String(error) });
        }
        res.status(500).send("Internal Server Error");
        return;
    }
});
// API below retrieve signal from sensor about online status and store in the database.
exports.updateSensorStatus = (0, https_1.onRequest)(async (req, res) => {
    logger.info("Received request to check sensor status", { method: req.method, url: req.url });
    if (req.method !== "POST") {
        logger.warn("Method not allowed", { method: req.method });
        res.status(405).send("Method Not Allowed");
        return;
    }
    const sensorStatus = req.body; // sensorStatus schema is below
    /*
        "device_id": string,
        "status": string, // "online" or "offline"
        "user_email": string
     */
    if (!sensorStatus || !sensorStatus.device_id || !sensorStatus.status || !sensorStatus.user_email) {
        logger.error("Invalid sensor status data", { data: sensorStatus });
        res.status(400).send("Bad Request: Invalid sensor status data");
        return;
    }
    // insert the sensor status into the database
    try {
        await pool.query(`INSERT INTO sensors_online_activity (device_id, status, user_email, last_activity)
                          VALUES ($1, $2, $3,
                                  NOW() AT TIME ZONE 'America/New_York')`, [sensorStatus.device_id, sensorStatus.status, sensorStatus.user_email]);
    }
    catch (error) {
        logger.error("Database insertion failed", { error: error instanceof Error ? error.message : String(error) });
        res.status(500).send("Internal Server Error: Failed to insert sensor status into the database");
        return;
    }
    res.status(200).send("Sensor status checked and notifications sent successfully");
});
// API to get device registration token from the app and store it in the database
exports.getDeviceRegistrationToken = (0, https_1.onRequest)(async (req, res) => {
    logger.info("Received request for device registration token", { method: req.method, url: req.url });
    if (req.method !== "POST") {
        logger.warn("Method not allowed", { method: req.method });
        res.status(405).send("Method Not Allowed");
        return;
    }
    const user_email = req.query.email || req.body.email;
    const deviceRegistrationToken = req.query.token || req.body.token;
    if (!user_email) {
        logger.error("Missing user email in request");
        res.status(400).send("Bad Request: Missing user email");
        return;
    }
    if (!deviceRegistrationToken) {
        logger.error("Missing device registration token in request");
        res.status(400).send("Bad Request: Missing device registration token");
        return;
    }
    // save the device registration token in the database
    try {
        await pool.query(`WITH updated AS (
        UPDATE public.firebase_auth
        SET device_registration_token = $2
        WHERE firebase_auth_email = $1 RETURNING *
        )
        INSERT
        INTO public.firebase_auth (firebase_auth_email, device_registration_token)
        SELECT $1,
               $2 WHERE NOT EXISTS (SELECT 1 FROM updated)`, [user_email, deviceRegistrationToken]);
        logger.info("Device registration token saved successfully", { user_email });
        res.status(200).send("Device registration token saved successfully");
        return;
    }
    catch (error) {
        logger.error("Error saving device registration token", { error: error instanceof Error ? error.message : String(error) });
        res.status(500).send("Internal Server Error while saving device registration token");
        return;
    }
});
// api for sending push notifications to the user
// see https://firebase.google.com/docs/cloud-messaging/server
exports.newDataNotification = (0, https_1.onRequest)(async (req, res) => {
    // when a new data is received from the sensor, this function will be called
    // function will send a push notification to the user(android app)
    /*
    based on user's email, look into the database and get the device registration token
    if device registration token is not found, return 404
    */
    const sensorData = req.body; // sensorData schema is below
    /*
        "device_id":
        "timeStamp":
        "motion_detected":
        "user_email":
     */
    // look for user's device registration token in the database
    let deviceRegistrationToken;
    try {
        const result = await pool.query(`SELECT device_registration_token
                                         FROM public.firebase_auth
                                         WHERE firebase_auth_email = $1`, [sensorData.user_email]);
        if (result.rows.length === 0) {
            logger.warn("No device registration token found for user", { user_email: sensorData.user_email });
            res.status(404).send("Not Found: No device registration token found for this user");
            return;
        }
        if (!result.rows[0].device_registration_token) {
            logger.warn("Device registration token is empty for user", { user_email: sensorData.user_email });
            res.status(404).send("Not Found: Device registration token is empty");
            return;
        }
        if (result.rows.length > 1) {
            logger.warn("Multiple device registration tokens found for user", { user_email: sensorData.user_email });
            res.status(500).send("Internal Server Error: Multiple device registration tokens found");
            return;
        }
        deviceRegistrationToken = result.rows[0].device_registration_token;
    }
    catch (error) {
        logger.error("Error fetching device registration token", { error: error instanceof Error ? error.message : String(error) });
        res.status(500).send("Internal Server Error while fetching device registration token");
        return;
    }
    // compose json message to send to the device
    const dateOfSensorData = new Date(sensorData.timeStamp);
    let formattedDate;
    if (isNaN(dateOfSensorData.getTime())) {
        formattedDate = formatDateForNotification(new Date()); // if the date is invalid, use current date
    }
    else {
        formattedDate = formatDateForNotification(dateOfSensorData); // format the date nicely
    }
    const message = {
        notification: {
            title: "New Mail Alert", body: "New mail is delivered in your mailbox on " + formattedDate,
        }, data: {
            deviceId: String(req.body.device_id),
            timestamp: String(req.body.timeStamp),
            motionDetected: String(req.body.motion_detected),
            userEmail: String(req.body.user_email),
        }, // @ts-ignore
        token: deviceRegistrationToken,
    };
    // send message to device.
    try {
        const response = await (0, messaging_1.getMessaging)().send(message);
        logger.info("Successfully sent message:", response);
    }
    catch (error) {
        logger.error("Error sending message:", error);
        res.status(500).send("Failed to send notification");
        return;
    }
    res.status(200).send("Notification sent successfully");
});
// API below when called it will need user_email then returns all sensors where motion_detected is true
exports.getSensorsWithMotionDetected = (0, https_1.onRequest)(async (req, res) => {
    logger.info("Received request to get sensors with motion detected", { method: req.method, url: req.url });
    if (req.method !== "POST") {
        logger.warn("Method not allowed", { method: req.method });
        res.status(405).send("Method Not Allowed");
        return;
    }
    const { user_email } = req.body;
    if (!user_email) {
        logger.error("Missing user's email  in request");
        res.status(400).send("Bad Request: Missing device_id or user_email");
        return;
    }
    try {
        const result = await pool.query(`SELECT *
                                         FROM sensors_data
                                         WHERE linked_user_email = $1
                                           AND motion_detected = true
                                         ORDER BY timestamp DESC`, [user_email]);
        res.status(200).json(result.rows);
    }
    catch (error) {
        logger.error("Error fetching sensors with motion detected", { error: error instanceof Error ? error.message : String(error) });
        res.status(500).send("Internal Server Error while fetching sensors with motion detected");
    }
});
// the API below adds a new device to the database, tables = sensors_data
exports.addNewDevice = (0, https_1.onRequest)(async (req, res) => {
    logger.info("Received request to add new device", { method: req.method, url: req.url });
    if (req.method !== "POST") {
        logger.warn("Method not allowed", { method: req.method });
        res.status(405).send("Method Not Allowed");
        return;
    }
    /* data expected is
        * device_id: string, // unique identifier for the device
        * user_email: string // email of the user who owns the device
     */
    const { device_id, user_email } = req.body;
    if (!device_id || !user_email) {
        logger.error("Missing device_id or user_email in request");
        res.status(400).send("Bad Request: Missing device_id or user_email");
        return;
    }
    try {
        await pool.query(`INSERT INTO sensors_data (device_id, linked_user_email)
                          VALUES ($1, $2)`, [device_id, user_email]);
        res.status(200).send("Device added successfully");
    }
    catch (error) {
        logger.error("Error adding new device", { error: error instanceof Error ? error.message : String(error) });
        res.status(500).send("Internal Server Error. Unable to add new device " + device_id + " for this user " + user_email);
    }
});
// API to remove a device from the database
exports.removeDevice = (0, https_1.onRequest)(async (req, res) => {
    logger.info("Received request to remove device", { method: req.method, url: req.url });
    if (req.method !== "POST") {
        logger.warn("Method not allowed", { method: req.method });
        res.status(405).send("Method Not Allowed");
        return;
    }
    /* data expected is
        * device_id: string, // unique identifier for the device
        * user_email: string // email of the user who owns the device
     */
    const { device_id, user_email } = req.body;
    if (!device_id || !user_email) {
        logger.error("Missing device_id or user_email in request");
        res.status(400).send("Bad Request: Missing device_id or user_email");
        return;
    }
    try {
        await pool.query(`DELETE
                          FROM sensors_data
                          WHERE device_id = $1
                            AND linked_user_email = $2`, [device_id, user_email]);
        res.status(200).send("Device removed successfully");
    }
    catch (error) {
        logger.error("Error removing device", { error: error instanceof Error ? error.message : String(error) });
        res.status(500).send("Internal Server Error. Unable to remove device " + device_id + " for this user " + user_email);
    }
});
// API to get all devices for a user
exports.getAllDevicesForUser = (0, https_1.onRequest)(async (req, res) => {
    logger.info("Received request to get all devices for user", { method: req.method, url: req.url });
    if (req.method !== "POST") {
        logger.warn("Method not allowed", { method: req.method });
        res.status(405).send("Method Not Allowed");
        return;
    }
    const { user_email } = req.body;
    if (!user_email) {
        logger.error("Missing user_email in request");
        res.status(400).send("Bad Request: Missing user_email");
        return;
    }
    try {
        const result = await pool.query(`
            SELECT DISTINCT device_id
            FROM sensors_data
            WHERE linked_user_email = $1`, [user_email]);
        res.status(200).json(result.rows);
    }
    catch (error) {
        logger.error("Error fetching devices for user", { error: error instanceof Error ? error.message : String(error) });
        res.status(500).send("Internal Server Error while fetching devices for user");
    }
});
// API to handle device log data from Raspberry Pi devices
exports.handleDeviceLogs = (0, https_1.onRequest)(async (req, res) => {
    logger.info("Received device log request", { method: req.method, url: req.url });
    // Only accept POST requests
    if (req.method !== "POST") {
        logger.warn("Method not allowed", { method: req.method });
        res.status(405).send("Method Not Allowed");
        return;
    }
    try {
        const { device_id, user_email, timestamp, status, system_info } = req.body;
        // Validate required fields
        if (!device_id || !user_email || !timestamp || !status) {
            logger.error("Missing required fields in device log request", { data: req.body });
            res.status(400).send("Bad Request: Missing required fields: device_id, user_email, timestamp, status");
            return;
        }
        // Extract system_info fields
        const cpu_temp = (system_info === null || system_info === void 0 ? void 0 : system_info.cpu_temp) || null;
        const uptime_seconds = (system_info === null || system_info === void 0 ? void 0 : system_info.uptime_seconds) || null;
        const system_timestamp = (system_info === null || system_info === void 0 ? void 0 : system_info.timestamp) || null;
        // Convert timestamp strings to Date objects for database insertion
        const deviceTimestamp = new Date(timestamp);
        const systemTimestampDate = system_timestamp ? new Date(system_timestamp) : null;
        // Insert device log data into database
        await pool.query(`
            INSERT INTO device_logs (device_id, user_email, timestamp, status, cpu_temp, uptime_seconds,
                                     system_timestamp)
            VALUES ($1, $2, $3, $4, $5, $6,
                    $7)`, [device_id, user_email, deviceTimestamp, status, cpu_temp, uptime_seconds, systemTimestampDate]);
        logger.info("Device log data stored successfully", {
            device_id, user_email, status, timestamp: deviceTimestamp.toISOString()
        });
        res.status(200).send("Device log data received and stored successfully");
    }
    catch (error) {
        logger.error("Error handling device log data", {
            error: error instanceof Error ? error.message : String(error), data: req.body
        });
        res.status(500).send("Internal Server Error while processing device log data");
    }
});
// API to get current device status indicators for a user
exports.getDeviceStatusIndicators = (0, https_1.onRequest)(async (req, res) => {
    logger.info("Received request for device status indicators", { method: req.method, url: req.url });
    if (req.method !== "POST") {
        logger.warn("Method not allowed", { method: req.method });
        res.status(405).send("Method Not Allowed");
        return;
    }
    try {
        const { user_email } = req.body;
        if (!user_email) {
            logger.error("Missing user_email in device status request");
            res.status(400).send("Bad Request: Missing user_email");
            return;
        }
        // Query to get latest device status with visual indicators
        const statusQuery = await pool.query(`
            WITH latest_logs AS (SELECT DISTINCT
            ON (device_id)
                device_id,
                user_email,
                timestamp,
                status,
                cpu_temp,
                uptime_seconds,
                created_at
            FROM device_logs
            WHERE user_email = $1
            ORDER BY device_id, timestamp DESC
                ),
                status_with_indicators AS (
            SELECT
                device_id, user_email, timestamp, status, cpu_temp, uptime_seconds, created_at, CASE
                WHEN timestamp > (NOW() AT TIME ZONE 'America/New_York') - INTERVAL '3 minutes' THEN 'online'
                WHEN timestamp > (NOW() AT TIME ZONE 'America/New_York') - INTERVAL '5 minutes' THEN 'warning'
                ELSE 'offline'
                END as connection_status, CASE
                WHEN timestamp > (NOW() AT TIME ZONE 'America/New_York') - INTERVAL '3 minutes' THEN 'green'
                WHEN timestamp > (NOW() AT TIME ZONE 'America/New_York') - INTERVAL '5 minutes' THEN 'yellow'
                ELSE 'red'
                END as visual_indicator, EXTRACT (EPOCH FROM ((NOW() AT TIME ZONE 'America/New_York') - timestamp))/60 as minutes_since_last_seen, EXTRACT (EPOCH FROM (NOW() - timestamp)) as seconds_since_last_seen
            FROM latest_logs
                )
            SELECT device_id,
                   connection_status,
                   visual_indicator,
                   ROUND(minutes_since_last_seen::numeric, 1) as minutes_since_last_seen,
                   seconds_since_last_seen, timestamp as last_seen, status as raw_status, cpu_temp, uptime_seconds, created_at
            FROM status_with_indicators
            ORDER BY device_id
        `, [user_email]);
        const devices = statusQuery.rows;
        // Create summary statistics
        const summary = {
            total_devices: devices.length,
            online: devices.filter(d => d.connection_status === 'online').length,
            warning: devices.filter(d => d.connection_status === 'warning').length,
            offline: devices.filter(d => d.connection_status === 'offline').length,
            last_updated: new Date().toISOString()
        };
        const response = {
            user_email, summary, devices: devices.map(device => ({
                device_id: device.device_id,
                connection_status: device.connection_status,
                visual_indicator: device.visual_indicator,
                last_seen: device.last_seen,
                minutes_since_last_seen: device.minutes_since_last_seen,
                cpu_temp: device.cpu_temp,
                uptime_seconds: device.uptime_seconds,
                raw_status: device.raw_status,
                health_info: {
                    is_healthy: device.connection_status === 'online',
                    last_heartbeat: device.last_seen,
                    uptime_hours: device.uptime_seconds ? Math.round(device.uptime_seconds / 3600) : null
                }
            }))
        };
        logger.info("Device status indicators retrieved successfully", {
            user_email, device_count: devices.length, summary
        });
        res.status(200).json(response);
    }
    catch (error) {
        logger.error("Error retrieving device status indicators", {
            error: error instanceof Error ? error.message : String(error)
        });
        res.status(500).send("Internal Server Error while retrieving device status indicators");
    }
});
// Periodic function to check device status and send visual indicators to mobile app
// Runs every 2 minutes to update device status indicators
exports.checkDeviceStatusIndicators = (0, scheduler_1.onSchedule)({
    schedule: "*/2 * * * *", timeZone: "America/New_York"
}, async (event) => {
    logger.info("Starting device status indicator check");
    try {
        // Query to get latest device logs with status classification
        const statusQuery = await pool.query(`
            WITH latest_logs AS (SELECT DISTINCT
            ON (device_id, user_email)
                device_id,
                user_email,
                timestamp,
                status,
                cpu_temp,
                uptime_seconds
            FROM device_logs
            ORDER BY device_id, user_email, timestamp DESC
                ),
                status_classification AS (
            SELECT
                device_id, user_email, timestamp, status, cpu_temp, uptime_seconds, CASE
                WHEN timestamp > NOW() - INTERVAL '3 minutes' THEN 'online'
                WHEN timestamp > NOW() - INTERVAL '5 minutes' THEN 'warning'
                ELSE 'offline'
                END as connection_status, EXTRACT (EPOCH FROM (NOW() - timestamp))/60 as minutes_since_last_seen
            FROM latest_logs
                )
            SELECT *
            FROM status_classification
        `);
        const devices = statusQuery.rows;
        logger.info(`Found ${devices.length} devices to process for status indicators`);
        // Group devices by user for efficient notification sending
        const userDevices = devices.reduce((acc, device) => {
            if (!acc[device.user_email]) {
                acc[device.user_email] = [];
            }
            acc[device.user_email].push({
                device_id: device.device_id,
                connection_status: device.connection_status,
                last_seen: device.timestamp,
                minutes_since_last_seen: Math.round(device.minutes_since_last_seen),
                cpu_temp: device.cpu_temp,
                uptime_seconds: device.uptime_seconds,
                raw_status: device.status
            });
            return acc;
        }, {});
        // Process device status indicators for each user
        for (const [userEmail, userDeviceList] of Object.entries(userDevices)) {
            logger.info(`Processed status indicators for user: ${userEmail}`, {
                devices: userDeviceList.length,
                online: userDeviceList.filter((d) => d.connection_status === 'online').length,
                warning: userDeviceList.filter((d) => d.connection_status === 'warning').length,
                offline: userDeviceList.filter((d) => d.connection_status === 'offline').length
            });
        }
        logger.info("Device status indicator check completed successfully");
    }
    catch (error) {
        logger.error("Error in device status indicator check", {
            error: error instanceof Error ? error.message : String(error)
        });
    }
});
/**
 * Start camera streaming for a specific device
 * URL: https://us-central1-realmail-39ab4.cloudfunctions.net/startStreaming/{device_id}
 */
exports.startStreaming = (0, https_1.onRequest)(async (req, res) => {
    res.set('Access-Control-Allow-Origin', '*');
    res.set('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    if (req.method === 'OPTIONS') {
        res.status(204).send('');
        return;
    }
    try {
        const deviceId = req.path.split('/')[1]; // Extract device_id from path
        if (!deviceId) {
            res.status(400).json({
                success: false,
                error: 'Device ID is required'
            });
            return;
        }
        logger.info(`Starting streaming for device: ${deviceId}`);
        // Look up device IP address from database
        const deviceQuery = `SELECT device_id, ip_address FROM devices WHERE device_id = $1`;
        const deviceResult = await pool.query(deviceQuery, [deviceId]);
        if (deviceResult.rows.length === 0) {
            logger.warn(`Device not found: ${deviceId}`);
            res.status(404).json({
                success: false,
                error: 'Device not found'
            });
            return;
        }
        const device = deviceResult.rows[0];
        const deviceIp = device.ip_address;
        if (!deviceIp) {
            logger.warn(`Device IP not available: ${deviceId}`);
            res.status(400).json({
                success: false,
                error: 'Device IP address not available'
            });
            return;
        }
        // Call the device's streaming service API
        const streamingUrl = `http://${deviceIp}:8080/start_streaming/${deviceId}`;
        logger.info(`Calling streaming API: ${streamingUrl}`);
        const response = await (0, node_fetch_1.default)(streamingUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            timeout: 10000 // 10 second timeout
        });
        const responseData = await response.json();
        if (response.ok) {
            logger.info(`Successfully started streaming for device: ${deviceId}`, responseData);
            res.status(200).json({
                success: true,
                message: 'Streaming started successfully',
                device_id: deviceId,
                device_ip: deviceIp,
                data: responseData
            });
        }
        else {
            logger.error(`Failed to start streaming for device: ${deviceId}`, {
                status: response.status,
                data: responseData
            });
            res.status(502).json({
                success: false,
                error: 'Failed to start streaming on device',
                device_response: responseData
            });
        }
    }
    catch (error) {
        logger.error('Error in startStreaming function', {
            error: error instanceof Error ? error.message : String(error)
        });
        res.status(500).json({
            success: false,
            error: 'Internal server error'
        });
    }
});
/**
 * Stop camera streaming for a specific device
 * URL: https://us-central1-realmail-39ab4.cloudfunctions.net/stopStreaming/{device_id}
 */
exports.stopStreaming = (0, https_1.onRequest)(async (req, res) => {
    res.set('Access-Control-Allow-Origin', '*');
    res.set('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    if (req.method === 'OPTIONS') {
        res.status(204).send('');
        return;
    }
    try {
        const deviceId = req.path.split('/')[1]; // Extract device_id from path
        if (!deviceId) {
            res.status(400).json({
                success: false,
                error: 'Device ID is required'
            });
            return;
        }
        logger.info(`Stopping streaming for device: ${deviceId}`);
        // Look up device IP address from database
        const deviceQuery = `SELECT device_id, ip_address FROM devices WHERE device_id = $1`;
        const deviceResult = await pool.query(deviceQuery, [deviceId]);
        if (deviceResult.rows.length === 0) {
            logger.warn(`Device not found: ${deviceId}`);
            res.status(404).json({
                success: false,
                error: 'Device not found'
            });
            return;
        }
        const device = deviceResult.rows[0];
        const deviceIp = device.ip_address;
        if (!deviceIp) {
            logger.warn(`Device IP not available: ${deviceId}`);
            res.status(400).json({
                success: false,
                error: 'Device IP address not available'
            });
            return;
        }
        // Call the device's streaming service API
        const streamingUrl = `http://${deviceIp}:8080/stop_streaming/${deviceId}`;
        logger.info(`Calling streaming API: ${streamingUrl}`);
        const response = await (0, node_fetch_1.default)(streamingUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            timeout: 10000 // 10 second timeout
        });
        const responseData = await response.json();
        if (response.ok) {
            logger.info(`Successfully stopped streaming for device: ${deviceId}`, responseData);
            res.status(200).json({
                success: true,
                message: 'Streaming stopped successfully',
                device_id: deviceId,
                device_ip: deviceIp,
                data: responseData
            });
        }
        else {
            logger.error(`Failed to stop streaming for device: ${deviceId}`, {
                status: response.status,
                data: responseData
            });
            res.status(502).json({
                success: false,
                error: 'Failed to stop streaming on device',
                device_response: responseData
            });
        }
    }
    catch (error) {
        logger.error('Error in stopStreaming function', {
            error: error instanceof Error ? error.message : String(error)
        });
        res.status(500).json({
            success: false,
            error: 'Internal server error'
        });
    }
});
/**
 * Get streaming status for a specific device
 * URL: https://us-central1-realmail-39ab4.cloudfunctions.net/getStreamingStatus/{device_id}
 */
exports.getStreamingStatus = (0, https_1.onRequest)(async (req, res) => {
    res.set('Access-Control-Allow-Origin', '*');
    res.set('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    if (req.method === 'OPTIONS') {
        res.status(204).send('');
        return;
    }
    try {
        const deviceId = req.path.split('/')[1]; // Extract device_id from path
        if (!deviceId) {
            res.status(400).json({
                success: false,
                error: 'Device ID is required'
            });
            return;
        }
        logger.info(`Getting streaming status for device: ${deviceId}`);
        // Look up device IP address from database
        const deviceQuery = `SELECT device_id, ip_address FROM devices WHERE device_id = $1`;
        const deviceResult = await pool.query(deviceQuery, [deviceId]);
        if (deviceResult.rows.length === 0) {
            logger.warn(`Device not found: ${deviceId}`);
            res.status(404).json({
                success: false,
                error: 'Device not found'
            });
            return;
        }
        const device = deviceResult.rows[0];
        const deviceIp = device.ip_address;
        if (!deviceIp) {
            logger.warn(`Device IP not available: ${deviceId}`);
            res.status(200).json({
                success: true,
                device_id: deviceId,
                streaming: false,
                camera_active: false,
                error: 'Device IP not available'
            });
            return;
        }
        // Call the device's status API
        const statusUrl = `http://${deviceIp}:8080/status/${deviceId}`;
        logger.info(`Calling status API: ${statusUrl}`);
        const response = await (0, node_fetch_1.default)(statusUrl, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            timeout: 5000 // 5 second timeout
        });
        if (response.ok) {
            const responseData = await response.json();
            logger.info(`Successfully got streaming status for device: ${deviceId}`, responseData);
            res.status(200).json(Object.assign({ success: true, device_id: deviceId, device_ip: deviceIp }, responseData));
        }
        else {
            logger.warn(`Device streaming service not responding: ${deviceId}`, {
                status: response.status
            });
            res.status(200).json({
                success: true,
                device_id: deviceId,
                device_ip: deviceIp,
                streaming: false,
                camera_active: false,
                error: 'Streaming service not responding'
            });
        }
    }
    catch (error) {
        logger.warn('Error getting streaming status', {
            error: error instanceof Error ? error.message : String(error)
        });
        // Return offline status instead of error
        res.status(200).json({
            success: true,
            device_id: req.path.split('/')[1],
            streaming: false,
            camera_active: false,
            error: 'Device not accessible'
        });
    }
});
//# sourceMappingURL=index.js.map