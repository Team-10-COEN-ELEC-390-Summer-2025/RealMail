package com.team10.realmail;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Session duration: 1 day in milliseconds
    private static final long SESSION_DURATION = 24 * 60 * 60 * 1000; // 1 day
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String LAST_LOGIN_TIME = "last_login_time";
    private static final String SESSION_EXPIRY = "session_expiry";
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted");
                    getDeviceRegistrationToken();
                    Toast.makeText(this, "Notifications enabled for new mail alerts", Toast.LENGTH_SHORT).show();
                } else {
                    Log.w(TAG, "Notification permission denied");
                    Toast.makeText(this, "Notifications disabled - you won't receive mail alerts", Toast.LENGTH_LONG).show();
                }
            });
    protected Button signIn;//declared variable
    protected TextView forgotpw, newaccount, lastLoginText;
    protected EditText email, password;
    String global_device_registration_token = "";
    private FirebaseAuth auth; // getting the instance from firebase
    private SharedPreferences loginPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Initialize SharedPreferences for session management
        loginPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        //link the id to the layout the objects
        signIn = findViewById(R.id.signin);
        forgotpw = findViewById(R.id.forgotpw);
        newaccount = findViewById(R.id.newaccount);
        lastLoginText = findViewById(R.id.last_login_text); // Add this to your layout
        email = findViewById(R.id.email_sign_in);
        password = findViewById(R.id.password_sign_in);

        askNotificationPermission();
        createNotificationChannel();

        auth = FirebaseAuth.getInstance();

        // Check if user is already logged in and session is valid
        checkExistingSession();

        // Display last login time
        displayLastLoginTime();

        signIn.setOnClickListener(v -> {
            signIn(); //run signin function
        });
        forgotpw.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ForgotPassword.class);//change the activity from  main to forgot pw
            startActivity(intent);
        });

        newaccount.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewAccount.class);// change the acti from main to newacc
            startActivity(intent);
        });

    }


    /**
     * Check if user has a valid existing session
     */
    private void checkExistingSession() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            // Check if session hasn't expired
            long sessionExpiry = loginPrefs.getLong(SESSION_EXPIRY, 0);
            long currentTime = System.currentTimeMillis();

            if (currentTime < sessionExpiry) {
                // Session is still valid, go directly to HomeActivity
                Log.d(TAG, "Valid session found, redirecting to HomeActivity");
                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();

                // Update device registration token for notifications
                getDeviceRegistrationToken();

                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish(); // Don't allow back to login screen
                return;
            } else {
                // Session expired, sign out the user
                Log.d(TAG, "Session expired, signing out user");
                auth.signOut();
                clearSessionData();
            }
        }

        // If no valid session, stay on login screen
        Log.d(TAG, "No valid session found, showing login screen");
    }

    /**
     * Display last login time if available
     */
    private void displayLastLoginTime() {
        long lastLoginTime = loginPrefs.getLong(LAST_LOGIN_TIME, 0);

        if (lastLoginTime > 0 && lastLoginText != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(lastLoginTime));
            lastLoginText.setText("Last login: " + formattedDate);
            lastLoginText.setVisibility(TextView.VISIBLE);
        } else if (lastLoginText != null) {
            lastLoginText.setVisibility(TextView.GONE);
        }
    }

    /**
     * Save session data after successful login
     */
    private void saveSessionData() {
        long currentTime = System.currentTimeMillis();
        long expiryTime = currentTime + SESSION_DURATION;

        SharedPreferences.Editor editor = loginPrefs.edit();
        editor.putLong(LAST_LOGIN_TIME, currentTime);
        editor.putLong(SESSION_EXPIRY, expiryTime);
        editor.apply();

        Log.d(TAG, "Session data saved. Expires at: " + new Date(expiryTime));
    }

    /**
     * Clear session data on logout or expiry
     */
    private void clearSessionData() {
        SharedPreferences.Editor editor = loginPrefs.edit();
        editor.remove(SESSION_EXPIRY);
        // Keep last login time for display purposes
        editor.apply();

        Log.d(TAG, "Session data cleared");
    }


    //signin function
    private void signIn() {
        auth = FirebaseAuth.getInstance();

        String email1 = email.getText().toString().trim(); //get the content from the email edit text,to store in email
        String password1 = password.getText().toString().trim(); //get the content from the pw edit text,to store in pw

        // Add input validation
        if (email1.isEmpty()) {
            email.setError("Email is required");
            email.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email1).matches()) {
            email.setError("Please enter a valid email");
            email.requestFocus();
            return;
        }

        if (password1.isEmpty()) {
            password.setError("Password is required");
            password.requestFocus();
            return;
        }

        // Disable sign-in button to prevent multiple submissions
        signIn.setEnabled(false);
        signIn.setText("Signing in...");

        //signin attempt to firebase with the email and password
        auth.signInWithEmailAndPassword(email1, password1).addOnCompleteListener(this, task -> {
            // Re-enable button regardless of outcome
            signIn.setEnabled(true);
            signIn.setText("Sign In");

            if (task.isSuccessful()) {
                // Save session data for persistence
                saveSessionData();

                getDeviceRegistrationToken();
                String userEmail = task.getResult().getUser().getEmail();

                // Only send registration token if we have one
                if (!global_device_registration_token.isEmpty()) {
                    new Thread(() -> {
                        try {
                            sendRegistrationToServer(global_device_registration_token, userEmail);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "Error sending registration token: " + e.getMessage());
                        }
                    }).start();
                } else {
                    Log.w(TAG, "Registration token not available yet, will retry later");
                }

                Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);//change the activity from main to home
                startActivity(intent);
                finish(); // Prevent going back to login screen
            } else {
                String errorMessage = "Error logging in";
                if (task.getException() != null) {
                    errorMessage = task.getException().getMessage();
                }
                Log.e(TAG, "Sign-in failed", task.getException());
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }

        });
    }

    private void getDeviceRegistrationToken() {

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        global_device_registration_token = task.getResult();
                    }
                });
    }

    private void sendRegistrationToServer(@NonNull String token, @NonNull String email) {
        // Use the token parameter directly instead of fetching again
        new Thread(() -> {
            try {
                String stringURL = "https://us-central1-realmail-39ab4.cloudfunctions.net/getDeviceRegistrationToken?token=" + token + "&email=" + email;
                URL url = new URL(stringURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(false);
                connection.setConnectTimeout(10000); // 10 second timeout
                connection.setReadTimeout(10000); // 10 second read timeout

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Registration token server response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    Log.d(TAG, "Registration token sent successfully");
                } else {
                    Log.w(TAG, "Failed to send registration token. Response code: " + responseCode);
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error sending registration token to server: " + e.getMessage());
                // Optionally show user a toast on UI thread
                runOnUiThread(() -> {
                    // Only show error if it's a critical failure
                    if (e instanceof java.net.SocketTimeoutException) {
                        Toast.makeText(MainActivity.this, "Network timeout - registration will retry later", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "default";  // Match the service channel ID
            CharSequence name = "Mail Notifications";
            String description = "Notifications for new mail";
            int importance = NotificationManager.IMPORTANCE_HIGH;  // Higher importance for mail notifications
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.enableVibration(true);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Permission already granted, get FCM token
                getDeviceRegistrationToken();
                Log.d(TAG, "Notification permission already granted");
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Show rationale and request permission
                Toast.makeText(this, "Notifications are needed to alert you about new mail", Toast.LENGTH_LONG).show();
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Request permission directly
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // For older Android versions, get token directly since permission is granted at install time
            getDeviceRegistrationToken();
        }
    }


}