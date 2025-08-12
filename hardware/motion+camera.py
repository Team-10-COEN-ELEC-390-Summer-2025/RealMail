## Code for motion sensor to trigger camera, then send images to server

import os
import sys
import RPi.GPIO as GPIO
import json
import time
import requests
from datetime import datetime
import subprocess

import warnings
# Ignore specific warning from google-crc32c fallback
warnings.filterwarnings("ignore", message="As the c extension couldn't be imported, `google-crc32c` is using a pure python implementation that is significantly slower.")

# GPIO pin for motion sensor
motionPin = 17
# Device identifier
device_id = "123456"
# URL to send motion event data
motion_url = "https://us-central1-realmail-39ab4.cloudfunctions.net/handleSensorIncomingData"
# URL to update device status
status_url = "https://us-central1-realmail-39ab4.cloudfunctions.net/updateSensorStatus"
# User email for identification
user_email = "sanomihigobertin@gmail.com"

# Use Broadcom pin numbering
GPIO.setmode(GPIO.BCM)
# Set motionPin as input
GPIO.setup(motionPin, GPIO.IN)

# Wait 2 seconds for sensor stabilization
time.sleep(2)

# Send device status: online or offline
def send_device_status(status):
    payload1 = {
        "device_id": device_id,
        "status": status,
        "user_email": user_email
    }
    try:
        # Post status to server
        response = requests.post(status_url, json=payload1)
        print(f"\n=== MACHINE {status.upper()} ===")
        print(f"Sensors {status}. Response code: {response.status_code}\n")
        print("Payload:")
        print(json.dumps(payload1, indent=2))
        return response.status_code
    except Exception as e:
        # Print error if failed
        print(f"Failed to send status '{status}': {e}")
        return None

# Send motion detected event to server
def send_motion_detected(timestamp_iso):
    payload2 = {
        "device_id": device_id,
        "timeStamp": timestamp_iso,
        "motion_detected": True,
        "user_email": user_email
    }
    try:
        # Post motion event
        response = requests.post(motion_url, json=payload2)
        print("\n=== MOTION DETECTED ===")
        print(f"Response code: {response.status_code}\n")
        print("Payload:")
        print(json.dumps(payload2, indent=2))
        return response.status_code
    except Exception as e:
        # Print error if failed
        print(f"Failed to send motion event: {e}")
        return None

# Capture image and upload to Firebase
def take_and_upload_photo(filename, image_number):
    filepath = f"/tmp/{filename}"
    try:
        # Capture image with rpicam-still
        subprocess.run(['rpicam-still', '-o', filepath], check=True)
        print(f"\nImage {image_number} saved: {filepath}")
    except subprocess.CalledProcessError as e:
        # Print capture error
        print(f"Failed to capture image {image_number}: {e}")
        return

    try:
        # Firebase API key and bucket name
        api_key = "AIzaSyDJbBgsd3EI5Hn2oo_H316SQyeh53efwz0"
        bucket = "realmail-39ab4.firebasestorage.app"

        # Authenticate anonymously to get token
        auth_url = f"https://identitytoolkit.googleapis.com/v1/accounts:signUp?key={api_key}"
        auth_response = requests.post(auth_url, json={})
        auth_response.raise_for_status()
        id_token = auth_response.json()['idToken']

        # Prepare upload URL and headers
        upload_path = f"{device_id}_{timestamp_str}/{filename}"
        upload_url = f"https://firebasestorage.googleapis.com/v0/b/{bucket}/o?uploadType=media&name={upload_path}"
        headers = {
            "Authorization": f"Bearer {id_token}",
            "Content-Type": "image/jpeg"
        }

        # Upload the image file
        with open(filepath, "rb") as f:
            upload_response = requests.post(upload_url, headers=headers, data=f)
            upload_response.raise_for_status()

        metadata = upload_response.json()
        download_token = metadata.get("downloadTokens")

        if download_token:
            print(f"Uploaded: {filename}")
        else:
            print(f"Uploaded without token: {filename}")

    except Exception as e:
        # Print upload error
        print(f"Upload error: {e}")

    finally:
        # Delete local image file if exists
        if os.path.exists(filepath):
            os.remove(filepath)
            print("Deleted local file")

# Notify server device is online
send_device_status("online")

try:
    while True:
        # Check for motion detected
        if GPIO.input(motionPin):
            timestamp = datetime.utcnow()
            timestamp_iso = timestamp.isoformat() + "Z"
            global timestamp_str
            timestamp_str = timestamp.strftime("%Y%m%dT%H%M%S")

            # Send motion event to server
            send_motion_detected(timestamp_iso)

            # Take and upload 5 photos quickly
            for i in range(1, 6):
                photo_name = f"{timestamp_str}_{i}.jpg"
                take_and_upload_photo(photo_name, i)
                time.sleep(0.001)  # tiny pause between shots

            print("===============================================")

            # Wait until motion stops
            while GPIO.input(motionPin):
                time.sleep(0)

            print("\nWaiting for next motion...\n")

        # Small delay to reduce CPU usage
        time.sleep(0.1)

except KeyboardInterrupt:
    # Clean up GPIO and notify server offline
    GPIO.cleanup()
    send_device_status("offline")
    print("\nProgram ended.")