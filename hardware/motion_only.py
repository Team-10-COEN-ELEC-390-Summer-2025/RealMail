## Code for motion sensor to trigger signal to server

import RPi.GPIO as GPIO
import time
import requests
from datetime import datetime

motionPin = 17  # GPIO pin connected to motion sensor
device_id = "123456"  # Device ID for identification
server_url = "https://us-central1-realmail-39ab4.cloudfunctions.net/handleSensorIncomingData"
user_email = "sanomihigobertin@gmail.com"  # User email for tracking

GPIO.setmode(GPIO.BCM)  # Use Broadcom pin numbering
GPIO.setup(motionPin, GPIO.IN)  # Set motionPin as input

time.sleep(1)  # Wait 1 second for sensor to stabilize
print("Starting motion sensor")

def send_motion_detected():
    # Prepare JSON payload with current UTC time and device info
    payload = {
        "device_id": device_id,
        "timeStamp": datetime.utcnow().isoformat() + "Z",
        "motion_detected": True,
        "user_email": user_email
    }
    try:
        # Send POST request with motion event data
        response = requests.post(server_url, json=payload)
        print(f"\nMotion detected. Status Code: {response.status_code}. \nJSON sent: {payload}")
    except Exception as e:
        # Print error if sending fails
        print(f"Failed to send data: {e}")

try:
    while True:
        if GPIO.input(motionPin):  # Motion detected (input HIGH)
            send_motion_detected()  # Send motion event to server
            
            # Wait here until motion stops to avoid repeated triggers
            while GPIO.input(motionPin):
                time.sleep(0.00001)  # Short sleep to reduce CPU usage
        time.sleep(0.00001)  # Short sleep while waiting for motion

except KeyboardInterrupt:
    GPIO.cleanup()  # Reset GPIO pins on exit
    print('\nSignal has been terminated.')