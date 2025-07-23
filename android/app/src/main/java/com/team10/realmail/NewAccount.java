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

import com.google.firebase.Firebase;
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

    private void createAccount() {
        String email1 = email.getText().toString();
        String password1 = password.getText().toString();
        String cpassword1 = cpassword.getText().toString();
        String firstName1 = firstName.getText().toString();
        String lastName1 = lastName.getText().toString();

        if (!password1.equals(cpassword1)) {
            Toast.makeText(this, "Passwords don't match!", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email1, password1).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                database = FirebaseFirestore.getInstance();
                String userID = user.getUid();

                Map<String, Object> map = new HashMap<>();

                map.put("firstName", firstName1);
                map.put("lastName", lastName1);

                database.collection("users")
                        .document(userID)
                        .collection("User Info")
                        .document("User Names")
                        .set(map)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "First name successfully saved");
                        })
                        .addOnFailureListener(e -> {
                            Log.d("Firestore", "Saving failed " + e);
                        });

                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(NewAccount.this, MainActivity.class);
                startActivity(intent); //go the the main activity
            } else {
                Toast.makeText(this, "Error creating account", Toast.LENGTH_SHORT).show();
                return;
            }
        });

    }

    private void saveInfo() {

    }
}