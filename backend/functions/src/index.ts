/**
 * Import function triggers from their respective submodules:
 *
 * import {onCall} from "firebase-functions/v2/https";
 * import {onDocumentWritten} from "firebase-functions/v2/firestore";
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

import {setGlobalOptions} from "firebase-functions";
import {onRequest} from "firebase-functions/https";
const { onInit } = require('firebase-functions/v2/core');
import * as logger from "firebase-functions/logger";
import pool from "./db_connect";
import * as admin from 'firebase-admin';
import {getAuth} from "firebase-admin/auth";

// Start writing functions
// https://firebase.google.com/docs/functions/typescript

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({maxInstances: 10});

admin.initializeApp();

// https://firebase.google.com/docs/functions/tips
onInit(async () => {
    try {
        const result = await pool.query("SELECT NOW()"); // test the connection
        console.log("Database connected successfully:", result.rows[0]);
    } catch (error) {
        console.error("Database connection failed:", error);
        throw new Error("Database connection failed");
    }
});

// a dummy function to test the http
export const handleFirebaseJWT = onRequest((req, res) => {
    logger.info("Received request", {method: req.method, url: req.url});

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
export const handleSensorIncomingData = onRequest(async (req, res) => {
    logger.info("Received sensor data", {method: req.method, url: req.url});
    if (req.method !== "POST") {
        res.status(405).send("Method Not Allowed");
        logger.warn("Method not allowed", {method: req.method});
        return;
    }
    const sensorData = req.body;
    /* sensordata schema is below
        device_id: string
        timeStamp: iso 8601 string
        motion_detected: boolean
        user_email: string
        */
    if (!sensorData || !sensorData.device_id || !sensorData.timeStamp ||
        typeof sensorData.motion_detected !== "boolean" ||
        !sensorData.user_email) {
        res.status(400).send("Bad Request: Invalid sensor data");
        logger.error("Invalid sensor data", {data: sensorData});
        return;
    }
    // store data in the database.
    try {
        pool.query(
            `INSERT INTO sensors_data (device_id, timestamp, motion_detected, linked_user_email)
             VALUES ($1, $2, $3, $4)`,
            [
                sensorData.device_id,
                sensorData.timeStamp,
                sensorData.motion_detected,
                sensorData.user_email,
            ]
        );
        res.status(200).send("Sensor data received successfully");
    } catch (error) {
        logger.error("Database insertion failed", {data: sensorData});
        res.status(500).send("Internal Server Error: Failed to store sensor data");
        return;
    }

});

// handle JWT authentication token from Android app
// https://firebase.google.com/docs/auth/admin/manage-users
export const verifyToken = onRequest(async (req, res) => {
    logger.info("Received JWT authentication request", {method: req.method, url: req.url});
    if (req.method !== "POST") {
        res.status(405).send("Method Not Allowed");
        logger.warn("Method not allowed", {method: req.method});
        return;
    }
    const jwt_token: string = req.query.token as string || req.body.token;
    const user_uid: string = req.query.uid as string || req.body.uid;
    if (!jwt_token) {
        res.status(400).send("Bad Request: Missing JWT token");
        logger.error("Missing JWT token in request");
        return;
    }
    if (!user_uid) {
        res.status(400).send("Bad Request: Missing user UID");
        logger.error("Missing user UID in request");
        return;
    }

    try {
        const userRecord = await getAuth().getUser(user_uid);
        // Corrected ON CONFLICT to use (user_uid) instead of (user_id)
        await pool.query(
            `INSERT INTO public.firebase_auth (user_uid, firebase_auth_email, jwt)
             VALUES ($1, $2, $3) ON CONFLICT (user_uid) DO
            UPDATE
                SET firebase_auth_email = EXCLUDED.firebase_auth_email, jwt = EXCLUDED.jwt`,
            [userRecord.uid, userRecord.email, jwt_token]
        );
        res.status(200).send("JWT token received and saved successfully");
    } catch (error) {
        // Log the actual error and send a response to prevent timeout
        if (error instanceof Error) {
            logger.error("Unable to save authentication details in the db", {error: error.message, stack: error.stack});
        } else {
            logger.error("Unable to save authentication details in the db", {error: String(error)});
        }
        res.status(500).send("Internal Server Error");
    }
});