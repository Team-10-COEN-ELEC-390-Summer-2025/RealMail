package com.example.realmail;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class NewAccount extends AppCompatActivity {

    protected Button submit;
    protected EditText email, password, cpassword;

    private FirebaseAuth auth;

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
        //firebase ojects
        auth = FirebaseAuth.getInstance();//get instance

        submit = findViewById(R.id.submit);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        cpassword = findViewById(R.id.cpassword);

        //when we submit we call creat function
        submit.setOnClickListener(v -> {
            createAccount();
        });
    }

    private void createAccount(){
        String email1 = email.getText().toString();
        //get email,extracting content,storing in email,pw and cpw
        String password1 = password.getText().toString();
        String cpassword1 = cpassword.getText().toString();

        //pw not match with cpw
        if(!password1.equals(cpassword1)){
            Toast.makeText(this, "Passwords don't match!", Toast.LENGTH_SHORT).show();
            return;
        }
        //for creating a new account in the database
        auth.createUserWithEmailAndPassword(email1, password1).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()){
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(NewAccount.this, MainActivity.class);
                startActivity(intent); //go the the main activity
            }
            else {
                Toast.makeText(this, "Error creating account", Toast.LENGTH_SHORT).show();
                return;
            }
        });

    }
}