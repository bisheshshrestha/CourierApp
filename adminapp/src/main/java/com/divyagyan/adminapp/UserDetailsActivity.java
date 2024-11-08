package com.divyagyan.adminapp;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserDetailsActivity extends AppCompatActivity {

    private EditText editTextUserName, editTextEmail, editTextPhone, editTextAddress;
    private DatabaseReference userRef;
    private String userUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        // Bind views
        editTextUserName = findViewById(R.id.editTextUserName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextAddress = findViewById(R.id.editTextAddress);

        // Get data from intent
        userUid = getIntent().getStringExtra("uid"); // Get the UID
        String userName = getIntent().getStringExtra("userName");
        String email = getIntent().getStringExtra("email");
        String phone = getIntent().getStringExtra("phone");
        String address = getIntent().getStringExtra("address");

        // Set data in the EditTexts
        editTextUserName.setText(userName);
        editTextEmail.setText(email);
        editTextPhone.setText(phone);
        editTextAddress.setText(address);

        // Get reference to user in Firebase using the UID
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userUid);

        // Save the updated data back to Firebase
        findViewById(R.id.buttonSave).setOnClickListener(v -> {
            String newUserName = editTextUserName.getText().toString();
            String newEmail = editTextEmail.getText().toString();
            String newPhone = editTextPhone.getText().toString();
            String newAddress = editTextAddress.getText().toString();

            // Update user data without changing UID
            userRef.child("userName").setValue(newUserName);
            userRef.child("email").setValue(newEmail);
            userRef.child("phone").setValue(newPhone);
            userRef.child("address").setValue(newAddress);

            Toast.makeText(UserDetailsActivity.this, "User updated successfully", Toast.LENGTH_SHORT).show();

            // Redirect back to UserListActivity after saving
            Intent intent = new Intent(UserDetailsActivity.this, UsersActivity.class);
            startActivity(intent);
            finish(); // Close the UserDetailsActivity
        });
    }
}
