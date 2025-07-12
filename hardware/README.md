# RealMail

## Install for Hardware
1. Make sure that Node.js, Python, Bonjour Print Service, Putty, and Raspberry Pi Imager onto your computer before starting.
    https://nodejs.org/en/download
    https://www.python.org/downloads/
    https://www.raspberrypi.com/software/
    https://www.putty.org/
    https://support.apple.com/en-us/106380

2. Open your VSCode terminal and write the following dependencies:
```bash
npm install
npm install onoff axios
sudo node pir.js
```

3. Install the following extension to VSCode:
Remote - SSH

4. Connect your micro SD card to your computer. Make sure that the drive is empty.

5. From the installation menu, do the following:
    Raspberry Pi Device: RASPBERRY PI ZERO
    Operating System: Raspberry Pi OS (Debian Bullseye)
    Storage: SDHC Card

6. Edit OS Customisation settings and do the following: 
    Service Tab: 
        Enable SSH
        Use password authentication
    General Tab: 
        Set hostname:
            raspberrypi.local
        Set username and Password
            Username: team10
            Password: elec390
    
7. Press Save. Press Yes. Press Write. Wait for the message telling you to remove your SD Card.

8. Insert the micro SD card into the Raspberrypi. Connect a monitor, a keyboard, and a mouse to the Raspberrypi. Connect the power cable and your Raspberrypi will boot up!

9. Sart coding through the Rasperberrypi terminal.
