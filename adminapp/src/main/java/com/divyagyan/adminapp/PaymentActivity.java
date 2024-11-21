package com.divyagyan.adminapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {

    private TextView totalIncomeTextView, completedDeliveryChargesTextView, pendingPaymentsTextView, completedOrdersTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize views
        totalIncomeTextView = findViewById(R.id.totalIncomeTextView);
        completedDeliveryChargesTextView = findViewById(R.id.completedDeliveryChargesTextView); // Updated TextView
        pendingPaymentsTextView = findViewById(R.id.pendingPaymentsTextView);
        completedOrdersTextView = findViewById(R.id.completedOrdersTextView);

        // Load income data from Firebase
        showBillingDetails();
        setupClickListeners();
    }

    private void showBillingDetails() {
        FirebaseDatabase.getInstance().getReference("orders")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double totalIncome = 0;
                        double completedDeliveryCharges = 0; // Track only completed delivery charges
                        double pendingPayments = 0;
                        int completedOrders = 0;

                        for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                            Double price = null;

                            // Attempt to retrieve price as a Double directly
                            try {
                                price = orderSnapshot.child("price").getValue(Double.class);
                            } catch (DatabaseException e) {
                                // If it fails, try to retrieve it as a String and then parse it to Double
                                String priceString = orderSnapshot.child("price").getValue(String.class);
                                if (priceString != null) {
                                    try {
                                        price = Double.parseDouble(priceString);
                                    } catch (NumberFormatException ex) {
                                        Toast.makeText(PaymentActivity.this, "Invalid price format in database.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }

                            if (price != null) {
                                String status = orderSnapshot.child("status").getValue(String.class);

                                // Update totals based on order status
                                if ("Delivered".equals(status)) {
                                    completedDeliveryCharges += price; // Only add charges for delivered orders
                                    totalIncome += price;
                                    completedOrders++;
                                } else if ("Sent For Delivery".equals(status)) {
                                    pendingPayments += price;
                                }
                            }
                        }

                        // Update TextViews with calculated values
                        totalIncomeTextView.setText(String.format(Locale.getDefault(), "Rs %.2f", totalIncome));
                        completedDeliveryChargesTextView.setText(String.format(Locale.getDefault(), "Rs %.2f", completedDeliveryCharges));
                        pendingPaymentsTextView.setText(String.format(Locale.getDefault(), "Rs %.2f", pendingPayments));
                        completedOrdersTextView.setText(String.valueOf(completedOrders));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(PaymentActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // Method to set click listeners for the TextViews
    private void setupClickListeners() {
        // Open SentOrderActivity when clicking on pending payments
        pendingPaymentsTextView.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentActivity.this, SentOrderActivity.class);
            startActivity(intent);
        });

        // Open DeliveredOrderActivity when clicking on completed delivery charges
        completedDeliveryChargesTextView.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentActivity.this, DeliveredOrderActivity.class);
            startActivity(intent);
        });

        // Open DeliveredOrderActivity when clicking on completed orders
        completedOrdersTextView.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentActivity.this, DeliveredOrderActivity.class);
            startActivity(intent);
        });
    }
}
