package com.divyagyan.courierapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import com.divyagyan.courierapp.databinding.ActivityDashboardBinding;

public class DashboardActivity extends DrawerBaseActivity {

    private CardView createOrderCardView, viewOrderCardView, profileCardView, logoutCardView;
    ActivityDashboardBinding activityDashboardBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityDashboardBinding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(activityDashboardBinding.getRoot());
        allocateActivityTitle("Dashboard");

        createOrderCardView = findViewById(R.id.createOrderCardView);
        viewOrderCardView = findViewById(R.id.viewOrderCardView);
        profileCardView = findViewById(R.id.profileCardView);
        logoutCardView = findViewById(R.id.logoutCardView);

        createOrderCardView.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, OrderActivity.class);
            startActivity(intent);
        });

        viewOrderCardView.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, ViewOrderActivity.class);
            startActivity(intent);
        });

        profileCardView.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        logoutCardView.setOnClickListener(view -> showLogoutConfirmationDialog());

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userUid = sharedPreferences.getString("user_uid", null);
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Handle the logout action
                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.apply();
                    Toast.makeText(DashboardActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(DashboardActivity.this, SignInActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }
}
