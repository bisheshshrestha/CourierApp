package com.divyagyan.courierapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.divyagyan.courierapp.databinding.ActivityDashboardBinding;


public class ViewOrderActivity extends DrawerBaseActivity {

    private TextView textView3;
    ActivityDashboardBinding activityDashboardBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityDashboardBinding= ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(activityDashboardBinding.getRoot());
        allocateActivityTitle("View Orders");

//        String userUid = getIntent().getStringExtra("user_uid");
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userUid = sharedPreferences.getString("user_uid", null);


    }
}