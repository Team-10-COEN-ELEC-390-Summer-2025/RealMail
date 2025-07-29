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
Object.defineProperty(exports, "__esModule", { value: true });
exports.getAllDevicesForUser = exports.removeDevice = exports.addNewDevice = exports.getSensorsWithMotionDetected = exports.newDataNotification = exports.getDeviceRegistrationToken = exports.updateSensorStatus = exports.checkSensorStatus = exports.verifyToken = exports.handleSensorIncomingData = exports.handleFirebaseJWT = void 0;
const https_1 = require("firebase-functions/https");
const logger = __importStar(require("firebase-functions/logger"));
const admin = __importStar(require("firebase-admin"));
const auth_1 = require("firebase-admin/auth");
const messaging_1 = require("firebase-admin/messaging");
require("dotenv/config");
const pg_1 = require("pg");
const scheduler_1 = require("firebase-functions/v2/scheduler");
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
async function sendNotificationToDevice(options) {
    // once sensor data is stored, send notification to the app
    try {
        const response = await fetch(process.env.SERVER_URL + "/newDataNotification", options);
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
// Periodic API to check update android app about the sensor status
// check in database, if last update time is less than 1 minute, then send notification to the app.
// https://firebase.google.com/docs/functions/schedule-functions?gen=2nd
exports.checkSensorStatus = (0, scheduler_1.onSchedule)({ schedule: "*/5 * * * *", timeZone: "America/Toronto" }, async (event) => {
    /*
    data struct expected
        {
            "device_id": string,
            "status": string, // "online" or "offline"
            "user_email": string
     */
    // get all sensors from the database
    let sensors;
    // check if last update time is less than 1 minute
    // const currentTime = new Date();
    // const oneMinuteAgo = new Date(currentTime.getTime() - 1 * 60 * 1000); // 1 min = 60,000 ms
    // below gets distinct sensors with their last activity time.
    const result_AllTimesOneMinuteAgo = await pool.query(`
            WITH latest_activity AS (SELECT DISTINCT
            ON (user_email)
                *
            FROM sensors_online_activity
            ORDER BY user_email, last_activity DESC
                )
            SELECT *
            FROM latest_activity
            WHERE last_activity > now() - interval '2 minutes'
              AND status ILIKE 'online';`);
    // if results is empty -> all devices are offline!
    // if results is not empty then sensors are online. let users know that sensors are online.
    if (result_AllTimesOneMinuteAgo.rows.length === 0) {
        logger.info("All sensors are offline for last 2 minutes. no notifications will be sent.");
        return;
    }
    sensors = result_AllTimesOneMinuteAgo.rows;
    logger.info("Sensors that need status update:", { sensors });
    // for each sensor, send notification to the user
    for (const sensor of sensors) {
        const sensorData = {
            device_id: sensor.device_id, status: sensor.status, user_email: sensor.user_email,
        };
        // send notification to the user
        await sendNotificationToDevice({
            method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(sensorData)
        });
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
                                  NOW())`, [sensorStatus.device_id, sensorStatus.status, sensorStatus.user_email]);
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
    let dataInString;
    if (isNaN(dateOfSensorData.getTime())) {
        dataInString = new Date().toISOString(); // if the date is invalid, use current date`
    }
    else {
        dataInString = dateOfSensorData.toISOString(); // convert date to ISO string
    }
    const message = {
        notification: {
            title: "New Mail Alert", body: "New mail is delivered in your mailbox on " + dataInString,
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
//# sourceMappingURL=index.js.map