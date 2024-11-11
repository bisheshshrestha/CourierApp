package com.divyagyan.adminapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.divyagyan.adminapp.databinding.ActivityOrderBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderActivity extends DrawerBaseActivity {

    ActivityOrderBinding activityOrderBinding;

    private ListView listViewOrders;
    private List<String> orderList;
    private List<Map<String, Object>> orderDataList;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityOrderBinding = ActivityOrderBinding.inflate(getLayoutInflater());
        setContentView(activityOrderBinding.getRoot());
        allocateActivityTitle("Order Details");

        listViewOrders = findViewById(R.id.listViewOrders);
        orderList = new ArrayList<>();
        orderDataList = new ArrayList<>();

        fetchAllOrders();

        listViewOrders.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> selectedOrder = orderDataList.get(position);
            String orderId = safeGetString(selectedOrder, "orderId");

            if (orderId == null || orderId.isEmpty()) {
                Toast.makeText(OrderActivity.this, "Order ID is missing!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(OrderActivity.this, OrderDetailsActivity.class);
            intent.putExtra("orderId", orderId);
            intent.putExtra("trackingNumber", safeGetString(selectedOrder, "trackingNumber"));
            intent.putExtra("packageDetails", safeGetString(selectedOrder, "packageDetails"));
            intent.putExtra("recipientName", safeGetString(selectedOrder, "recipientName"));
            intent.putExtra("recipientPhone", safeGetString(selectedOrder, "recipientPhone"));
            intent.putExtra("price", safeGetString(selectedOrder, "price"));
            intent.putExtra("distance", safeGetString(selectedOrder, "distance"));

            Map<String, Object> pickupLocation = (Map<String, Object>) selectedOrder.get("pickupLocation");
            Map<String, Object> deliveryLocation = (Map<String, Object>) selectedOrder.get("deliveryLocation");

            intent.putExtra("pickupLat", pickupLocation != null ? safeGetString(pickupLocation, "latitude") : "0");
            intent.putExtra("pickupLng", pickupLocation != null ? safeGetString(pickupLocation, "longitude") : "0");
            intent.putExtra("deliveryLat", deliveryLocation != null ? safeGetString(deliveryLocation, "latitude") : "0");
            intent.putExtra("deliveryLng", deliveryLocation != null ? safeGetString(deliveryLocation, "longitude") : "0");

            Log.d("OrderActivity", "Order ID passed: " + orderId);
            startActivity(intent);
        });
    }

    private void fetchAllOrders() {
        FirebaseDatabase.getInstance().getReference("orders")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int count = 1;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();

                            if (orderData != null) {
                                orderData.put("orderId", snapshot.getKey());
                                orderDataList.add(orderData);

                                // Extract details
                                String recipientName = safeGetString(orderData, "recipientName");
                                String recipientPhone = safeGetString(orderData, "recipientPhone");
                                String status = safeGetString(orderData, "status");
                                String price;
                                String distance = safeGetString(orderData, "distance");

                                if ("N/A".equals(status)) {
                                    status = "Order Created";
                                }

                                try {
                                    price = String.format(Locale.getDefault(), "%.2f", Double.parseDouble(safeGetString(orderData, "price")));
                                } catch (Exception e) {
                                    price = "N/A";
                                }

                                // Create a formatted string for the ListView item
                                String orderDetails = String.format(
                                        "Name: %s\nPhone: %s\nPrice: Rs %s\nDistance: %s km\nStatus: %s",
                                        recipientName, recipientPhone, price, distance, status
                                );

                                orderList.add(orderDetails);
                                count++;
                            }
                        }

                        if (orderList.isEmpty()) {
                            Toast.makeText(OrderActivity.this, "No orders found", Toast.LENGTH_SHORT).show();
                        } else {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(OrderActivity.this, android.R.layout.simple_list_item_1, orderList);
                            listViewOrders.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(OrderActivity.this, "Failed to fetch orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String safeGetString(Map<String, Object> map, String key) {
        return map != null && map.containsKey(key) && map.get(key) != null ? map.get(key).toString() : "N/A";
    }
}
