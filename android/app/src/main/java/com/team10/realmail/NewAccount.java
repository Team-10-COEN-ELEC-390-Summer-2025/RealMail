package com.team10.realmail;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NewAccount extends AppCompatActivity {

    protected Button submit;
    protected EditText email, password, cpassword, firstName, lastName;

    private FirebaseAuth auth;

    private FirebaseFirestore database;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();//get instance
        submit = findViewById(R.id.button_create_account);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        cpassword = findViewById(R.id.cpassword);
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        submit.setOnClickListener(v -> {
            createAccount();
        });
    }

    //functions
    private void createAccount() {
        String email1 = email.getText().toString().trim();
        String password1 = password.getText().toString().trim();
        String cpassword1 = cpassword.getText().toString().trim();
        String firstName1 = firstName.getText().toString().trim();
        String lastName1 = lastName.getText().toString().trim();

        // Add comprehensive input validation
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

        if (password1.length() < 6) {
            password.setError("Password must be at least 6 characters");
            password.requestFocus();
            return;
        }

        if (cpassword1.isEmpty()) {
            cpassword.setError("Please confirm your password");
            cpassword.requestFocus();
            return;
        }

        if (firstName1.isEmpty()) {
            firstName.setError("First name is required");
            firstName.requestFocus();
            return;
        }

        if (lastName1.isEmpty()) {
            lastName.setError("Last name is required");
            lastName.requestFocus();
            return;
        }

        //checking passwords
        if (!password1.equals(cpassword1)) {
            cpassword.setError("Passwords don't match!");
            cpassword.requestFocus();
            Toast.makeText(this, "Passwords don't match!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable submit button to prevent multiple submissions
        submit.setEnabled(false);
        submit.setText("Creating Account...");

        // create user with the email and password,completelisen means once task complete do someth
        auth.createUserWithEmailAndPassword(email1, password1).addOnCompleteListener(this, task -> {
            // Re-enable button regardless of outcome
            submit.setEnabled(true);
            submit.setText("Create Account");

            if (task.isSuccessful()) {
                //get the currentuser
                FirebaseUser user = auth.getCurrentUser();

                if (user != null) {
                    database = FirebaseFirestore.getInstance();
                    //get the user id
                    String userID = user.getUid();

                    //create the map object,database saved in firestore
                    Map<String, Object> map = new HashMap<>();

                    map.put("firstName", firstName1);
                    map.put("lastName", lastName1);

                    //path in firestore
                    database.collection("users")
                            .document(userID)
                            .collection("User Info")
                            .document("User Names")
                            //store the map in firestore
                            .set(map)

                            .addOnSuccessListener(aVoid -> {
                                //add the log,
                                Log.d("Firestore", "User info successfully saved");
                                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(NewAccount.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent); //go the the main activity
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Saving user info failed", e);
                                Toast.makeText(this, "Account created but failed to save user info", Toast.LENGTH_LONG).show();
                                // Still redirect to main activity since auth was successful
                                Intent intent = new Intent(NewAccount.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                } else {
                    Log.e("NewAccount", "User is null after successful authentication");
                    Toast.makeText(this, "Error: User authentication failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                String errorMessage = "Error creating account";
                if (task.getException() != null) {
                    errorMessage = task.getException().getMessage();
                }
                Log.e("NewAccount", "Account creation failed", task.getException());
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

    }

    private void saveInfo() {

    }
}