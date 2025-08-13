#!/bin/bash

# Interactive RealMail Streaming API Test Script

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
BASE_URL="https://us-central1-realmail-39ab4.cloudfunctions.net"

echo -e "${BLUE}RealMail Streaming API Interactive Test${NC}"
echo "======================================"
echo

# Get device ID from user
if [ -z "$1" ]; then
    echo -n "Enter device ID: "
    read DEVICE_ID
else
    DEVICE_ID=$1
fi

if [ -z "$DEVICE_ID" ]; then
    echo -e "${RED}Device ID is required${NC}"
    exit 1
fi

echo -e "${YELLOW}Testing device: $DEVICE_ID${NC}"
echo

# Function to make API call and display result
make_api_call() {
    local method=$1
    local endpoint=$2
    local description=$3
    
    echo -e "${BLUE}$description${NC}"
    echo "URL: $BASE_URL/$endpoint"
    echo
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" "$BASE_URL/$endpoint")
    else
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X "$method" "$BASE_URL/$endpoint")
    fi
    
    http_code=$(echo $response | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
    response_body=$(echo $response | sed -e 's/HTTPSTATUS:.*//g')
    
    echo "HTTP Status: $http_code"
    echo "Response:"
    echo "$response_body" | python3 -m json.tool 2>/dev/null || echo "$response_body"
    echo
    
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓ Success${NC}"
    elif [ "$http_code" = "404" ]; then
        echo -e "${YELLOW}⚠ Device not found${NC}"
    elif [ "$http_code" = "502" ]; then
        echo -e "${YELLOW}⚠ Device not responding${NC}"
    else
        echo -e "${RED}✗ Error${NC}"
    fi
    echo
}

# Interactive menu
while true; do
    echo -e "${YELLOW}Choose an action:${NC}"
    echo "1) Get streaming status"
    echo "2) Start streaming"
    echo "3) Stop streaming"
    echo "4) Test all endpoints"
    echo "5) Change device ID"
    echo "6) Exit"
    echo
    echo -n "Select option (1-6): "
    read choice
    echo
    
    case $choice in
        1)
            make_api_call "GET" "getStreamingStatus/$DEVICE_ID" "Getting streaming status..."
            ;;
        2)
            make_api_call "POST" "startStreaming/$DEVICE_ID" "Starting streaming..."
            ;;
        3)
            make_api_call "POST" "stopStreaming/$DEVICE_ID" "Stopping streaming..."
            ;;
        4)
            echo -e "${BLUE}Testing all endpoints for device: $DEVICE_ID${NC}"
            echo "=" $(printf '=%.0s' {1..50})
            echo
            
            make_api_call "GET" "getStreamingStatus/$DEVICE_ID" "1. Getting initial status..."
            
            make_api_call "POST" "startStreaming/$DEVICE_ID" "2. Starting streaming..."
            
            echo "Waiting 3 seconds..."
            sleep 3
            
            make_api_call "GET" "getStreamingStatus/$DEVICE_ID" "3. Getting status after start..."
            
            make_api_call "POST" "stopStreaming/$DEVICE_ID" "4. Stopping streaming..."
            
            make_api_call "GET" "getStreamingStatus/$DEVICE_ID" "5. Getting final status..."
            ;;
        5)
            echo -n "Enter new device ID: "
            read DEVICE_ID
            if [ -z "$DEVICE_ID" ]; then
                echo -e "${RED}Device ID cannot be empty${NC}"
            else
                echo -e "${GREEN}Device ID updated to: $DEVICE_ID${NC}"
            fi
            echo
            ;;
        6)
            echo -e "${GREEN}Goodbye!${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid option. Please choose 1-6.${NC}"
            echo
            ;;
    esac
    
    echo -e "${YELLOW}Press Enter to continue...${NC}"
    read
    echo
done
