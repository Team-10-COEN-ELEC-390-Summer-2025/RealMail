package com.example.realmail;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
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
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // FCM SDK (and your app) can post notifications.
                } else {
                    // Inform user that your app will not show notifications.
                }
            });
    protected Button signIn;
    protected TextView forgotpw, newaccount;
    protected EditText email, password;
    // declaration below for allow notifications
    // see https://firebase.google.com/docs/cloud-messaging/android/client
    String global_device_registration_token = "";
    private FirebaseAuth auth;

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
        signIn = findViewById(R.id.signin);
        forgotpw = findViewById(R.id.forgotpw);
        newaccount = findViewById(R.id.newaccount);
        email = findViewById(R.id.email_sign_in);
        password = findViewById(R.id.password_sign_in);

        // Ask for notification permission if necessary
        askNotificationPermission();

        // Create notification channel
        // ref: https://developer.android.com/develop/ui/views/notifications/channels
        createNotificationChannel();



        signIn.setOnClickListener(v -> {
            signIn();
        });
        forgotpw.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ForgotPassword.class);
            startActivity(intent);
        });

        newaccount.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewAccount.class);
            startActivity(intent);
        });

    }

    private void signIn() {
        auth = FirebaseAuth.getInstance();

        //extract the email & pw
        String email1 = email.getText().toString();
        String password1 = password.getText().toString();

        auth.signInWithEmailAndPassword(email1, password1).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Get new FCM registration token
                // https://firebase.google.com/docs/cloud-messaging/android/client#retrieve-the-current-registration-token
                getDeviceRegistrationToken(); // Retrieve the current FCM registration token
                String userEmail = task.getResult().getUser().getEmail();
                new Thread(() -> {
                    // Async Send the registration token to server
                    try {
                        sendRegistrationToServer(global_device_registration_token, userEmail);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Handle exceptions, such as network errors or JSON parsing errors
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error sending registration token: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }).start();
                Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Error logging in", Toast.LENGTH_SHORT).show();
                return;
            }

        });
    }

    private void getDeviceRegistrationToken() {
        // This method is used to retrieve the current FCM registration token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }
                        // Get new FCM registration token
                        global_device_registration_token = task.getResult();
                    }
                });
    }

    private void sendRegistrationToServer(@NonNull String token, @NonNull String email) throws Exception {
        // send token to server via api call
        // This method should be implemented to send the FCM token to your server
        // For example, you can use Retrofit or any HTTP client to make a POST request
        // to your server's endpoint that handles token registration.

        // how to access device registration token
        // https://firebase.google.com/docs/cloud-messaging/android/client#sample-register
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token_ = task.getResult();
                        new Thread(() -> {
                            try {
                                String stringURL = "https://us-central1-realmail-39ab4.cloudfunctions.net/getDeviceRegistrationToken?token=" + token_ + "&email=" + email;
                                URL url = new URL(stringURL);
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("POST");
                                connection.setDoOutput(false); // post request via params
                                int responseCode = connection.getResponseCode();
                                // below read the response from the server
                                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                String inputLine;
                                StringBuilder response = new StringBuilder();
                                while ((inputLine = in.readLine()) != null) {
                                    response.append(inputLine);
                                }
                                in.close();
                                // Optionally log or use the response
                                connection.disconnect();
                            } catch (Exception e) {
                                e.printStackTrace();
                                // Handle exceptions, such as network errors or JSON parsing errors
                                Log.w("Main.Activity", "Error sending registration token to server: " + e.getMessage());
                            }
                        }).start();
                    }
                });


    }
    private void createNotificationChannel() {
        // // ref: https://developer.android.com/develop/ui/views/notifications/channels
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "default_channel_id"; // Define your channel ID
            CharSequence name = getString(R.string.channel_name); // Add channel_name to strings.xml
            String description = getString(R.string.channel_description); // Add channel_description to strings.xml
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }


}