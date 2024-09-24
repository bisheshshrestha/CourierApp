package com.divyagyan.courierapp;

import android.os.Bundle;


import com.divyagyan.courierapp.databinding.ActivityCustomerBinding;


public class OrderActivity extends DrawerBaseActivity {

    ActivityCustomerBinding activityCustomerBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityCustomerBinding =ActivityCustomerBinding.inflate(getLayoutInflater());
        setContentView(activityCustomerBinding.getRoot());
        allocateActivityTitle("Orders");

    }
}