package com.divyagyan.adminapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends Activity {

    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    private ProgressBar progressBar;
    private Button signInButton;
    private CheckBox staySignedInCheckBox;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Auth and preferences
        mAuth = FirebaseAuth.getInstance();
        preferences = getSharedPreferences("com.divyagyan.adminapp", MODE_PRIVATE);

        // Reference views
        emailEditText = findViewById(R.id.emailSignInEditText);
        passwordEditText = findViewById(R.id.passwordSignInEditText);
        progressBar = findViewById(R.id.loginProgressBar);
        signInButton = findViewById(R.id.userSignInButton);
        staySignedInCheckBox = findViewById(R.id.staySignedInCheckBox);

        signInButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (!email.isEmpty() && !password.isEmpty()) {
                loginAdmin(email, password);
            } else {
                Toast.makeText(SignInActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginAdmin(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            String userEmail = user.getEmail();

                            // Check if the signed-in email is "admin@gmail.com"
                            if (userEmail != null && userEmail.equals("admin@gmail.com")) {
                                Toast.makeText(SignInActivity.this, "Admin Login Successful", Toast.LENGTH_SHORT).show();

                                // Save "Stay signed in" preference
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putBoolean("staySignedIn", staySignedInCheckBox.isChecked());
                                editor.apply();

                                // Redirect to MainActivity
                                Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // Sign out the user if the email is not "admin@gmail.com"
                                mAuth.signOut();
                                Toast.makeText(SignInActivity.this, "Access Denied: Only admin can log in", Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        Toast.makeText(SignInActivity.this, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    protected void onStart() {
        super.onStart();

        // Check if "Stay signed in" is enabled
        boolean staySignedIn = preferences.getBoolean("staySignedIn", false);
        if (staySignedIn && mAuth.getCurrentUser() != null) {
            // Redirect to MainActivity if user is already signed in
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}