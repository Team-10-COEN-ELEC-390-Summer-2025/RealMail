package com.example.realmail;

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

public class MainActivity extends AppCompatActivity {

    protected Button signIn;
    protected TextView forgotpw,newaccount;
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


        signIn.setOnClickListener(v -> {
            signIn();
        });
        forgotpw.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ForgotPassword.class);
            startActivity(intent);
        });

        newaccount.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this,NewAccount.class);
            startActivity(intent);
        });

    }

    private void signIn(){
        auth = FirebaseAuth.getInstance();

        //extract the email & pw
        String email1 = email.getText().toString();
        String password1 = password.getText().toString();

        auth.signInWithEmailAndPassword(email1, password1).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()){
                Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
            }
            else {
                Toast.makeText(this, "Error logging in", Toast.LENGTH_SHORT).show();
                return;
            }

        });
    }

}