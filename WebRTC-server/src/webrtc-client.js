// WebRTC Client Implementation
class WebRTCClient {
    constructor(socketUrl) {
        console.log('Initializing WebRTC Client with URL:', socketUrl);

        // Check WebRTC support immediately
        if (!this.checkWebRTCSupport()) {
            this.updateStatus('WebRTC not supported in this browser');
            return;
        }

        this.socket = io(socketUrl, {
            transports: ['websocket', 'polling'],
            timeout: 20000,
            forceNew: true
        });

        this.remoteVideo = document.getElementById('remoteVideo');
        this.localVideo = document.getElementById('localVideo');
        this.cameraBtn = document.getElementById('cameraBtn');
        this.viewerBtn = document.getElementById('viewerBtn');
        this.status = document.getElementById('status');

        this.peerConnection = null;
        this.localStream = null;
        this.isCamera = false;
        this.isStreaming = false;

        // Enhanced media constraints with fallbacks
        this.mediaConstraints = {
            video: {
                facingMode: { ideal: "environment" },
                width: { ideal: 1280, min: 640 },
                height: { ideal: 720, min: 480 },
                frameRate: { ideal: 30, min: 15 }
            },
            audio: {
                echoCancellation: true,
                noiseSuppression: true,
                autoGainControl: true
            }
        };

        this.config = {
            iceServers: [
                { urls: 'stun:stun.l.google.com:19302' },
                { urls: 'stun:stun1.l.google.com:19302' },
                { urls: 'stun:stun2.l.google.com:19302' }
            ]
        };

        this.init();
    }

    checkWebRTCSupport() {
        // Check for basic WebRTC support
        if (!window.RTCPeerConnection) {
            console.error('RTCPeerConnection not supported');
            return false;
        }

        // Check for getUserMedia support
        if (!navigator.mediaDevices) {
            console.error('navigator.mediaDevices not supported');
            // Try older getUserMedia API
            navigator.mediaDevices = {};
            if (navigator.getUserMedia) {
                navigator.mediaDevices.getUserMedia = function(constraints) {
                    return new Promise((resolve, reject) => {
                        navigator.getUserMedia(constraints, resolve, reject);
                    });
                };
            } else if (navigator.webkitGetUserMedia) {
                navigator.mediaDevices.getUserMedia = function(constraints) {
                    return new Promise((resolve, reject) => {
                        navigator.webkitGetUserMedia(constraints, resolve, reject);
                    });
                };
            } else if (navigator.mozGetUserMedia) {
                navigator.mediaDevices.getUserMedia = function(constraints) {
                    return new Promise((resolve, reject) => {
                        navigator.mozGetUserMedia(constraints, resolve, reject);
                    });
                };
            } else {
                console.error('getUserMedia not supported');
                return false;
            }
        }

        if (!navigator.mediaDevices.getUserMedia) {
            console.error('getUserMedia not available');
            return false;
        }

        // Check if running over HTTPS (required for camera access)
        if (location.protocol !== 'https:' && location.hostname !== 'localhost' && location.hostname !== '127.0.0.1') {
            console.warn('Camera access requires HTTPS');
            this.updateStatus('⚠️ Camera requires HTTPS. Please use https:// or localhost');
            return false;
        }

        return true;
    }

    init() {
        // Set up event listeners only if buttons exist
        if (this.cameraBtn) {
            this.cameraBtn.onclick = () => this.toggleCamera();
        }
        if (this.viewerBtn) {
            this.viewerBtn.onclick = () => this.startAsViewer();
        }

        // Socket event listeners
        this.setupSocketListeners();
    }

    setupSocketListeners() {
        this.socket.on('connect', () => {
            console.log('Connected to server');
            this.updateStatus('Connected to server');
        });

        this.socket.on('disconnect', () => {
            console.log('Disconnected from server');
            this.updateStatus('Disconnected from server');
        });

        this.socket.on('camera-available', () => {
            console.log('Camera is available');
            this.updateStatus('Camera available - connecting...');
        });

        this.socket.on('no-camera', () => {
            console.log('No camera available');
            this.updateStatus('No camera available - waiting...');
        });

        this.socket.on('create-offer', async () => {
            console.log('Creating offer...');
            if (this.isCamera && this.peerConnection) {
                await this.createOffer();
            }
        });

        this.socket.on('offer', async (offer) => {
            console.log('Received offer');
            if (!this.isCamera) {
                await this.handleOffer(offer);
            }
        });

        this.socket.on('answer', async (answer) => {
            console.log('Received answer');
            if (this.isCamera && this.peerConnection) {
                await this.peerConnection.setRemoteDescription(answer);
            }
        });

        this.socket.on('ice-candidate', async (candidate) => {
            console.log('Received ICE candidate');
            if (this.peerConnection && candidate) {
                try {
                    await this.peerConnection.addIceCandidate(candidate);
                } catch (error) {
                    console.error('Error adding ICE candidate:', error);
                }
            }
        });

        this.socket.on('connect_error', (error) => {
            console.error('Connection error:', error);
            this.updateStatus('Connection error - retrying...');
        });
    }

    async toggleCamera() {
        if (this.isStreaming) {
            await this.stopCamera();
        } else {
            await this.startAsCamera();
        }
    }

    async startAsCamera() {
        try {
            console.log('Starting as camera...');
            this.updateStatus('Requesting camera access...');

            // Check permissions first
            if (!this.checkWebRTCSupport()) {
                throw new Error('WebRTC not supported or HTTPS required');
            }

            // Try to get user media with fallback constraints
            this.localStream = await this.getUserMedia();

            if (this.localVideo) {
                this.localVideo.srcObject = this.localStream;
                this.localVideo.muted = true; // Prevent feedback
                this.localVideo.style.display = 'block';
            }

            this.isCamera = true;
            this.isStreaming = true;
            this.socket.emit('register', 'camera');

            if (this.cameraBtn) {
                this.cameraBtn.textContent = 'Stop Camera';
                this.cameraBtn.style.backgroundColor = '#dc3545';
            }

            this.updateStatus('Camera started - waiting for viewers...');
            await this.createPeerConnection();

        } catch (error) {
            console.error('Error starting camera:', error);

            let errorMessage = 'Camera error: ';
            if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
                errorMessage += 'Camera permission denied. Please allow camera access and try again.';
            } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
                errorMessage += 'No camera found. Please connect a camera device.';
            } else if (error.name === 'NotSupportedError') {
                errorMessage += 'Camera not supported on this device.';
            } else if (error.name === 'NotReadableError') {
                errorMessage += 'Camera is being used by another application.';
            } else {
                errorMessage += error.message;
            }

            this.updateStatus(errorMessage);
            this.isStreaming = false;
            if (this.cameraBtn) {
                this.cameraBtn.textContent = 'Start Camera';
                this.cameraBtn.style.backgroundColor = '#007bff';
            }
        }
    }

    async getUserMedia() {
        // Try different constraint combinations
        const constraints = [
            this.mediaConstraints,
            { video: true, audio: true },
            { video: { facingMode: "environment" }, audio: false },
            { video: true, audio: false },
            { video: { facingMode: "user" }, audio: false },
            { video: { width: 640, height: 480 }, audio: false }
        ];

        for (let i = 0; i < constraints.length; i++) {
            try {
                console.log(`Trying constraint ${i + 1}:`, constraints[i]);
                const stream = await navigator.mediaDevices.getUserMedia(constraints[i]);
                console.log('Successfully got media stream');
                return stream;
            } catch (error) {
                console.warn(`Failed with constraint ${i + 1}:`, error);
                if (i === constraints.length - 1) {
                    throw error; // Re-throw the last error
                }
                continue;
            }
        }
    }

    async stopCamera() {
        try {
            console.log('Stopping camera...');

            if (this.localStream) {
                this.localStream.getTracks().forEach(track => {
                    track.stop();
                    console.log('Stopped track:', track.kind);
                });
                this.localStream = null;
            }

            if (this.localVideo) {
                this.localVideo.srcObject = null;
                this.localVideo.style.display = 'none';
            }

            if (this.peerConnection) {
                this.peerConnection.close();
                this.peerConnection = null;
            }

            this.isCamera = false;
            this.isStreaming = false;

            if (this.cameraBtn) {
                this.cameraBtn.textContent = 'Start Camera';
                this.cameraBtn.style.backgroundColor = '#007bff';
            }

            this.updateStatus('Camera stopped');

        } catch (error) {
            console.error('Error stopping camera:', error);
        }
    }

    async startAsViewer() {
        try {
            console.log('Starting as viewer...');
            this.updateStatus('Connecting as viewer...');

            this.isCamera = false;
            this.socket.emit('register', 'viewer');
            this.updateStatus('Registered as viewer - looking for camera...');

            await this.createPeerConnection();

        } catch (error) {
            console.error('Error starting as viewer:', error);
            this.updateStatus(`Viewer error: ${error.message}`);
        }
    }

    async createPeerConnection() {
        try {
            this.peerConnection = new RTCPeerConnection(this.config);

            // Add local stream if camera
            if (this.isCamera && this.localStream) {
                this.localStream.getTracks().forEach(track => {
                    this.peerConnection.addTrack(track, this.localStream);
                    console.log('Added track to peer connection:', track.kind);
                });
            }

            // Handle remote stream
            this.peerConnection.ontrack = (event) => {
                console.log('Received remote track');
                if (this.remoteVideo && event.streams[0]) {
                    this.remoteVideo.srcObject = event.streams[0];
                    this.updateStatus('Connected - receiving video');
                }
            };

            // Handle ICE candidates
            this.peerConnection.onicecandidate = (event) => {
                if (event.candidate) {
                    console.log('Sending ICE candidate');
                    this.socket.emit('ice-candidate', event.candidate);
                }
            };

            // Handle connection state
            this.peerConnection.onconnectionstatechange = () => {
                console.log('Connection state:', this.peerConnection.connectionState);
                if (this.peerConnection.connectionState === 'connected') {
                    this.updateStatus('WebRTC connected');
                } else if (this.peerConnection.connectionState === 'failed') {
                    this.updateStatus('Connection failed - retrying...');
                }
            };

        } catch (error) {
            console.error('Error creating peer connection:', error);
            throw error;
        }
    }

    async createOffer() {
        try {
            console.log('Creating offer...');
            const offer = await this.peerConnection.createOffer();
            await this.peerConnection.setLocalDescription(offer);

            console.log('Sending offer to viewers');
            this.socket.emit('offer', offer);
            this.updateStatus('Offer sent to viewers');

        } catch (error) {
            console.error('Error creating offer:', error);
            this.updateStatus('Error creating offer');
        }
    }

    async handleOffer(offer) {
        try {
            console.log('Handling offer...');
            await this.peerConnection.setRemoteDescription(offer);

            const answer = await this.peerConnection.createAnswer();
            await this.peerConnection.setLocalDescription(answer);

            console.log('Sending answer');
            this.socket.emit('answer', answer);
            this.updateStatus('Answer sent - connecting...');

        } catch (error) {
            console.error('Error handling offer:', error);
            this.updateStatus('Error handling offer');
        }
    }

    updateStatus(message) {
        console.log('Status:', message);
        if (this.status) {
            this.status.textContent = message;
        }
    }

    setMediaConstraints(constraints) {
        this.mediaConstraints = constraints;
    }

    static isSupported() {
        return !!(navigator.mediaDevices &&
                 navigator.mediaDevices.getUserMedia &&
                 window.RTCPeerConnection);
    }
}

// Auto-initialize if in browser environment
if (typeof window !== 'undefined') {
    window.WebRTCClient = WebRTCClient;
}
