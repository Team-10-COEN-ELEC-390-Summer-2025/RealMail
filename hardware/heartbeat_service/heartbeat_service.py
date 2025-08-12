#!/usr/bin/env python3
"""
Raspberry Pi Heartbeat Service
Sends periodic heartbeat signals to Firebase backend to indicate device online status
"""

import os
import sys
import time
import requests
import logging
import subprocess
from datetime import datetime
from pathlib import Path

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('/var/log/heartbeat_service.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

class HeartbeatService:
    def __init__(self, config_file='./hardware/device_info.txt'):
        self.config = self.load_config(config_file)
        self.session = requests.Session()
        self.session.timeout = 30

    def load_config(self, config_file):
        """Load configuration from device_info.txt"""
        config = {}
        try:
            # Get the directory where this script is located
            script_dir = Path(__file__).parent.absolute()
            config_path = script_dir / 'device_info.txt'

            if not config_path.exists():
                # Fallback to the provided path
                config_path = Path(config_file)

            logger.info(f"Loading config from: {config_path}")

            with open(config_path, 'r') as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith('#'):
                        if '=' in line:
                            key, value = line.split('=', 1)
                            config[key.strip()] = value.strip()

            # Validate required fields
            required_fields = ['DEVICE_ID', 'USER_EMAIL', 'HEARTBEAT_URL', 'HEARTBEAT_INTERVAL']
            for field in required_fields:
                if field not in config:
                    raise ValueError(f"Missing required field: {field}")

            # Convert interval to integer
            config['HEARTBEAT_INTERVAL'] = int(config['HEARTBEAT_INTERVAL'])

            logger.info(f"Configuration loaded successfully for device: {config['DEVICE_ID']}")
            return config

        except Exception as e:
            logger.error(f"Failed to load configuration: {e}")
            raise

    def get_system_info(self):
        """Get system information for heartbeat"""
        try:
            # Get CPU temperature
            cpu_temp = None
            try:
                with open('/sys/class/thermal/thermal_zone0/temp', 'r') as f:
                    cpu_temp = float(f.read().strip()) / 1000.0
            except:
                pass

            # Get uptime
            uptime_seconds = None
            try:
                with open('/proc/uptime', 'r') as f:
                    uptime_seconds = int(float(f.read().split()[0]))
            except:
                pass

            return {
                'cpu_temp': cpu_temp,
                'uptime_seconds': uptime_seconds,
                'system_timestamp': datetime.now().isoformat()
            }
        except Exception as e:
            logger.warning(f"Failed to get system info: {e}")
            return {}

    def send_heartbeat(self):
        """Send heartbeat to Firebase backend"""
        try:
            system_info = self.get_system_info()

            payload = {
                'device_id': self.config['DEVICE_ID'],
                'user_email': self.config['USER_EMAIL'],
                'timestamp': datetime.now().isoformat(),
                'status': 'online',
                **system_info
            }

            response = self.session.post(
                self.config['HEARTBEAT_URL'],
                json=payload,
                headers={'Content-Type': 'application/json'}
            )

            if response.status_code == 200:
                logger.info(f"Heartbeat sent successfully for device {self.config['DEVICE_ID']}")
                return True
            else:
                logger.warning(f"Heartbeat failed with status {response.status_code}: {response.text}")
                return False

        except requests.exceptions.RequestException as e:
            logger.error(f"Network error sending heartbeat: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error sending heartbeat: {e}")
            return False

    def run(self):
        """Main service loop"""
        logger.info(f"Starting heartbeat service for device {self.config['DEVICE_ID']}")
        logger.info(f"Heartbeat interval: {self.config['HEARTBEAT_INTERVAL']} seconds")
        logger.info(f"Target URL: {self.config['HEARTBEAT_URL']}")

        consecutive_failures = 0
        max_failures = 5

        while True:
            try:
                success = self.send_heartbeat()

                if success:
                    consecutive_failures = 0
                else:
                    consecutive_failures += 1

                if consecutive_failures >= max_failures:
                    logger.error(f"Too many consecutive failures ({consecutive_failures}). Will keep trying...")
                    # Don't exit, just log and continue
                    consecutive_failures = 0  # Reset to avoid spam

                time.sleep(self.config['HEARTBEAT_INTERVAL'])

            except KeyboardInterrupt:
                logger.info("Heartbeat service stopped by user")
                break
            except Exception as e:
                logger.error(f"Unexpected error in main loop: {e}")
                time.sleep(self.config['HEARTBEAT_INTERVAL'])

def main():
    try:
        service = HeartbeatService()
        service.run()
    except Exception as e:
        logger.error(f"Failed to start heartbeat service: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
