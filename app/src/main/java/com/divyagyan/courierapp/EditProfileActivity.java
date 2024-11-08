package com.divyagyan.courierapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.divyagyan.courierapp.databinding.ActivityEditProfileBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private DatabaseReference userDatabaseRef;
    private String userUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Retrieve user UID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userUid = sharedPreferences.getString("user_uid", null);

        if (userUid == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userDatabaseRef = FirebaseDatabase.getInstance().getReference("users").child(userUid);

        loadUserData();

        binding.saveProfileButton.setOnClickListener(v -> saveProfileChanges());
    }

    private void loadUserData() {
        userDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("userName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);

                    // Pre-fill the EditText fields with the fetched data
                    binding.usernameEditText.setText(username);
                    binding.emailEditText.setText(email);
                    binding.phoneEditText.setText(phone);
                    binding.addressEditText.setText(address);
                } else {
                    Toast.makeText(EditProfileActivity.this, "Failed to load user data!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfileChanges() {
        String newUsername = binding.usernameEditText.getText().toString().trim();
        String newEmail = binding.emailEditText.getText().toString().trim();
        String newPhone = binding.phoneEditText.getText().toString().trim();
        String newAddress = binding.addressEditText.getText().toString().trim();

        // Perform validations
        if (!isValidInput(newUsername, newEmail, newPhone, newAddress)) {
            return; // Stop execution if validation fails
        }

        // Check if the username is already taken by another user
        FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("userName").equalTo(newUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean usernameTaken = false;
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            String uid = userSnapshot.getKey();
                            if (!uid.equals(userUid)) {  // Check if it’s a different user
                                usernameTaken = true;
                                break;
                            }
                        }

                        if (usernameTaken) {
                            binding.usernameEditText.setError("Username already taken");
                            binding.usernameEditText.requestFocus();
                        } else {
                            // Username is available; proceed to update profile
                            updateUserProfile(newUsername, newEmail, newPhone, newAddress);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(EditProfileActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserProfile(String newUsername, String newEmail, String newPhone, String newAddress) {
        // Update the user profile in the database
        userDatabaseRef.child("userName").setValue(newUsername);
        userDatabaseRef.child("email").setValue(newEmail);
        userDatabaseRef.child("phone").setValue(newPhone);
        userDatabaseRef.child("address").setValue(newAddress);

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
        finish(); // Optionally, close the activity after saving
    }

    private boolean isValidInput(String username, String email, String phone, String address) {
        // Validate username
        if (TextUtils.isEmpty(username)) {
            binding.usernameEditText.setError("Username is required");
            binding.usernameEditText.requestFocus();
            return false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            binding.emailEditText.setError("Email is required");
            binding.emailEditText.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.setError("Please enter a valid email address");
            binding.emailEditText.requestFocus();
            return false;
        }

        // Validate phone number
        if (TextUtils.isEmpty(phone)) {
            binding.phoneEditText.setError("Phone number is required");
            binding.phoneEditText.requestFocus();
            return false;
        }
        if (phone.length() != 10) {  // Assuming phone number must be 10 digits
            binding.phoneEditText.setError("Phone number must be 10 digits");
            binding.phoneEditText.requestFocus();
            return false;
        }

        // Validate address
        if (TextUtils.isEmpty(address)) {
            binding.addressEditText.setError("Address is required");
            binding.addressEditText.requestFocus();
            return false;
        }

        return true; // All validations passed
    }
}
