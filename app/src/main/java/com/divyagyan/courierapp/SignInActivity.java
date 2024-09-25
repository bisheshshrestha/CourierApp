package com.divyagyan.courierapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity {
    private EditText emailSignInEditText, passwordSignInEditText;
    private Button userSignInButton;
    private TextView forgotPasswordTextView, signUpTextView;
    private ProgressBar loginProgressBar;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();

        emailSignInEditText = findViewById(R.id.emailSignInEditText);
        passwordSignInEditText = findViewById(R.id.passwordSignInEditText);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        signUpTextView = findViewById(R.id.signUpTextView);
        userSignInButton = findViewById(R.id.userSignInButton);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        // Set click listener for sign-in button
        userSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailSignInEditText.getText().toString().trim();
                String password = passwordSignInEditText.getText().toString().trim();

                // Full validation
                if (validateInput(email, password)) {
                    loginProgressBar.setVisibility(View.VISIBLE);
                    signInUser(email, password);
                }
            }
        });

        // Set click listener for forgot password
        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        // Set click listener for sign up
        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    // Method to validate email and password input
    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            emailSignInEditText.setError("Email is required");
            emailSignInEditText.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailSignInEditText.setError("Please enter a valid email address");
            emailSignInEditText.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            passwordSignInEditText.setError("Password is required");
            passwordSignInEditText.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            passwordSignInEditText.setError("Password must be at least 6 characters");
            passwordSignInEditText.requestFocus();
            return false;
        }

        return true; // All validations passed
    }

    // Method to sign in the user using Firebase authentication
    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                loginProgressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        String uid = user.getUid(); // Get the unique user ID (UID)

                        // Save the UID in SharedPreferences
                        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("user_uid", uid);
                        editor.apply();

                        // Navigate to Dashboard
                        Intent intent = new Intent(SignInActivity.this, DashboardActivity.class);
                        startActivity(intent);

                        Toast.makeText(SignInActivity.this, "User Successfully Signed In", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SignInActivity.this, "Failed to Sign In: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
