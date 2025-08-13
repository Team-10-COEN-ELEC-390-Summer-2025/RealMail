#!/usr/bin/env python3
"""
Test script to verify WebRTC server integration
This simulates what the Pi streaming service should do
"""

import asyncio
import socketio
import base64
import time
import logging
from PIL import Image, ImageDraw
import io

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class WebRTCServerTest:
    def __init__(self, server_url, device_id):
        self.server_url = server_url
        self.device_id = device_id
        self.sio = None
        self.streaming = False
        
    async def test_connection(self):
        """Test connection to WebRTC server"""
        try:
            self.sio = socketio.AsyncClient(logger=True, engineio_logger=True)
            
            @self.sio.event
            async def connect():
                logger.info(f"Connected to WebRTC server at {self.server_url}")
                await self.register_as_camera()
                
            @self.sio.event
            async def disconnect():
                logger.info("Disconnected from WebRTC server")
                self.streaming = False
                
            @self.sio.event
            async def connect_error(data):
                logger.error(f"Connection failed: {data}")
                
            @self.sio.event
            async def create_offer(data):
                logger.info("Received create-offer request from server")
                
            # Connect to server
            await self.sio.connect(self.server_url, transports=['websocket'])
            
            # Start sending test frames
            await self.send_test_frames()
            
        except Exception as e:
            logger.error(f"Test failed: {e}")
            
    async def register_as_camera(self):
        """Register as a camera device"""
        logger.info(f"Registering as camera for device: {self.device_id}")
        await self.sio.emit('register', {
            'type': 'camera',
            'device_id': self.device_id
        })
        self.streaming = True
        
    async def send_test_frames(self):
        """Send test frames to simulate camera streaming"""
        frame_count = 0
        
        while self.streaming and frame_count < 100:  # Send 100 test frames
            try:
                # Create a test image
                img = Image.new('RGB', (640, 480), color=(frame_count % 255, 100, 150))
                draw = ImageDraw.Draw(img)
                draw.text((10, 10), f"Test Frame {frame_count}", fill=(255, 255, 255))
                draw.text((10, 30), f"Device: {self.device_id}", fill=(255, 255, 255))
                draw.text((10, 50), f"Time: {time.strftime('%H:%M:%S')}", fill=(255, 255, 255))
                
                # Convert to JPEG bytes
                img_bytes = io.BytesIO()
                img.save(img_bytes, format='JPEG', quality=80)
                img_bytes.seek(0)
                
                # Encode as base64
                b64_image = base64.b64encode(img_bytes.getvalue()).decode('utf-8')
                
                # Send frame to WebRTC server
                frame_data = {
                    'device_id': self.device_id,
                    'frame': b64_image,
                    'timestamp': time.time()
                }
                
                await self.sio.emit('video_frame', frame_data)
                
                frame_count += 1
                logger.info(f"Sent test frame {frame_count}")
                
                # Wait before sending next frame (simulate 10 FPS)
                await asyncio.sleep(0.1)
                
            except Exception as e:
                logger.error(f"Error sending frame {frame_count}: {e}")
                break
                
        logger.info(f"Finished sending {frame_count} test frames")
        
        # Keep connection alive for a bit
        await asyncio.sleep(10)
        
        # Disconnect
        if self.sio:
            await self.sio.disconnect()

async def main():
    import sys
    
    if len(sys.argv) != 3:
        print("Usage: python test_webrtc_integration.py <server_url> <device_id>")
        print("Example: python test_webrtc_integration.py https://camera-webrtc-204949720800.us-central1.run.app test-device-123")
        sys.exit(1)
        
    server_url = sys.argv[1]
    device_id = sys.argv[2]
    
    print(f"Testing WebRTC server integration...")
    print(f"Server: {server_url}")
    print(f"Device ID: {device_id}")
    print(f"Viewer URL: {server_url}/viewer/{device_id}")
    print()
    
    tester = WebRTCServerTest(server_url, device_id)
    await tester.test_connection()

if __name__ == "__main__":
    asyncio.run(main())
