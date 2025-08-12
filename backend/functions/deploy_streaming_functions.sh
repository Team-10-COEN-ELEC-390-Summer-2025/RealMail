#!/bin/bash

# Deploy Firebase Functions for RealMail Streaming Service
# This script deploys the updated functions to Firebase

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Deploying RealMail Firebase Functions${NC}"
echo "===================================="
echo

# Check if we're in the right directory
if [ ! -f "package.json" ] || [ ! -d "src" ]; then
    echo -e "${RED}Error: Please run this script from the backend/functions directory${NC}"
    exit 1
fi

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo -e "${RED}Error: Firebase CLI not installed${NC}"
    echo "Install with: npm install -g firebase-tools"
    exit 1
fi

# Install/update dependencies
echo -e "${BLUE}1. Installing dependencies...${NC}"
npm install

# Check for TypeScript errors
echo -e "${BLUE}2. Checking TypeScript compilation...${NC}"
npm run build

if [ $? -ne 0 ]; then
    echo -e "${RED}TypeScript compilation failed. Please fix errors before deploying.${NC}"
    exit 1
fi

# Deploy functions
echo -e "${BLUE}3. Deploying functions to Firebase...${NC}"
firebase deploy --only functions

if [ $? -eq 0 ]; then
    echo
    echo -e "${GREEN}✓ Deployment successful!${NC}"
    echo
    echo -e "${YELLOW}New streaming endpoints:${NC}"
    echo "  Start: https://us-central1-realmail-39ab4.cloudfunctions.net/startStreaming/{device_id}"
    echo "  Stop:  https://us-central1-realmail-39ab4.cloudfunctions.net/stopStreaming/{device_id}"
    echo "  Status: https://us-central1-realmail-39ab4.cloudfunctions.net/getStreamingStatus/{device_id}"
    echo
    echo -e "${YELLOW}Test with curl:${NC}"
    echo "  curl -X POST \"https://us-central1-realmail-39ab4.cloudfunctions.net/startStreaming/your-device-id\""
    echo "  curl \"https://us-central1-realmail-39ab4.cloudfunctions.net/getStreamingStatus/your-device-id\""
else
    echo -e "${RED}✗ Deployment failed!${NC}"
    exit 1
fi
