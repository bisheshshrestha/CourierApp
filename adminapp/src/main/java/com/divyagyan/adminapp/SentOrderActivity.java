package com.divyagyan.adminapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.divyagyan.adminapp.databinding.ActivitySentOrderBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SentOrderActivity extends DrawerBaseActivity {

    ActivitySentOrderBinding activitySentOrderBinding;
    private ListView listViewSentOrders;
    private List<String> sentOrderList;
    private List<Map<String, Object>> sentOrderDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activitySentOrderBinding = ActivitySentOrderBinding.inflate(getLayoutInflater());
        setContentView(activitySentOrderBinding.getRoot());
        allocateActivityTitle("Sent For Delivery Orders");

        listViewSentOrders = findViewById(R.id.listViewSentOrders);
        sentOrderList = new ArrayList<>();
        sentOrderDataList = new ArrayList<>();

        // Fetch orders and display in ListView
        fetchSentForDeliveryOrders();

        // Set item click listener for the ListView
        listViewSentOrders.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> selectedOrder = sentOrderDataList.get(position);
            String orderId = safeGetString(selectedOrder, "orderId");

            if (orderId == null || orderId.isEmpty()) {
                Toast.makeText(SentOrderActivity.this, "Order ID is missing!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Start OrderDetailsActivity and pass order details
            Intent intent = new Intent(SentOrderActivity.this, OrderDetailsActivity.class);
            intent.putExtra("orderId", orderId);
            intent.putExtra("trackingNumber", safeGetString(selectedOrder, "trackingNumber"));
            intent.putExtra("packageDetails", safeGetString(selectedOrder, "packageDetails"));
            intent.putExtra("recipientName", safeGetString(selectedOrder, "recipientName"));
            intent.putExtra("recipientPhone", safeGetString(selectedOrder, "recipientPhone"));
            intent.putExtra("price", safeGetString(selectedOrder, "price"));

            // Pickup and delivery locations
            Map<String, Object> pickupLocation = (Map<String, Object>) selectedOrder.get("pickupLocation");
            Map<String, Object> deliveryLocation = (Map<String, Object>) selectedOrder.get("deliveryLocation");

            intent.putExtra("pickupLat", pickupLocation != null ? safeGetString(pickupLocation, "latitude") : "0");
            intent.putExtra("pickupLng", pickupLocation != null ? safeGetString(pickupLocation, "longitude") : "0");
            intent.putExtra("deliveryLat", deliveryLocation != null ? safeGetString(deliveryLocation, "latitude") : "0");
            intent.putExtra("deliveryLng", deliveryLocation != null ? safeGetString(deliveryLocation, "longitude") : "0");

            Log.d("SentOrderActivity", "Order ID passed: " + orderId);
            startActivity(intent);
        });
    }

    private void fetchSentForDeliveryOrders() {
        FirebaseDatabase.getInstance().getReference("orders")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int count = 1;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();

                            if (orderData != null) {
                                String status = safeGetString(orderData, "status");
                                if ("Sent for Delivery".equalsIgnoreCase(status)) {
                                    orderData.put("orderId", snapshot.getKey());
                                    sentOrderDataList.add(orderData);

                                    // Extract recipient details
                                    String recipientName = safeGetString(orderData, "recipientName");
                                    String recipientPhone = safeGetString(orderData, "recipientPhone");
                                    String price = safeGetString(orderData, "price");
                                    String distance = safeGetString(orderData, "distance");

                                    // Format the order details
                                    String orderDetails = count + ". " +
                                            "Name: " + recipientName + "\n" +
                                            "Phone: " + recipientPhone + "\n" +
                                            "Price: Rs " + price + "\n" +
                                            "Distance: " + distance + " km" + "\n"+
                                            "Status: " + status;

                                    sentOrderList.add(orderDetails);
                                    count++;
                                }
                            }
                        }

                        if (sentOrderList.isEmpty()) {
                            Toast.makeText(SentOrderActivity.this, "No Sent For Delivery Orders found", Toast.LENGTH_SHORT).show();
                        } else {
                            // Set data to ListView using ArrayAdapter
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(SentOrderActivity.this, android.R.layout.simple_list_item_1, sentOrderList);
                            listViewSentOrders.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(SentOrderActivity.this, "Failed to fetch orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String safeGetString(Map<String, Object> map, String key) {
        return map != null && map.containsKey(key) && map.get(key) != null ? map.get(key).toString() : "N/A";
    }
}
