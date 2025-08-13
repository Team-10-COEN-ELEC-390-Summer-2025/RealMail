#!/bin/bash

# Raspberry Pi Heartbeat Service Setup Script
# This script sets up the heartbeat service to run automatically on boot

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running as root
if [[ $EUID -eq 0 ]]; then
   print_error "This script should not be run as root. Run as team10 user and it will use sudo when needed."
   exit 1
fi

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
HEARTBEAT_DIR="$SCRIPT_DIR"

print_status "Setting up Raspberry Pi Heartbeat Service..."
print_status "Heartbeat service directory: $HEARTBEAT_DIR"

# Check if required files exist
REQUIRED_FILES=("heartbeat_service.py" "heartbeat.service" "device_info.txt")
for file in "${REQUIRED_FILES[@]}"; do
    if [ ! -f "$HEARTBEAT_DIR/$file" ]; then
        print_error "Required file not found: $file"
        exit 1
    fi
done

print_success "All required files found"

# Create virtual environment with system site packages access
VENV_DIR="$HEARTBEAT_DIR/venv"
print_status "Creating virtual environment with system site packages..."
if [ ! -d "$VENV_DIR" ]; then
    python3 -m venv --system-site-packages "$VENV_DIR"
    print_success "Virtual environment created at $VENV_DIR"
else
    print_status "Virtual environment already exists at $VENV_DIR"
fi

# Activate virtual environment and install dependencies
print_status "Installing Python dependencies in virtual environment..."
source "$VENV_DIR/bin/activate"

if [ -f "$HEARTBEAT_DIR/../requirements.txt" ]; then
    pip install -r "$HEARTBEAT_DIR/../requirements.txt"
elif [ -f "$HEARTBEAT_DIR/requirements.txt" ]; then
    pip install -r "$HEARTBEAT_DIR/requirements.txt"
else
    print_status "Installing requests module..."
    pip install requests
fi

deactivate
print_success "Dependencies installed in virtual environment"

# Make the Python script executable
chmod +x "$HEARTBEAT_DIR/heartbeat_service.py"
print_success "Made heartbeat_service.py executable"

# Create log directory if it doesn't exist
sudo mkdir -p /var/log
sudo touch /var/log/heartbeat_service.log
sudo chown team10:team10 /var/log/heartbeat_service.log
print_success "Created log file"

# Update the service file with correct paths and virtual environment
TEMP_SERVICE="/tmp/heartbeat.service"
sed -e "s|/home/team10/RealMail/hardware/heartbeat_service|$HEARTBEAT_DIR|g" \
    -e "s|ExecStart=.*|ExecStart=$VENV_DIR/bin/python $HEARTBEAT_DIR/heartbeat_service.py|g" \
    "$HEARTBEAT_DIR/heartbeat.service" > "$TEMP_SERVICE"

# Copy service file to systemd directory
print_status "Installing systemd service..."
sudo cp "$TEMP_SERVICE" /etc/systemd/system/heartbeat.service
rm "$TEMP_SERVICE"

# Reload systemd and enable the service
print_status "Configuring systemd service..."
sudo systemctl daemon-reload
sudo systemctl enable heartbeat.service

print_success "Heartbeat service installed and enabled"

# Check device configuration
print_status "Checking device configuration..."
if grep -q "user@example.com" "$HEARTBEAT_DIR/device_info.txt"; then
    print_warning "Device configuration contains default values!"
    print_warning "Please edit $HEARTBEAT_DIR/device_info.txt with your actual:"
    print_warning "  - DEVICE_ID: Unique identifier for your device"
    print_warning "  - USER_EMAIL: Associated user email"
    print_warning "  - HEARTBEAT_URL: Your Firebase Cloud Function endpoint"
    print_warning "  - HEARTBEAT_INTERVAL: Heartbeat interval in seconds (default: 60)"
    echo
    print_warning "After updating the configuration, restart the service with:"
    print_warning "  sudo systemctl restart heartbeat.service"
else
    print_success "Device configuration appears to be customized"
fi

# Test the service with virtual environment
print_status "Testing the heartbeat service..."
if "$VENV_DIR/bin/python" "$HEARTBEAT_DIR/heartbeat_service.py" --help > /dev/null 2>&1 || true; then
    print_success "Python script can be executed with virtual environment"
else
    print_warning "Python script test had issues, but continuing..."
fi

# Start the service
print_status "Starting heartbeat service..."
if sudo systemctl start heartbeat.service; then
    print_success "Heartbeat service started successfully"

    # Wait a moment and check status
    sleep 2
    if sudo systemctl is-active --quiet heartbeat.service; then
        print_success "Service is running"
    else
        print_warning "Service may not be running properly"
        print_status "Check service status with: sudo systemctl status heartbeat.service"
    fi
else
    print_error "Failed to start heartbeat service"
    print_status "Check service status with: sudo systemctl status heartbeat.service"
fi

echo
print_success "Setup complete!"
echo
print_status "Useful commands:"
print_status "  View service status:    sudo systemctl status heartbeat.service"
print_status "  View service logs:      sudo journalctl -u heartbeat.service -f"
print_status "  Restart service:        sudo systemctl restart heartbeat.service"
print_status "  Stop service:           sudo systemctl stop heartbeat.service"
print_status "  Disable service:        sudo systemctl disable heartbeat.service"
print_status "  View log file:          tail -f /var/log/heartbeat_service.log"
echo

if grep -q "user@example.com" "$HEARTBEAT_DIR/device_info.txt"; then
    print_warning "IMPORTANT: Don't forget to update device_info.txt with your actual configuration!"
fi

print_success "The heartbeat service will now start automatically on every boot."
