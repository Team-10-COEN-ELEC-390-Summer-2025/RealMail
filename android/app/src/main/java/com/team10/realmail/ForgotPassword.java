package com.team10.realmail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {
    protected EditText email;
    protected TextView backtologin;
    protected Button reset;

    private FirebaseAuth auth; // get firebase instance authencation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //auto generated 27-34
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // instancating ui components
        backtologin = findViewById(R.id.backtologin);
        email = findViewById(R.id.email_forgot); //edit
        reset = findViewById(R.id.reset_password);

        auth = FirebaseAuth.getInstance();//get instance in firebase

        //sent reset email when click on reset button
        reset.setOnClickListener(v -> {
            String email1 = email.getText().toString(); //store email string

            //if email is empty send a msg
            if (email1.isEmpty()) {
                Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show();
            }
            //send reset password email which is entered
            auth.sendPasswordResetEmail(email1).addOnCompleteListener(task -> {
                //if task is successful ,send msg
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Reset password email sent", Toast.LENGTH_SHORT).show();
                }
            });
            //every time click on back to login
            backtologin.setOnClickListener(V -> {
                Intent intent = new Intent(ForgotPassword.this, MainActivity.class);
                startActivity(intent); //switch one activity to another,forgot pw go bk to main acti

            });
        });
    }
}
