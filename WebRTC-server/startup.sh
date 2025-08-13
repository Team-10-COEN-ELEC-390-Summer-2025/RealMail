#!/bin/bash

# WebRTC Server Startup Script
echo "Starting WebRTC Server..."

# Check if running in Docker
if [ -f /.dockerenv ]; then
    echo "Running in Docker container"

    # Production settings for Docker
    export PORT=${PORT:-8000}
    export HOST="0.0.0.0"
    export DEBUG=false

    echo "Starting production server on $HOST:$PORT..."
    python main.py
    exit 0
fi

# For non-Docker environments:
# Install dependencies if needed
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment
source venv/bin/activate

# Install requirements
echo "Installing dependencies..."
pip install -r requirements.txt

# Set environment variables
if [ "$ENV" == "dev" ]; then
    # Development settings with HTTPS
    export PORT=8443
    export HOST="127.0.0.1"
    export DEBUG=true

    echo "Starting development server with HTTPS on https://$HOST:$PORT"
    echo "Note: Camera access requires HTTPS. Make sure to accept the self-signed certificate."
    python run_https.py
else
    # Production settings
    export PORT=${PORT:-8080}
    export HOST="0.0.0.0"
    export DEBUG=false

    echo "Starting production server on $HOST:$PORT..."
    python main.py
fi
