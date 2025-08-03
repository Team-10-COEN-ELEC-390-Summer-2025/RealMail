## Purchase Before Working
1. Raspberry Pi compatible camera, such as the imx219
2. 5V 2A Power Source
3. OpenPIR Motion Sensor

## Install Before Working
1. Make sure that Python, Bonjour Print Service, and Raspberry Pi Imager onto your computer before starting.
    https://www.python.org/downloads/
    https://www.raspberrypi.com/software/
    https://support.apple.com/en-us/106380

2. Connect your micro SD card to your computer. Make sure that the drive is empty.

3. From the installation menu, do the following:
    Raspberry Pi Device: RASPBERRY PI ZERO
    Operating System: Raspberry Pi OS (Debian Bullseye)
    Storage: SDHC Card

4. Edit OS Customisation settings and do the following: 
    Service Tab: 
        Enable SSH
        Use password authentication
    General Tab: 
        Set hostname:
            raspberrypi.local
        Set username and Password
            Username: team10
            Password: elec390
    
5. Press Save. Press Yes. Press Write. Wait for the message telling you to remove your SD Card.

6. Insert the micro SD card into the Raspberrypi. Connect a monitor, a keyboard, and a mouse to the Raspberrypi. Connect the power cable and your Raspberrypi will boot up!

7. Enter the following commands on your Raspberry Pi terminal
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y python3-pip python3-venv python3-rpi.gpio libcamera-apps
sudo apt install -y git build-essential cmake libmicrohttpd-dev libjansson-dev \
libssl-dev libsrtp2-dev libsofia-sip-ua-dev libglib2.0-dev libopus-dev \
libogg-dev libcurl4-openssl-dev liblua5.3-dev libconfig-dev pkg-config
git clone https://github.com/meetecho/janus-gateway.git
cd janus-gateway
sh autogen.sh
sudo apt install -y libnice-dev
sudo apt install -y libwebsockets-dev
./configure --prefix=/opt/janus
make
sudo make install
sudo make configs
python3 -m venv firebase-env
source ~/firebase-env/bin/activate
pip install --upgrade pip
pip install --upgrade pip setuptools wheel
pip install requests
pip install RPi.GPIO
sudo apt install python3-picamera2
deactivate
sudo nano /boot/firmware/congfig.txt
```

8. Edit the config.txt thusly:
    Change the automatic camera detection to: camera_auto_detect=0
    Add this to the bottom of the file: dtoverlay=imx219

9. Reboot your Rasberry Pi

10. Activate firebase-env, then run code.