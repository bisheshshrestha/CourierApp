package com.divyagyan.courierapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.divyagyan.courierapp.databinding.ActivityViewOrderBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ViewOrderActivity extends DrawerBaseActivity {

    private ActivityViewOrderBinding activityViewOrderBinding;
    private ListView listViewOrders;
    private List<String> orderList;
    private List<Map<String, Object>> orderDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize view binding for this activity
        activityViewOrderBinding = ActivityViewOrderBinding.inflate(getLayoutInflater());
        setContentView(activityViewOrderBinding.getRoot());

        // Allocate title to the drawer for this activity
        allocateActivityTitle("View Orders");

        // Initialize ListView and lists
        listViewOrders = activityViewOrderBinding.listViewOrders;  // Using ViewBinding to get ListView
        orderList = new ArrayList<>();
        orderDataList = new ArrayList<>();

        // Fetch user ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userUid = sharedPreferences.getString("user_uid", null);

        if (userUid != null) {
            // Fetch orders for this user
            fetchOrders(userUid);
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
        }

        // Set item click listener to view order details
        listViewOrders.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> selectedOrder = orderDataList.get(position);

            // Retrieve the orderId from the selectedOrder map
            String orderId = safeGetString(selectedOrder, "orderId");

            if (orderId != null) {
                // Start OrderDetailsActivity and pass order details to it
                Intent intent = new Intent(ViewOrderActivity.this, OrderDetailsActivity.class);
                intent.putExtra("orderId", orderId);  // Correctly pass the orderId
                intent.putExtra("trackingNumber", safeGetString(selectedOrder, "trackingNumber"));
                intent.putExtra("packageDetails", safeGetString(selectedOrder, "packageDetails"));
                intent.putExtra("recipientName", safeGetString(selectedOrder, "recipientName"));
                intent.putExtra("recipientPhone", safeGetString(selectedOrder, "recipientPhone"));
                intent.putExtra("price", safeGetString(selectedOrder, "price"));
                intent.putExtra("distance", safeGetString(selectedOrder, "distance"));
                intent.putExtra("orderCreationTime", safeGetString(selectedOrder, "orderCreationTime"));

                // Safely retrieve pickup and delivery locations
                Map<String, Object> pickupLocation = (Map<String, Object>) selectedOrder.get("pickupLocation");
                Map<String, Object> deliveryLocation = (Map<String, Object>) selectedOrder.get("deliveryLocation");

                intent.putExtra("pickupLat", pickupLocation != null ? safeGetString(pickupLocation, "latitude") : "0");
                intent.putExtra("pickupLng", pickupLocation != null ? safeGetString(pickupLocation, "longitude") : "0");
                intent.putExtra("deliveryLat", deliveryLocation != null ? safeGetString(deliveryLocation, "latitude") : "0");
                intent.putExtra("deliveryLng", deliveryLocation != null ? safeGetString(deliveryLocation, "longitude") : "0");

                startActivity(intent);
            } else {
                Toast.makeText(ViewOrderActivity.this, "Order ID not found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to fetch orders from Firebase
    // Method to fetch orders from Firebase
    private void fetchOrders(String userUid) {
        FirebaseDatabase.getInstance().getReference("orders")
                .orderByChild("userUid")
                .equalTo(userUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        orderDataList.clear(); // Clear list before adding new data
                        orderList.clear(); // Clear the displayed list as well

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();
                            if (orderData != null) {
                                orderData.put("orderId", snapshot.getKey()); // Store orderId in orderData map
                                orderDataList.add(orderData); // Add to list

                                // Extract order details
                                String recipientName = safeGetString(orderData, "recipientName");
                                String recipientPhone = safeGetString(orderData, "recipientPhone");
                                String price = safeGetString(orderData, "price");
                                String distance = safeGetString(orderData, "distance");

                                String status = safeGetString(orderData, "status");
                                if ("N/A".equals(status)) {
                                    status = "Order Created";
                                }
                                    // Format the order summary string
                                String orderDetails = String.format(
                                        "Name: %s\nPhone: %s\nPrice: Rs %s\nDistance: %s km\nStatus: %s",
                                        recipientName, recipientPhone, price, distance, status
                                );


                                orderList.add(orderDetails);
                            }
                        }

                        if (orderList.isEmpty()) {
                            Toast.makeText(ViewOrderActivity.this, "No orders found", Toast.LENGTH_SHORT).show();
                        } else {
                            // Display orders in ListView
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(ViewOrderActivity.this, android.R.layout.simple_list_item_1, orderList);
                            listViewOrders.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ViewOrderActivity.this, "Failed to fetch orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // Helper method to safely retrieve string from map
    private String safeGetString(Map<String, Object> map, String key) {
        return map != null && map.containsKey(key) && map.get(key) != null ? map.get(key).toString() : "N/A";
    }
}
