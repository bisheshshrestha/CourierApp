package com.divyagyan.courierapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;


import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class DrawerBaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;

    @Override
    public void setContentView(View view) {
        drawerLayout =(DrawerLayout) getLayoutInflater().inflate(R.layout.activity_drawer_base,null);
        FrameLayout container = drawerLayout.findViewById(R.id.activityContainer);
        container.addView(view);
        super.setContentView(drawerLayout);

        Toolbar toolbar = drawerLayout.findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.menu_drawer_open,R.string.menu_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

//    @Override
//    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//        drawerLayout.closeDrawer(GravityCompat.START);
//
//        int itemId = item.getItemId();
//
//        if (itemId == R.id.nav_orders) {
//            startActivity(new Intent(this, OrderActivity.class));
//            overridePendingTransition(0,0);
//        } else if (itemId == R.id.nav_customers) {
//            startActivity(new Intent(this, CustomerActivity.class));
//            overridePendingTransition(0,0);
//        } else if (itemId == R.id.nav_report) {
//            startActivity(new Intent(this, ReportActivity.class));
//            overridePendingTransition(0,0);
//        } else if (itemId == R.id.nav_profile) {
//            startActivity(new Intent(this, ProfileActivity.class));
//            overridePendingTransition(0,0);
//        } else {
//            return false;
//        }
//
//        return true;  // Indicate that the item was selected
//    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);

        int itemId = item.getItemId();

        if (itemId == R.id.nav_create_order) {
            startActivity(new Intent(this, OrderActivity.class));
            overridePendingTransition(0, 0);
        } else if (itemId == R.id.nav_view_order) {
            startActivity(new Intent(this, ViewOrderActivity.class));
            overridePendingTransition(0, 0);
        }
//        } else if (itemId == R.id.nav_report) {
//            startActivity(new Intent(this, ReportActivity.class));
//            overridePendingTransition(0, 0);
        //}
        else if (itemId == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
        } else if (itemId == R.id.nav_logout) {
            // Handle logout action
            performLogout();
        } else {
            return false;
        }

        return true;
    }

    private void performLogout() {
        // Clear SharedPreferences or any user session data
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear(); // Clear all user data
        editor.apply();

        // Redirect to login activity or main activity
        Intent intent = new Intent(this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear activity stack
        startActivity(intent);
        finish(); // Finish current activity
    }



    protected void allocateActivityTitle(String titleString){
        if(getSupportActionBar() != null){
            getSupportActionBar().setTitle(titleString);
        }
    }
}