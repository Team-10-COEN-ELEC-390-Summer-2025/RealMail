package com.team10.realmail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;


public class MainActivity extends AppCompatActivity {

    private static final Logger logger = Logger.getLogger(MainActivity.class.getName());
    protected Button signIn;
    protected TextView forgotpw, newaccount;
    protected EditText email, password;
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

        // Check if user is already signed in
        // code below works. just commented out for dev purposes.
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        if (user != null) {
//            // User is signed in, go to HomeActivity
//            Intent intent = new Intent(this, HomeActivity.class);
//            startActivity(intent);
//            finish();
//        }

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
                Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();
                new Thread(() -> {
                    sendJWTtoServer(auth.getCurrentUser().getIdToken(false).getResult().getToken());
                }).start(); // Send JWT to server in a separate thread
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Error logging in", Toast.LENGTH_SHORT).show();
            }

        });
    }

    protected void sendJWTtoServer(String jwt_token) {
        // https://firebase.google.com/docs/auth/admin/verify-id-tokens#android

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        user.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
            public void onComplete(@NonNull Task<GetTokenResult> task) {
                if (task.isSuccessful()) {
                    String idToken = task.getResult().getToken();
                    String userUid = FirebaseAuth.getInstance().getUid();
                    // Send token to your backend via HTTPS
                    new Thread(() -> {
                        try {
                            URL url = new URL("https://us-central1-realmail-39ab4.cloudfunctions.net/verifyToken?" + "token=" + jwt_token + "&uid=" + userUid);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST");
                            int statusCode = conn.getResponseCode();
                            logger.info("Status code: " + statusCode);
                            if (statusCode == HttpURLConnection.HTTP_OK) {
                                logger.info("Token sent successfully to server.");
                            } else {
                                logger.warning("Failed to send token to server. Status code: " + statusCode);
                            }
                            // close connection
                            conn.disconnect();


                        } catch (Exception e) {
                            logger.severe("Error sending token to server: " + e.getMessage());
                            Toast.makeText(MainActivity.this, "Unable to send token to server", Toast.LENGTH_SHORT).show();
                        }
                    }).start();
                }

            }
        });
    }


}