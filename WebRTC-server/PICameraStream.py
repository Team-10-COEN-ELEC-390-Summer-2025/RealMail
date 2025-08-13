#!/usr/bin/env python3
"""
Raspberry Pi Camera WebRTC Streaming Client
Integrates with the WebRTC server for device-specific streaming
"""

import argparse
import asyncio
import base64
import logging
import time
from threading import Thread, Event

import cv2
import socketio
from picamera2 import Picamera2

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class PiCameraStreamer:
    def __init__(self, server_url, device_id, resolution=(1280, 720), fps=30):
        self.server_url = server_url
        self.device_id = device_id
        self.resolution = resolution
        self.fps = fps

        # Camera setup
        self.picam2 = None
        self.streaming = False
        self.stop_event = Event()

        # Socket.IO client
        self.sio = socketio.AsyncClient(logger=True, engineio_logger=True)

        # Frame buffer
        self.current_frame = None
        self.frame_lock = Event()

        # Setup socket events
        self.setup_socket_events()

    def setup_socket_events(self):
        """Setup Socket.IO event handlers"""

        @self.sio.event
        async def connect():
            logger.info(f"Connected to server at {self.server_url}")
            await self.sio.emit('register', {'type': 'camera', 'device_id': self.device_id})
            self.streaming = True
            Thread(target=self.stream_frames, daemon=True).start()

        @self.sio.event
        async def disconnect():
            logger.info("Disconnected from server")
            self.streaming = False

        @self.sio.event
        async def connect_error(data):
            logger.error(f"Connection failed: {data}")
            self.streaming = False

    async def connect(self):
        await self.sio.connect(self.server_url, transports=['websocket'])
        await self.sio.wait()

    def stream_frames(self):
        self.picam2 = Picamera2()
        self.picam2.configure(self.picam2.create_video_configuration(main={'size': self.resolution}))
        self.picam2.start()
        logger.info("Camera started streaming.")
        while not self.stop_event.is_set() and self.streaming:
            frame = self.picam2.capture_array()
            _, jpeg = cv2.imencode('.jpg', frame)
            b64_jpeg = base64.b64encode(jpeg.tobytes()).decode('utf-8')
            data = {
                'device_id': self.device_id,
                'image': b64_jpeg,
                'timestamp': time.time()
            }
            asyncio.run(self.sio.emit('stream', data))
            time.sleep(1 / self.fps)
        self.picam2.stop()
        logger.info("Camera stopped streaming.")

    def stop(self):
        self.stop_event.set()
        self.streaming = False


def main():
    """Main function to run the Pi Camera streamer"""
    parser = argparse.ArgumentParser(description='Raspberry Pi Camera WebRTC Streamer')
    parser.add_argument('--server', required=True, help='WebRTC server URL (e.g., https://127.0.0.1:8443)')
    parser.add_argument('--team10_board', required=True, help='Device ID for this Pi camera')
    parser.add_argument('--width', type=int, default=1280, help='Camera resolution width')
    parser.add_argument('--height', type=int, default=720, help='Camera resolution height')
    parser.add_argument('--fps', type=int, default=30, help='Frames per second')
    args = parser.parse_args()

    streamer = PiCameraStreamer(
        server_url=args.server,
        device_id=args.team10_board,
        resolution=(args.width, args.height),
        fps=args.fps
    )
    try:
        asyncio.run(streamer.connect())
    except KeyboardInterrupt:
        logger.info("Interrupted by user. Stopping...")
        streamer.stop()
    except Exception as e:
        logger.error(f"Error: {e}")
        streamer.stop()


if __name__ == "__main__":
    main()
