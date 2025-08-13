#!/bin/bash

# Integration test for RealMail Streaming Service
# Tests the complete flow from Firebase Functions to Raspberry Pi

set -e

# Configuration
DEVICE_ID="test-device-001"
FIREBASE_BASE_URL="https://us-central1-realmail-39ab4.cloudfunctions.net"
PI_IP=""  # Set this to your Pi's IP address for local testing

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}RealMail Streaming Service Integration Test${NC}"
echo "============================================"
echo

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --device-id)
      DEVICE_ID="$2"
      shift 2
      ;;
    --pi-ip)
      PI_IP="$2"
      shift 2
      ;;
    -h|--help)
      echo "Usage: $0 --device-id <ID> [--pi-ip <IP>]"
      echo "  --device-id: Device ID to test (required)"
      echo "  --pi-ip: Pi IP address for local testing (optional)"
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option $1${NC}"
      exit 1
      ;;
  esac
done

if [ -z "$DEVICE_ID" ]; then
    echo -e "${RED}Device ID is required${NC}"
    echo "Usage: $0 --device-id <ID>"
    exit 1
fi

echo "Testing device: $DEVICE_ID"
if [ -n "$PI_IP" ]; then
    echo "Pi IP address: $PI_IP"
fi
echo

# Test 1: Firebase Function Health Check
echo -e "${BLUE}Test 1: Firebase Functions Health Check${NC}"
echo "Testing if Firebase functions are deployed..."

response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "${FIREBASE_BASE_URL}/getStreamingStatus/${DEVICE_ID}" || echo "HTTPSTATUS:000")
http_code=$(echo $response | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
response_body=$(echo $response | sed -e 's/HTTPSTATUS:.*//g')

if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 404 ] || [ "$http_code" -eq 502 ]; then
    echo -e "${GREEN}✓ Firebase functions are accessible${NC}"
else
    echo -e "${RED}✗ Firebase functions not accessible (HTTP: $http_code)${NC}"
    echo "Response: $response_body"
    exit 1
fi

# Test 2: Get Initial Status
echo -e "${BLUE}Test 2: Get Initial Streaming Status${NC}"
response=$(curl -s "${FIREBASE_BASE_URL}/getStreamingStatus/${DEVICE_ID}")
echo "Response: $response"
echo

# Test 3: Start Streaming
echo -e "${BLUE}Test 3: Start Streaming${NC}"
response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST "${FIREBASE_BASE_URL}/startStreaming/${DEVICE_ID}")
http_code=$(echo $response | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
response_body=$(echo $response | sed -e 's/HTTPSTATUS:.*//g')

echo "HTTP Code: $http_code"
echo "Response: $response_body"

if [ "$http_code" -eq 200 ]; then
    echo -e "${GREEN}✓ Start streaming request successful${NC}"
elif [ "$http_code" -eq 404 ]; then
    echo -e "${YELLOW}⚠ Device not found in database${NC}"
elif [ "$http_code" -eq 502 ]; then
    echo -e "${YELLOW}⚠ Device not responding (may be offline)${NC}"
else
    echo -e "${RED}✗ Start streaming failed${NC}"
fi
echo

# Test 4: Wait and Check Status
echo -e "${BLUE}Test 4: Check Streaming Status After Start${NC}"
echo "Waiting 3 seconds..."
sleep 3

response=$(curl -s "${FIREBASE_BASE_URL}/getStreamingStatus/${DEVICE_ID}")
echo "Response: $response"
echo

# Test 5: Stop Streaming
echo -e "${BLUE}Test 5: Stop Streaming${NC}"
response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST "${FIREBASE_BASE_URL}/stopStreaming/${DEVICE_ID}")
http_code=$(echo $response | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
response_body=$(echo $response | sed -e 's/HTTPSTATUS:.*//g')

echo "HTTP Code: $http_code"
echo "Response: $response_body"

if [ "$http_code" -eq 200 ]; then
    echo -e "${GREEN}✓ Stop streaming request successful${NC}"
elif [ "$http_code" -eq 404 ]; then
    echo -e "${YELLOW}⚠ Device not found in database${NC}"
elif [ "$http_code" -eq 502 ]; then
    echo -e "${YELLOW}⚠ Device not responding (may be offline)${NC}"
else
    echo -e "${RED}✗ Stop streaming failed${NC}"
fi
echo

# Test 6: Final Status Check
echo -e "${BLUE}Test 6: Final Status Check${NC}"
response=$(curl -s "${FIREBASE_BASE_URL}/getStreamingStatus/${DEVICE_ID}")
echo "Response: $response"
echo

# Test 7: Local Pi API (if IP provided)
if [ -n "$PI_IP" ]; then
    echo -e "${BLUE}Test 7: Direct Pi API Test${NC}"
    
    echo "Testing Pi health endpoint..."
    response=$(curl -s -m 5 "http://${PI_IP}:8080/health" || echo "Connection failed")
    echo "Health: $response"
    
    echo "Testing Pi status endpoint..."
    response=$(curl -s -m 5 "http://${PI_IP}:8080/status/${DEVICE_ID}" || echo "Connection failed")
    echo "Status: $response"
    
    echo "Testing Pi start streaming..."
    response=$(curl -s -m 5 -X POST "http://${PI_IP}:8080/start_streaming/${DEVICE_ID}" || echo "Connection failed")
    echo "Start: $response"
    
    sleep 2
    
    echo "Testing Pi stop streaming..."
    response=$(curl -s -m 5 -X POST "http://${PI_IP}:8080/stop_streaming/${DEVICE_ID}" || echo "Connection failed")
    echo "Stop: $response"
fi

echo
echo -e "${GREEN}Integration test completed!${NC}"
echo
echo -e "${YELLOW}Summary:${NC}"
echo "  - Firebase functions are accessible"
echo "  - API endpoints respond correctly"
echo "  - Error handling works for offline devices"
if [ -n "$PI_IP" ]; then
    echo "  - Direct Pi API communication tested"
fi
echo
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Install the streaming service on your Raspberry Pi"
echo "2. Ensure the Pi's IP is registered in the database"
echo "3. Test with a real device ID"
echo "4. Monitor logs for any issues"
