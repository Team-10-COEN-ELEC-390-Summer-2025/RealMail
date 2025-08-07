import os
import sys
import RPi.GPIO as GPIO
import json
import time
import requests
from datetime import datetime
from picamera2 import Picamera2

import warnings
# Remove warnings message
warnings.filterwarnings("ignore", message="As the c extension couldn't be imported, `google-crc32c` is using a pure python implementation that is significantly slower.")

# --------------------------
# GPIO and device settings
# --------------------------
motionPin = 17  # GPIO pin number where motion sensor output is connected 
device_id = "123456"  # Unique identifier for device
motion_url = "https://us-central1-realmail-39ab4.cloudfunctions.net/handleSensorIncomingData"
status_url = "https://us-central1-realmail-39ab4.cloudfunctions.net/updateSensorStatus"
user_email = "sanomihigobertin@gmail.com"  # User email to send with JSON data

# Setup GPIO in BCM mode and motionPin as input
GPIO.setmode(GPIO.BCM)
GPIO.setup(motionPin, GPIO.IN)

# --------------------------
# Camera setup using picamera2
# --------------------------

# Initialize the camera
camera = Picamera2()

camera_config = camera.create_still_configuration(
    main={"size": (1640, 1232)},  # Higher resolution capture size for IMX219
    lores={"size": (640, 480)}    # Lower resolution stream if needed
)

camera.configure(camera_config)
camera.start()

# --------------------------
# Start motion sensor - function to send status event JSON to server
# --------------------------
time.sleep(2)
def send_device_status(status):
    payload1 = {
        "device_id": device_id,
        "status": status,
        "user_email": user_email
    }
    try:
        response = requests.post(status_url, json=payload1)
        print(f"\n================= MACHINE {status.upper()} =================")
        print(f"\n\nSensors turned {status}. Status Code: {response.status_code}\n")
        print("JSON sent:")
        print(json.dumps(payload1, indent=2))  # Pretty print JSON payload1
        return response.status_code
    except Exception as e:
        print(f"Failed to send device status '{status}': {e}")
        return None

    
# --------------------------
# Function to send motion event JSON to server
# --------------------------
def send_motion_detected(timestamp_iso):
    payload2 = {
        "device_id": device_id,
        "timeStamp": timestamp_iso,
        "motion_detected": True,
        "user_email": user_email
    }
    try:
        response = requests.post(motion_url, json=payload2)
        print("\n================= MOTION EVENT =================")
        print(f"\nMotion detected. Status Code: {response.status_code}\n")
        print("JSON sent:")
        print(json.dumps(payload2, indent=2))  # Pretty print JSON payload2
        return response.status_code
    except Exception as e:
        print(f"Failed to send data: {e}")
        return None


# Get current UTC time for consistent timestamping
timestamp = datetime.utcnow()
# ISO format string for JSON payload2
timestamp_iso = timestamp.isoformat() + "Z"
# Compact timestamp for filenames
timestamp_str = timestamp.strftime("%Y%m%dT%H%M%S")

# --------------------------
# Capture photo & upload
# --------------------------
def take_and_upload_photo(filename, image_number):
    filepath = f"/tmp/{filename}"
    camera.capture_file(filepath)
    print(f"\n📸 Image {image_number}: {filepath}")

    try:
        # Firebase config
        api_key = "AIzaSyDJbBgsd3EI5Hn2oo_H316SQyeh53efwz0"
        bucket = "realmail-39ab4.firebasestorage.app"

        # Step 1: Sign in anonymously
        auth_url = f"https://identitytoolkit.googleapis.com/v1/accounts:signUp?key={api_key}"
        auth_response = requests.post(auth_url, json={})
        auth_response.raise_for_status()
        id_token = auth_response.json()['idToken']

        # Step 2: Upload file to Firebase Storage
        upload_path = f"{device_id}_{timestamp_str}/{filename}"
        upload_url = f"https://firebasestorage.googleapis.com/v0/b/{bucket}/o?uploadType=media&name={upload_path}"

        headers = {
            "Authorization": f"Bearer {id_token}",
            "Content-Type": "image/jpeg"
        }

        with open(filepath, "rb") as f:
            upload_response = requests.post(upload_url, headers=headers, data=f)
            upload_response.raise_for_status()

        metadata = upload_response.json()
        download_token = metadata.get("downloadTokens")

        if download_token:
            download_url = f"https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{upload_path.replace('/', '%2F')}?alt=media&token={download_token}"
            print(f"✅ Uploaded: {filename}")
        else:
            print(f"✅ Uploaded (no download token): {filename}")

    except Exception as e:
        print(f"❌ Error uploading photo: {e}")

    finally:
        if os.path.exists(filepath):
            os.remove(filepath)
            print("🗑️ Deleted local file")

# --------------------------
# Main loop: wait for motion, then act
# --------------------------

# Send online status
send_device_status("online")
try:
    while True:
        if GPIO.input(motionPin):  # Motion detected
            # Send motion event JSON to server and print status
            send_motion_detected(timestamp_iso)

            # Take first photo immediately
            photo_name1 = f"{timestamp_str}_1.jpg"
            take_and_upload_photo(photo_name1, 1)

            # Wait 5 seconds, then take second photo
            time.sleep(5)
            photo_name2 = f"{timestamp_str}_2.jpg"
            take_and_upload_photo(photo_name2, 2)

            # Wait another 5 seconds (10 seconds total), then take third photo
            time.sleep(5)
            photo_name3 = f"{timestamp_str}_3.jpg"
            take_and_upload_photo(photo_name3, 3)

            print("===============================================\n")

            # Wait here until motion ends to avoid repeated triggers
            while GPIO.input(motionPin):
                time.sleep(0)

            # After motion ended, show waiting for next motion
            print("\nWaiting for the next motion...\n")

        # Short delay to reduce CPU usage while waiting for motion
        time.sleep(0.1)

except KeyboardInterrupt:  # Handle Ctrl+C gracefully
    GPIO.cleanup()  # Reset GPIO pins to safe state
    send_device_status("offline")        
    print("\nProgram terminated cleanly.")