#!/bin/bash

SSIDS=(
    "setmeup123"
    "SM-S921W1498"
    "KG Cell"
    "1502"
    "390"
    "1234567890"
    "Test"
)
PASSWORDS=(
    "setmeup123"
    "skce7421"
    "Soup1234"
    "Project1502"
    "Helloworld"
    "1234567890"
    "testingtesting"
)

echo "🔧 Disconnecting existing Wi-Fi..."
nmcli radio wifi off
sleep 2
nmcli radio wifi on
sleep 2

echo "📡 Searching for Wi-Fi networks..."
nmcli dev wifi list

success=0

for i in "${!SSIDS[@]}"; do
    SSID="${SSIDS[$i]}"
    PASSWORD="${PASSWORDS[$i]}"
    echo "📄 Attempting to connect to SSID: $SSID"

    nmcli dev wifi connect "$SSID" password "$PASSWORD" ifname wlan0
    sleep 7

    # Get the currently connected SSID
    CURRENT_SSID=$(nmcli -t -f active,ssid dev wifi | grep '^yes' | cut -d: -f2)

    if [[ "$CURRENT_SSID" == "$SSID" ]]; then
        IP=$(hostname -I | awk '{print $1}')
        echo "✅ Connected to $SSID with IP: $IP"
        success=1
        break
    else
        echo "❌ Failed to connect to $SSID. Trying next..."
    fi
done

if [ $success -eq 0 ]; then
    echo "❌ Could not connect to any Wi-Fi network."
fi