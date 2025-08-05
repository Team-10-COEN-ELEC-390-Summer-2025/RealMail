#!/bin/bash

declare -A wifis
wifis["setmeup123"]="setmeup123"
wifis["KG Cell"]="Soup1234"
wifis["SM-S921W1498"]="skce7421"
wifis["390"]="Helloworld"
wifis["1234567890"]="1234567890"
wifis["1502"]="Project2025"
wifis["Test"]="testingtesting"

for SSID in "${!wifis[@]}"; do
    PASSWORD="${wifis[$SSID]}"
    echo "Adding Wi-Fi: $SSID"
    nmcli dev wifi connect "$SSID" password "$PASSWORD" ifname wlan0
done