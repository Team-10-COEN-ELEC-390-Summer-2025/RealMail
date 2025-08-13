import ssl
from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse, HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
import os
from typing import Dict
import logging
import socketio
import json
# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("webrtc-server")

# Create Socket.IO server with correct configuration
sio = socketio.AsyncServer(
    async_mode='asgi',
    cors_allowed_origins='*',
    logger=True,
    engineio_logger=True,
    ping_timeout=60,
    ping_interval=25
)

# Create FastAPI app
app = FastAPI(title="WebRTC Camera Server", version="1.0.0")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Serve static files
static_dir = os.path.join(os.path.dirname(__file__), "src")
app.mount("/static", StaticFiles(directory=static_dir), name="static")

# Store connected clients and active devices
clients = {}
active_devices = {}  # device_id -> camera_sid mapping
device_viewers = {}  # device_id -> list of viewer_sids

# Socket.IO events
@sio.event
async def connect(sid, environ):
    logger.info(f"Client connected: {sid}")

@sio.event
async def disconnect(sid):
    logger.info(f"Client disconnected: {sid}")
    if sid in clients:
        client_info = clients[sid]
        client_type = client_info['type']
        device_id = client_info.get('device_id')

        del clients[sid]

        if client_type == 'camera' and device_id:
            # Remove camera from active devices
            if device_id in active_devices and active_devices[device_id] == sid:
                del active_devices[device_id]
                logger.info(f'Camera disconnected for device: {device_id}')

                # Notify viewers of this device that camera is gone
                if device_id in device_viewers:
                    for viewer_sid in device_viewers[device_id]:
                        if viewer_sid in clients:
                            await sio.emit('no-camera', room=viewer_sid)
                    del device_viewers[device_id]

        elif client_type == 'viewer' and device_id:
            # Remove viewer from device viewers list
            if device_id in device_viewers:
                device_viewers[device_id] = [v for v in device_viewers[device_id] if v != sid]
                if not device_viewers[device_id]:
                    del device_viewers[device_id]

@sio.event
async def register(sid, data):
    client_type = data.get('type')
    device_id = data.get('device_id')

    clients[sid] = {'type': client_type, 'sid': sid, 'device_id': device_id}
    logger.info(f"{client_type} registered: {sid} for device: {device_id}")

    if client_type == 'camera':
        # Register camera for specific device
        active_devices[device_id] = sid
        logger.info(f'Camera active for device: {device_id}')

        # Notify viewers of this device that camera is available
        if device_id in device_viewers:
            for viewer_sid in device_viewers[device_id]:
                if viewer_sid in clients:
                    await sio.emit('camera-available', room=viewer_sid)

        # Ask camera to create offer
        await sio.emit('create-offer', room=sid)

    elif client_type == 'viewer':
        # Add viewer to device viewers list
        if device_id not in device_viewers:
            device_viewers[device_id] = []
        device_viewers[device_id].append(sid)

        # Check if camera is available for this device
        if device_id in active_devices:
            camera_sid = active_devices[device_id]
            if camera_sid in clients:
                logger.info(f'Camera available for viewer on device: {device_id}')
                await sio.emit('camera-available', room=sid)
                # Ask camera to create offer for this viewer
                await sio.emit('create-offer', room=camera_sid)
            else:
                await sio.emit('no-camera', room=sid)
        else:
            await sio.emit('no-camera', room=sid)

@sio.event
async def offer(sid, data):
    logger.info(f'Received offer from camera: {sid}')
    # Get device_id for this camera
    device_id = clients.get(sid, {}).get('device_id')
    if device_id and device_id in device_viewers:
        # Forward offer to viewers of this device
        for viewer_sid in device_viewers[device_id]:
            if viewer_sid in clients:
                logger.info(f'Sending offer to viewer: {viewer_sid} for device: {device_id}')
                await sio.emit('offer', data, room=viewer_sid)

@sio.event
async def answer(sid, data):
    logger.info(f'Received answer from viewer: {sid}')
    # Get device_id for this viewer
    device_id = clients.get(sid, {}).get('device_id')
    if device_id and device_id in active_devices:
        camera_sid = active_devices[device_id]
        if camera_sid in clients:
            logger.info(f'Sending answer to camera: {camera_sid} for device: {device_id}')
            await sio.emit('answer', data, room=camera_sid)

@sio.event
async def ice_candidate(sid, data):
    logger.info(f'Received ICE candidate from: {sid}')
    client_info = clients.get(sid, {})
    client_type = client_info.get('type')
    device_id = client_info.get('device_id')

    if client_type == 'camera' and device_id in device_viewers:
        # Send to viewers of this device
        for viewer_sid in device_viewers[device_id]:
            if viewer_sid in clients:
                await sio.emit('ice-candidate', data, room=viewer_sid)
    elif client_type == 'viewer' and device_id in active_devices:
        # Send to camera of this device
        camera_sid = active_devices[device_id]
        if camera_sid in clients:
            await sio.emit('ice-candidate', data, room=camera_sid)

@sio.event
async def video_frame(sid, data):
    """Handle video frames from Pi Camera"""
    logger.debug(f'Received video frame from camera: {sid}')
    
    # Get device_id for this camera
    device_id = clients.get(sid, {}).get('device_id')
    if device_id and device_id in device_viewers:
        # Forward frame to viewers of this device
        frame_data = {
            'device_id': device_id,
            'frame': data.get('frame'),
            'timestamp': data.get('timestamp')
        }
        
        for viewer_sid in device_viewers[device_id]:
            if viewer_sid in clients:
                await sio.emit('video_frame', frame_data, room=viewer_sid)

# FastAPI routes
@app.get("/")
def root():
    return FileResponse(os.path.join(static_dir, "test.html"))

@app.get("/streamer/{device_id}")
async def get_streamer_with_device_id(device_id: str):
    """Serve streamer page for specific device ID"""
    if not device_id or len(device_id.strip()) == 0:
        raise HTTPException(status_code=404, detail="Device ID not found")

    # Read the streamer HTML file and inject the device_id
    try:
        with open(os.path.join(static_dir, "streamer.html"), 'r') as f:
            html_content = f.read()

        # Inject device_id into the HTML - replace the placeholder
        # Look for the exact pattern in the HTML file
        old_line = "        const DEVICE_ID = DEVICE_ID || 'default-device';"
        # Escape device_id for safe JavaScript embedding
        new_line = f"        const DEVICE_ID = {json.dumps(device_id)};"
        
        html_content = html_content.replace(old_line, new_line)
        
        logger.info(f"Serving streamer page for device: {device_id}")
        return HTMLResponse(content=html_content)
    except FileNotFoundError:
        logger.error(f"Streamer page not found at: {os.path.join(static_dir, 'streamer.html')}")
        raise HTTPException(status_code=404, detail="Streamer page not found")
    except Exception as e:
        logger.error(f"Error serving streamer page: {e}")
        raise HTTPException(status_code=500, detail="Internal server error")

@app.get("/android/{device_id}")
async def get_android_with_device_id(device_id: str):
    """Serve Android viewer page for specific device ID"""
    if not device_id or len(device_id.strip()) == 0:
        raise HTTPException(status_code=404, detail="Device ID not found")

    # Read the android HTML file and inject the device_id
    try:
        with open(os.path.join(static_dir, "android.html"), 'r') as f:
            html_content = f.read()

        # Inject device_id into the HTML - replace the placeholder
        html_content = html_content.replace(
            'const DEVICE_ID = DEVICE_ID || \'default-device\';',
            f'const DEVICE_ID = "{device_id}";'
        )

        return HTMLResponse(content=html_content)
    except FileNotFoundError:
        raise HTTPException(status_code=404, detail="Android viewer page not found")

@app.get("/viewer/{device_id}")
async def get_viewer_with_device_id(device_id: str):
    """Serve viewer page for specific device ID"""
    if not device_id or len(device_id.strip()) == 0:
        raise HTTPException(status_code=404, detail="Device ID not found")

    # Read the viewer HTML file and inject the device_id
    try:
        with open(os.path.join(static_dir, "viewer.html"), 'r') as f:
            html_content = f.read()

        # Inject device_id into the HTML - replace the placeholder
        html_content = html_content.replace(
            'const DEVICE_ID = DEVICE_ID || \'default-device\';',
            f'const DEVICE_ID = "{device_id}";'
        )

        return HTMLResponse(content=html_content)
    except FileNotFoundError:
        raise HTTPException(status_code=404, detail="Viewer page not found")

# Legacy routes (without device_id) - redirect to error page
@app.get("/streamer")
def get_streamer():
    error_html = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Device ID Required</title>
        <style>
            body {
                font-family: Arial, sans-serif;
                background: #111;
                color: #fff;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                height: 100vh;
                margin: 0;
            }
            .error-container {
                text-align: center;
                padding: 40px;
                background: #222;
                border-radius: 10px;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
            }
            h1 { color: #ff6b6b; margin-bottom: 20px; }
            p { margin-bottom: 10px; }
            .example { color: #4ecdc4; font-family: monospace; }
        </style>
    </head>
    <body>
        <div class="error-container">
            <h1>404 - Device ID Required</h1>
            <p>Please specify a device ID in the URL.</p>
            <p>Example: <span class="example">/streamer/my-device-123</span></p>
        </div>
    </body>
    </html>
    """
    return HTMLResponse(content=error_html, status_code=404)

@app.get("/android")
def get_android():
    error_html = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Device ID Required</title>
        <style>
            body {
                font-family: Arial, sans-serif;
                background: #111;
                color: #fff;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                height: 100vh;
                margin: 0;
            }
            .error-container {
                text-align: center;
                padding: 40px;
                background: #222;
                border-radius: 10px;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
            }
            h1 { color: #ff6b6b; margin-bottom: 20px; }
            p { margin-bottom: 10px; }
            .example { color: #4ecdc4; font-family: monospace; }
        </style>
    </head>
    <body>
        <div class="error-container">
            <h1>404 - Device ID Required</h1>
            <p>Please specify a device ID in the URL.</p>
            <p>Example: <span class="example">/android/my-device-123</span></p>
        </div>
    </body>
    </html>
    """
    return HTMLResponse(content=error_html, status_code=404)

@app.get("/viewer")
def get_viewer():
    return FileResponse(os.path.join(static_dir, "viewer.html"))

@app.get("/test")
def get_test():
    return FileResponse(os.path.join(static_dir, "test.html"))

@app.get("/webrtc-client.js")
def get_webrtc_client_js():
    return FileResponse(os.path.join(static_dir, "webrtc-client.js"), media_type="application/javascript")

@app.get("/health")
def health_check():
    return {"status": "healthy", "clients": len(clients), "active_devices": len(active_devices)}

# Create the Socket.IO ASGI app and combine with FastAPI
socket_app = socketio.ASGIApp(sio, other_asgi_app=app, socketio_path="socket.io")

if __name__ == "__main__":
    import uvicorn

    # Cloud Run provides PORT environment variable
    port = int(os.environ.get("PORT", 8000))
    host = "0.0.0.0"  # Required for Cloud Run

    logger.info(f"Starting server on {host}:{port}")
    logger.info("Server optimized for Cloud Run deployment")

    # Cloud Run doesn't need SSL certificates - it handles HTTPS at the load balancer level
    uvicorn.run(
        socket_app,
        host=host,
        port=port,
        log_level="info",
        access_log=True
    )
