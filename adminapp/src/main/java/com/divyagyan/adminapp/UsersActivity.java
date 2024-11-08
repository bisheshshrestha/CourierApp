package com.divyagyan.adminapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.divyagyan.adminapp.databinding.ActivityPickupOrderBinding;
import com.divyagyan.adminapp.databinding.ActivityUsersBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends DrawerBaseActivity {

    ActivityUsersBinding activityUsersBinding;

    private ListView listViewUsers;
    private List<String> userNames; // This will store serial number + user name
    private List<User> userList;    // This will store user data (with UID)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityUsersBinding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(activityUsersBinding.getRoot());
        allocateActivityTitle("Users Details");

        listViewUsers = findViewById(R.id.listViewUsers);
        userNames = new ArrayList<>();
        userList = new ArrayList<>();

        // Fetch users from Firebase
        FirebaseDatabase.getInstance().getReference("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        userList.clear();
                        userNames.clear();
                        int sn = 1; // Starting Serial Number
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            User user = snapshot.getValue(User.class);
                            user.setUid(snapshot.getKey()); // Store the user's UID (Firebase key)
                            userList.add(user);
                            // Add serial number and user name to the list
                            userNames.add(sn + ". " + user.getUserName());
                            sn++;
                        }

                        // Set the adapter to display user names with serial numbers
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(UsersActivity.this, android.R.layout.simple_list_item_1, userNames);
                        listViewUsers.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(UsersActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    }
                });

        // Handle click events on the ListView items
        listViewUsers.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = userList.get(position);

            // Open UserDetailsActivity and pass user data, including the UID
            Intent intent = new Intent(UsersActivity.this, UserDetailsActivity.class);
            intent.putExtra("uid", selectedUser.getUid());
            intent.putExtra("userName", selectedUser.getUserName());
            intent.putExtra("email", selectedUser.getEmail());
            intent.putExtra("phone", selectedUser.getPhone());
            intent.putExtra("address", selectedUser.getAddress());
            startActivity(intent);
        });
    }
}
