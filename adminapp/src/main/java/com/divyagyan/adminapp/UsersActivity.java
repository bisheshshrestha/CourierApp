package com.divyagyan.adminapp;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.divyagyan.adminapp.Adapter.UserAdapter;
import com.divyagyan.adminapp.databinding.ActivityUsersBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends DrawerBaseActivity {

    private ActivityUsersBinding activityUsersBinding;
    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityUsersBinding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(activityUsersBinding.getRoot());
        allocateActivityTitle("User Details");

        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(this, userList);
        recyclerViewUsers.setAdapter(userAdapter);

        // Fetch users from Firebase
        FirebaseDatabase.getInstance().getReference("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        userList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                user.setUid(snapshot.getKey());
                                userList.add(user);
                            }
                        }
                        userAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(UsersActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
