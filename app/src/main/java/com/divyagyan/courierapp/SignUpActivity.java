package com.divyagyan.courierapp;

import android.content.Intent;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SignUpActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText, phoneEditText, emailEditText, addressEditText;
    private TextView usernameErrorTextView, emailErrorTextView; // Error TextViews
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
        usernameErrorTextView = findViewById(R.id.usernameErrorTextView); // Initialize error TextView
        emailErrorTextView = findViewById(R.id.emailErrorTextView); // Initialize error TextView
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear previous error messages
                usernameErrorTextView.setVisibility(View.GONE);
                emailErrorTextView.setVisibility(View.GONE);

                String txtUserName = usernameEditText.getText().toString().trim();
                String txtPassword = passwordEditText.getText().toString().trim();
                String txtPhone = phoneEditText.getText().toString().trim();
                String txtEmail = emailEditText.getText().toString().trim();
                String txtAddress = addressEditText.getText().toString().trim();

                if (validateInput(txtUserName, txtPassword, txtEmail, txtPhone, txtAddress)) {
                    progressBar.setVisibility(View.VISIBLE);
                    checkIfUserExists(txtUserName, txtEmail, txtPassword, txtPhone, txtAddress);
                }
            }
        });
    }

    private void checkIfUserExists(String username, String email, String password, String phone, String address) {
        FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("userName").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            progressBar.setVisibility(View.GONE);
                            showError(usernameErrorTextView, "Username already taken");
                        } else {
                            checkIfEmailExists(username, email, password, phone, address);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(SignUpActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIfEmailExists(String username, String email, String password, String phone, String address) {
        FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            progressBar.setVisibility(View.GONE);
                            showError(emailErrorTextView, "Email already registered");
                        } else {
                            registerUser(username, password, email, phone, address);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(SignUpActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUser(String username, String password, String email, String phone, String address) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            User user = new User(username, password, email, phone, address);
                            FirebaseDatabase.getInstance().getReference("users")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            progressBar.setVisibility(View.GONE);
                                            if (task.isSuccessful()) {
                                                Toast.makeText(SignUpActivity.this, "User Registered Successfully", Toast.LENGTH_LONG).show();
                                                startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                                            } else {
                                                Toast.makeText(SignUpActivity.this, "User Failed to Register", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(SignUpActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private boolean validateInput(String userName, String password, String email, String phone, String address) {
        if (userName.isEmpty()) {
            showError(usernameErrorTextView, "Please enter a username");
            return false;
        }

        if (password.isEmpty() || password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return false;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(emailErrorTextView, "Please enter a valid email");
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

    private void showError(TextView errorTextView, String message) {
        errorTextView.setText(message);
        errorTextView.setVisibility(View.VISIBLE);
    }
}
