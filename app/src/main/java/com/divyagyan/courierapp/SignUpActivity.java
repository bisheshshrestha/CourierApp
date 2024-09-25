package com.divyagyan.courierapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {
    private EditText usernameEditText, passwordEditText, phoneEditText, emailEditText, addressEditText;
    private Button registerButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        usernameEditText = findViewById(R.id.userNameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        addressEditText = findViewById(R.id.addressEditText);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txtUserName = usernameEditText.getText().toString().trim();
                String txtPassword = passwordEditText.getText().toString().trim();
                String txtPhone = phoneEditText.getText().toString().trim();
                String txtEmail = emailEditText.getText().toString().trim();
                String txtAddress = addressEditText.getText().toString().trim();

                if (validateInput(txtUserName, txtPassword, txtEmail, txtPhone, txtAddress)) {
                    progressBar.setVisibility(View.VISIBLE);

                    mAuth.createUserWithEmailAndPassword(txtEmail, txtPassword)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        User user = new User(txtUserName, txtPassword, txtEmail, txtPhone, txtAddress);
                                        FirebaseDatabase.getInstance().getReference("Users")
                                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            Toast.makeText(SignUpActivity.this, "User Registered Successfully", Toast.LENGTH_LONG).show();
                                                            progressBar.setVisibility(View.GONE);
                                                            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                                                        } else {
                                                            Toast.makeText(SignUpActivity.this, "User Failed to Register", Toast.LENGTH_LONG).show();
                                                            progressBar.setVisibility(View.GONE);
                                                        }
                                                    }
                                                });

                                    } else {
                                        Toast.makeText(SignUpActivity.this, "User Failed to Register: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        progressBar.setVisibility(View.GONE);
                                    }
                                }
                            });
                }
            }
        });
    }

    // Method to validate input fields
    private boolean validateInput(String userName, String password, String email, String phone, String address) {
        if (userName.isEmpty()) {
            usernameEditText.setError("Please enter a username");
            usernameEditText.requestFocus();
            return false;
        }

        if (password.isEmpty() || password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return false;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return false;
        }

        if (phone.isEmpty() || !phone.matches("\\d{10}")) {
            phoneEditText.setError("Please enter a valid 10-digit phone number");
            phoneEditText.requestFocus();
            return false;
        }

        if (address.isEmpty()) {
            addressEditText.setError("Please enter an address");
            addressEditText.requestFocus();
            return false;
        }

        return true; // All validations passed
    }
}
