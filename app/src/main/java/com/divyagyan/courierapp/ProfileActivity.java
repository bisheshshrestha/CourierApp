package com.divyagyan.courierapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.divyagyan.courierapp.databinding.ActivityProfileBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends DrawerBaseActivity {

    ActivityProfileBinding activityProfileBinding;
    private DatabaseReference userDatabaseRef;
    private String userUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityProfileBinding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(activityProfileBinding.getRoot());
        allocateActivityTitle("Profile");

        // Load the UID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userUid = sharedPreferences.getString("user_uid", null);
        if (userUid == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userDatabaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userUid);


        loadUserData();

        activityProfileBinding.editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }
    private void loadUserData() {
        userDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Use the correct field name from your Firebase structure (userName)
                    String username = snapshot.child("userName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);

                    activityProfileBinding.displayUsername.setText(username != null ? username : "N/A");
                    activityProfileBinding.displayEmail.setText(email != null ? email : "N/A");
                    activityProfileBinding.displayPhone.setText(phone != null ? phone : "N/A");
                    activityProfileBinding.displayAddress.setText(address != null ? address : "N/A");

                } else {
                    Toast.makeText(ProfileActivity.this, "User data not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
