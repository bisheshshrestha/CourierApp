package com.divyagyan.adminapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.divyagyan.adminapp.databinding.ActivityMainBinding;
import com.divyagyan.adminapp.databinding.ActivityPickupOrderBinding;


public class MainActivity extends DrawerBaseActivity {

    ActivityMainBinding activityMainBinding;
    private CardView manageUserCardView, manageOrderCardView, managePaymentCardView, logoutCardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());
        allocateActivityTitle("Admin App Dashboard");

        manageUserCardView = findViewById(R.id.manageUserCardView);
        manageOrderCardView = findViewById(R.id.manageOrderCardView);
        managePaymentCardView = findViewById(R.id.managePaymentCardView);
        logoutCardView = findViewById(R.id.logoutCardView);


        manageUserCardView.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, UsersActivity.class);
            startActivity(intent);
        });

        manageOrderCardView.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, OrderActivity.class);
            startActivity(intent);
        });

        managePaymentCardView.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, PaymentActivity.class);
            startActivity(intent);
        });

        logoutCardView.setOnClickListener(view -> showLogoutConfirmationDialog());
    }
        private void showLogoutConfirmationDialog() {
            new AlertDialog.Builder(this)
                    .setTitle("Logout Confirmation")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Toast.makeText(MainActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, SignInActivity.class);
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