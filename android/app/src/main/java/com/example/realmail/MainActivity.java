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
                    //push notification
                } else {
                    //notification not shown
                }
            });
    protected Button signIn;
    protected TextView forgotpw, newaccount;
    protected EditText email, password;
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

        askNotificationPermission();

        createNotificationChannel();

        auth = FirebaseAuth.getInstance();

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

        String email1 = email.getText().toString();
        String password1 = password.getText().toString();

        auth.signInWithEmailAndPassword(email1, password1).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {

                getDeviceRegistrationToken();
                String userEmail = task.getResult().getUser().getEmail();
                new Thread(() -> {

                    try {
                        sendRegistrationToServer(global_device_registration_token, userEmail);
                    } catch (Exception e) {
                        e.printStackTrace();

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

    private void sendRegistrationToServer(@NonNull String token, @NonNull String email) throws Exception {

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        String token_ = task.getResult();
                        new Thread(() -> {
                            try {
                                String stringURL = "https://us-central1-realmail-39ab4.cloudfunctions.net/getDeviceRegistrationToken?token=" + token_ + "&email=" + email;
                                URL url = new URL(stringURL);
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("POST");
                                connection.setDoOutput(false);
                                int responseCode = connection.getResponseCode();
                                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                String inputLine;
                                StringBuilder response = new StringBuilder();
                                while ((inputLine = in.readLine()) != null) {
                                    response.append(inputLine);
                                }
                                in.close();
                                connection.disconnect();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.w("Main.Activity", "Error sending registration token to server: " + e.getMessage());
                            }
                        }).start();
                    }
                });


    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "default_channel_id";
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {

            } else {

                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }


}