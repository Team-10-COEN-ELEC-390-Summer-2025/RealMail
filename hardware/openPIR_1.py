import RPi.GPIO as GPIO
import time
import requests
from datetime import datetime

motionPin = 17 # GPIO pin number
device_id = "team10_board"
server_url = "https://us-central1-realmail-39ab4.cloudfunctions.net/handleSensorIncomingData"
user_email = "alice01@example.com"

GPIO.setmode(GPIO.BCM)
GPIO.setup(motionPin,GPIO.IN)

# give motion sensor time to analyze room
time.sleep(2)
print("Starting motion senser")

def send_motion_detected():
    payload = {
        "device_id": device_id,
        "timeStamp": datetime.utcnow().isoformat() + "Z",
        "motion_detected": True,
        "user_email": user_email
    }
    try:
        response = requests.post(server_url, json=payload)
        print(f"\nMotion detected. Status Code: {response.status_code}. \nJSON sent: {payload}")
    except Exception as e:
        print(f"Failed to send data: {e}")

try:
    while True:
        if GPIO.input(motionPin): # motion has been detected
            send_motion_detected()
            
            ##wait until motion stops to avoid spamming
            while GPIO.input(motionPin):
                time.sleep(0.1)
        time.sleep(0.1)
except KeyboardInterrupt: # will execute at ctrl + C
    GPIO.cleanup()
    print('\nSignal has been terminated.')
