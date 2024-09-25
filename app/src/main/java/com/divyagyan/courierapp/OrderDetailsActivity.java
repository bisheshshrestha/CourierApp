package com.divyagyan.courierapp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OrderDetailsActivity extends AppCompatActivity {

    private TextView textViewTrackingNumber, textViewPackageDetails, textViewRecipientName, textViewRecipientPhone, textViewPrice, textViewOrderTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        // Bind views
        textViewTrackingNumber = findViewById(R.id.textView_trackingNumber);
        textViewPackageDetails = findViewById(R.id.textView_packageDetails);
        textViewRecipientName = findViewById(R.id.textView_recipientName);
        textViewRecipientPhone = findViewById(R.id.textView_recipientPhone);
        textViewPrice = findViewById(R.id.textView_orderPrice);
        textViewOrderTime = findViewById(R.id.textView_orderTime);

        // Get the order details from Intent extras
        String trackingNumber = getIntent().getStringExtra("trackingNumber");
        String packageDetails = getIntent().getStringExtra("packageDetails");
        String recipientName = getIntent().getStringExtra("recipientName");
        String recipientPhone = getIntent().getStringExtra("recipientPhone");
        String price = getIntent().getStringExtra("price");
        String orderCreationTime = getIntent().getStringExtra("orderCreationTime");

        // Set data in TextViews
        textViewTrackingNumber.setText("Tracking Number: " + trackingNumber);
        textViewPackageDetails.setText("Package Details: " + packageDetails);
        textViewRecipientName.setText("Recipient Name: " + recipientName);
        textViewRecipientPhone.setText("Recipient Phone: " + recipientPhone);
        textViewPrice.setText("Price: Rs " + price);
        textViewOrderTime.setText("Order Created At: " + orderCreationTime);
    }
}
