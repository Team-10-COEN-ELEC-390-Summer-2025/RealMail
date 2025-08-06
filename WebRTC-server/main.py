import ssl
from fastapi import FastAPI
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
import os
from typing import Dict
import logging
import socketio

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

# Store connected clients
clients = {}
active_camera = None

# Socket.IO events
@sio.event
async def connect(sid, environ):
    logger.info(f"Client connected: {sid}")

@sio.event
async def disconnect(sid):
    logger.info(f"Client disconnected: {sid}")
    if sid in clients:
        client_type = clients[sid]['type']
        del clients[sid]

        global active_camera
        if sid == active_camera:
            active_camera = None
            logger.info('Active camera disconnected')
            # Notify all viewers that the camera is gone
            for viewer_sid, client in clients.items():
                if client['type'] == 'viewer':
                    await sio.emit('no-camera', room=viewer_sid)

@sio.event
async def register(sid, data):
    client_type = data
    clients[sid] = {'type': client_type, 'sid': sid}
    logger.info(f"{client_type} registered: {sid}")

    global active_camera
    if client_type == 'camera':
        active_camera = sid
        logger.info(f'Camera became active: {sid}')
        # Notify viewers that camera is available
        for viewer_sid, client in clients.items():
            if client['type'] == 'viewer':
                await sio.emit('camera-available', room=viewer_sid)
        # Ask camera to create offer
        await sio.emit('create-offer', room=sid)

    elif client_type == 'viewer':
        if active_camera and active_camera in clients:
            logger.info(f'Camera available for new viewer: {sid}')
            await sio.emit('camera-available', room=sid)
            # Ask camera to create offer for this viewer
            await sio.emit('create-offer', room=active_camera)
        else:
            await sio.emit('no-camera', room=sid)

@sio.event
async def offer(sid, data):
    logger.info(f'Received offer from camera: {sid}')
    # Forward offer to all viewers
    for viewer_sid, client in clients.items():
        if client['type'] == 'viewer':
            logger.info(f'Sending offer to viewer: {viewer_sid}')
            await sio.emit('offer', data, room=viewer_sid)

@sio.event
async def answer(sid, data):
    logger.info(f'Received answer from viewer: {sid}')
    # Send answer to the active camera
    global active_camera
    if active_camera and active_camera in clients:
        logger.info(f'Sending answer to camera: {active_camera}')
        await sio.emit('answer', data, room=active_camera)

@sio.event
async def ice_candidate(sid, data):
    logger.info(f'Received ICE candidate from: {sid}')
    client_type = clients.get(sid, {}).get('type')

    if client_type == 'camera':
        # Send to all viewers
        for viewer_sid, client in clients.items():
            if client['type'] == 'viewer':
                await sio.emit('ice-candidate', data, room=viewer_sid)
    elif client_type == 'viewer':
        # Send to camera
        global active_camera
        if active_camera:
            await sio.emit('ice-candidate', data, room=active_camera)

# FastAPI routes
@app.get("/")
def root():
    return FileResponse(os.path.join(static_dir, "test.html"))

@app.get("/streamer")
def get_streamer():
    return FileResponse(os.path.join(static_dir, "streamer.html"))

@app.get("/android")
def get_android():
    return FileResponse(os.path.join(static_dir, "android.html"))

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
    return {"status": "healthy", "clients": len(clients), "active_camera": active_camera is not None}

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
