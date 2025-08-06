# WebRTC Camera Server

A real-time camera streaming server built with FastAPI and Socket.IO that enables live video broadcasting from camera devices to multiple viewers. Perfect for security cameras, live streaming, or remote monitoring applications.

## 🚀 Features

- **Real-time Video Streaming**: Live camera feed broadcasting using WebRTC
- **Multiple Viewers**: Support for multiple simultaneous viewers
- **Mobile Optimized**: Responsive design with dedicated Android viewer
- **Auto-connect**: Automatic connection for seamless viewing experience
- **HTTPS Support**: Secure connections required for camera access
- **Cloud Ready**: Optimized for Google Cloud Run deployment

## 🛠 Tech Stack

### Backend
- **Python 3.13** - Runtime environment
- **FastAPI** - Modern web framework for APIs
- **Socket.IO** - Real-time bidirectional communication
- **Uvicorn** - ASGI server for production

### Frontend
- **WebRTC** - Peer-to-peer video streaming
- **Socket.IO Client** - Real-time communication
- **Vanilla JavaScript** - No framework dependencies
- **Responsive CSS** - Mobile-first design

### Infrastructure
- **Google Cloud Run** - Serverless container platform
- **Docker** - Containerization
- **Cloud Build** - CI/CD pipeline

## 📁 Project Structure

```
WebRTC-server/
├── main.py                 # Main FastAPI application
├── requirements.txt        # Python dependencies
├── startup.sh             # Startup script for different environments
├── Dockerfile             # Container configuration
├── Procfile               # Process file for Cloud Run
├── cloudbuild.yaml        # Cloud Build configuration
├── app.yaml               # App Engine configuration (alternative)
├── run_https.py           # HTTPS development server
├── src/                   # Static files and web pages
│   ├── test.html          # Testing interface (camera + viewer)
│   ├── streamer.html      # Camera streaming interface
│   ├── android.html       # Mobile viewer (auto-connect)
│   ├── viewer.html        # Desktop viewer interface
│   └── webrtc-client.js   # WebRTC client library
└── README.md              # This file
```

## 🚀 Quick Start

### Local Development

1. **Clone and setup**:
   ```bash
   git clone <repository-url>
   cd WebRTC-server
   ```

2. **Install dependencies**:
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   ```

3. **Start development server with HTTPS** (required for camera access):
   ```bash
   ENV=dev bash startup.sh
   ```
   
   This starts the server at `https://127.0.0.1:8443`
   
   **Note**: Accept the self-signed certificate warning in your browser.

4. **Alternative - HTTP server** (limited functionality):
   ```bash
   bash startup.sh
   ```
   
   This starts at `http://localhost:8080` (camera won't work without HTTPS)

### Cloud Deployment

Deploy to Google Cloud Run with a single command:

```bash
gcloud run deploy camera-webrtc \
--source . \
--region us-central1 \
--platform managed \
--allow-unauthenticated \
--clear-base-image
```

After deployment, your server will be available at:
`https://camera-webrtc-[hash]-uc.a.run.app`

## 📱 How to Use

### For Camera/Streamer

1. **Open streamer interface**:
   - Local: `https://127.0.0.1:8443/streamer`
   - Cloud: `https://your-cloud-run-url/streamer`

2. **Start streaming**:
   - Click "Start Streaming"
   - Allow camera permissions when prompted
   - Your camera feed will begin broadcasting

3. **Stop streaming**:
   - Click "Stop Streaming" to end the broadcast

### For Viewers

#### Option 1: Auto-connect Mobile Viewer (Recommended for Android)
- **URL**: `/android`
- **Features**: Automatically connects to available camera, fullscreen support, touch controls
- **Perfect for**: Embedding in Android apps, mobile viewing

#### Option 2: Manual Viewer
- **URL**: `/viewer` or `/test`
- **Features**: Manual connection control, desktop optimized
- **Perfect for**: Desktop viewing, testing

### Testing Interface
- **URL**: `/test`
- **Features**: Can act as both camera and viewer on same page
- **Perfect for**: Development, testing WebRTC functionality

## 🔧 Configuration Files

### startup.sh Script

The startup script handles different deployment environments:

```bash
# Development with HTTPS (required for camera)
ENV=dev bash startup.sh

# Production HTTP
bash startup.sh

# Docker/Cloud Run (automatic)
# Detects container environment and uses production settings
```

**Environment Variables**:
- `ENV=dev` - Enables HTTPS development server
- `PORT` - Server port (defaults: 8443 for dev, 8080 for prod)
- `HOST` - Server host (127.0.0.1 for dev, 0.0.0.0 for prod)

### Dockerfile

Optimized for Cloud Run with Python 3.13:
- Multi-stage build for smaller images
- Non-root user for security
- Health checks included
- Proper dependency caching

### Cloud Build (cloudbuild.yaml)

Automated CI/CD pipeline:
1. Builds Docker container
2. Pushes to Container Registry
3. Deploys to Cloud Run
4. Configures scaling and resources

## 🌐 API Endpoints

### HTTP Routes
- `GET /` - Test interface
- `GET /streamer` - Camera streaming page
- `GET /android` - Mobile viewer (auto-connect)
- `GET /viewer` - Desktop viewer
- `GET /test` - Testing interface
- `GET /webrtc-client.js` - WebRTC client library
- `GET /health` - Health check endpoint

### WebSocket Events (Socket.IO)
- `register` - Register as 'camera' or 'viewer'
- `offer` - WebRTC offer from camera
- `answer` - WebRTC answer from viewer
- `ice-candidate` - ICE candidates for connection
- `camera-available` - Notify viewers camera is online
- `no-camera` - Notify viewers no camera available

## 🔒 Security & Requirements

### HTTPS Requirement
- **Camera access requires HTTPS** due to browser security policies
- Local development uses self-signed certificates
- Cloud Run provides automatic HTTPS

### Browser Compatibility
- **Chrome/Chromium** - Full support
- **Firefox** - Full support  
- **Safari** - Full support
- **Mobile browsers** - Full support with HTTPS

### Permissions
- Camera access permission required for streaming
- Microphone access for audio streaming (optional)

## 🐳 Docker Support

Build and run locally:

```bash
# Build image
docker build -t webrtc-camera-server .

# Run container
docker run -p 8000:8000 webrtc-camera-server
```

## 🔍 Troubleshooting

### Common Issues

1. **Camera access denied**:
   - Ensure you're using HTTPS
   - Check browser permissions
   - Try different browsers

2. **Connection issues**:
   - Check firewall settings
   - Verify Socket.IO connection in browser console
   - Test with `/health` endpoint

3. **Cloud Run deployment fails**:
   - Ensure `requirements.txt` is not empty
   - Check Docker build logs
   - Verify project permissions

### Debug Mode

Enable verbose logging:
```bash
# Set debug environment
export DEBUG=true
python main.py
```

## 📊 Monitoring

### Health Check
- Endpoint: `/health`
- Returns: Server status, client count, camera availability

### Logs
- Cloud Run: View in Google Cloud Console
- Local: Check terminal output for WebRTC events

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Test locally with HTTPS
4. Deploy to Cloud Run for testing
5. Submit pull request

## 📄 License

This project is licensed under the MIT License.

---

**Need help?** Check the troubleshooting section or open an issue with:
- Browser type and version
- Error messages from console
- Network environment (local/cloud)
- Steps to reproduce the issue
