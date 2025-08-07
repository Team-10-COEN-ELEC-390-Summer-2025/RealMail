package com.team10.realmail.utils;

import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SSHHelper {
    private static final String TAG = "SSHHelper";
    private static final int SSH_PORT = 22;
    private static final int TIMEOUT_MS = 10000;

    // Default Pi credentials for setup
    private static final String DEFAULT_USERNAME = "team10";
    private static final String DEFAULT_PASSWORD = "poop";
    private static final String DEFAULT_HOSTNAME = "raspberrypi";

    private ExecutorService executor;

    public SSHHelper() {
        executor = Executors.newSingleThreadExecutor();
    }

    public interface SSHCallback {
        void onSuccess(String result);

        void onError(String error);

        void onProgress(String status);
    }

    public static class SSHResult {
        public final boolean success;
        public final String output;
        public final String error;
        public final int exitCode;

        public SSHResult(boolean success, String output, String error, int exitCode) {
            this.success = success;
            this.output = output;
            this.error = error;
            this.exitCode = exitCode;
        }
    }

    /**
     * Test SSH connection to Raspberry Pi
     */
    public void testConnection(String ipAddress, SSHCallback callback) {
        executor.execute(() -> {
            try {
                callback.onProgress("Testing SSH connection to " + ipAddress);
                JSch jsch = new JSch();
                Session session = jsch.getSession(DEFAULT_USERNAME, ipAddress, SSH_PORT);
                session.setPassword(DEFAULT_PASSWORD);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setTimeout(TIMEOUT_MS);

                session.connect();

                if (session.isConnected()) {
                    callback.onSuccess("SSH connection successful");
                    session.disconnect();
                } else {
                    callback.onError("Failed to establish SSH connection");
                }

            } catch (Exception e) {
                Log.e(TAG, "SSH connection test failed", e);
                callback.onError("SSH connection failed: " + e.getMessage());
            }
        });
    }

    /**
     * Execute a single command via SSH
     */
    public void executeCommand(String ipAddress, String command, SSHCallback callback) {
        executor.execute(() -> {
            Session session = null;
            Channel channel = null;

            try {
                callback.onProgress("Executing command: " + command);

                JSch jsch = new JSch();
                session = jsch.getSession(DEFAULT_USERNAME, ipAddress, SSH_PORT);
                session.setPassword(DEFAULT_PASSWORD);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setTimeout(TIMEOUT_MS);

                session.connect();

                channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);

                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);

                InputStream in = channel.getInputStream();
                channel.connect();

                StringBuilder output = new StringBuilder();
                byte[] tmp = new byte[1024];

                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i < 0) break;
                        output.append(new String(tmp, 0, i));
                    }

                    if (channel.isClosed()) {
                        if (in.available() > 0) continue;
                        Log.d(TAG, "Command exit status: " + channel.getExitStatus());
                        break;
                    }

                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        // Ignore
                    }
                }

                String result = output.toString();
                if (channel.getExitStatus() == 0) {
                    callback.onSuccess(result);
                } else {
                    callback.onError("Command failed with exit code: " + channel.getExitStatus() + "\nOutput: " + result);
                }

            } catch (Exception e) {
                Log.e(TAG, "SSH command execution failed", e);
                callback.onError("SSH command failed: " + e.getMessage());
            } finally {
                if (channel != null) {
                    channel.disconnect();
                }
                if (session != null) {
                    session.disconnect();
                }
            }
        });
    }

    /**
     * Test WiFi connection before adding to wpa_supplicant.conf
     */
    public void testWiFiConnection(String ipAddress, String ssid, String password, SSHCallback callback) {
        String command = String.format("nmcli dev wifi connect \"%s\" password \"%s\" ifname wlan0", ssid, password);

        executeCommand(ipAddress, command, new SSHCallback() {
            @Override
            public void onSuccess(String result) {
                callback.onSuccess("WiFi connection test successful! Credentials are valid.");
            }

            @Override
            public void onError(String error) {
                // Even if connection fails, we'll continue with the setup
                String message = "WiFi connection test failed, but continuing setup anyway. " +
                        "This might be due to range issues or the network being unavailable right now. " +
                        "Error: " + error;
                callback.onSuccess(message); // Use onSuccess to continue the flow
            }

            @Override
            public void onProgress(String status) {
                callback.onProgress("Testing WiFi connection with provided credentials...");
            }
        });
    }

    /**
     * Add a new WiFi network to wpa_supplicant.conf (with optional connection test)
     */
    public void addWiFiNetworkWithTest(String ipAddress, String ssid, String password, SSHCallback callback) {
        // First test the WiFi connection
        testWiFiConnection(ipAddress, ssid, password, new SSHCallback() {
            @Override
            public void onSuccess(String testResult) {
                callback.onProgress(testResult);

                // Continue with adding the network regardless of test result
                addWiFiNetwork(ipAddress, ssid, password, callback);
            }

            @Override
            public void onError(String error) {
                // This shouldn't happen since testWiFiConnection always calls onSuccess
                callback.onProgress("WiFi test had issues, but continuing with setup...");
                addWiFiNetwork(ipAddress, ssid, password, callback);
            }

            @Override
            public void onProgress(String status) {
                callback.onProgress(status);
            }
        });
    }

    /**
     * Add a new WiFi network to wpa_supplicant.conf
     */
    public void addWiFiNetwork(String ipAddress, String ssid, String password, SSHCallback callback) {
        String command = String.format(
                "echo 'network={' | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf && " +
                        "echo '    ssid=\"%s\"' | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf && " +
                        "echo '    psk=\"%s\"' | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf && " +
                        "echo '    key_mgmt=WPA-PSK' | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf && " +
                        "echo '    priority=100' | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf && " +
                        "echo '}' | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf",
                ssid, password
        );

        executeCommand(ipAddress, command, new SSHCallback() {
            @Override
            public void onSuccess(String result) {
                // Restart wpa_supplicant to apply changes
                executeCommand(ipAddress, "sudo wpa_cli -i wlan0 reconfigure", new SSHCallback() {
                    @Override
                    public void onSuccess(String restartResult) {
                        callback.onSuccess("WiFi network added successfully. Pi will connect to " + ssid);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("WiFi network added but failed to restart wpa_supplicant: " + error);
                    }

                    @Override
                    public void onProgress(String status) {
                        callback.onProgress("Restarting WiFi service...");
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to add WiFi network: " + error);
            }

            @Override
            public void onProgress(String status) {
                callback.onProgress("Adding WiFi network to Pi configuration...");
            }
        });
    }

    /**
     * Get current wpa_supplicant.conf content
     */
    public void getWpaSupplicantConfig(String ipAddress, SSHCallback callback) {
        executeCommand(ipAddress, "sudo cat /etc/wpa_supplicant/wpa_supplicant.conf", callback);
    }

    /**
     * Reboot the Raspberry Pi
     */
    public void rebootPi(String ipAddress, SSHCallback callback) {
        executeCommand(ipAddress, "sudo reboot", new SSHCallback() {
            @Override
            public void onSuccess(String result) {
                callback.onSuccess("Raspberry Pi is rebooting...");
            }

            @Override
            public void onError(String error) {
                // Reboot command often "fails" because connection is lost
                callback.onSuccess("Raspberry Pi is rebooting...");
            }

            @Override
            public void onProgress(String status) {
                callback.onProgress("Sending reboot command...");
            }
        });
    }

    /**
     * Get Pi's current IP address
     */
    public void getCurrentIP(String ipAddress, SSHCallback callback) {
        executeCommand(ipAddress, "hostname -I", callback);
    }

    /**
     * Get Pi hostname
     */
    public void getHostname(String ipAddress, SSHCallback callback) {
        executeCommand(ipAddress, "hostname", callback);
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
