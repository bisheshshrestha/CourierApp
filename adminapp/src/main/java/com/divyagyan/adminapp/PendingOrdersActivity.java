package com.divyagyan.adminapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.divyagyan.adminapp.databinding.ActivityPendingOrdersBinding;
import com.divyagyan.adminapp.databinding.ActivityPendingOrdersBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PendingOrdersActivity extends DrawerBaseActivity {

    ActivityPendingOrdersBinding binding;
    private ListView listViewOrderCreated;
    private List<String> orderCreatedList;
    private List<Map<String, Object>> orderCreatedDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        allocateActivityTitle("Order Created");

        listViewOrderCreated = binding.listViewOrders;
        orderCreatedList = new ArrayList<>();
        orderCreatedDataList = new ArrayList<>();

        fetchOrderCreatedData();

        // Set item click listener for the ListView
        listViewOrderCreated.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> selectedOrder = orderCreatedDataList.get(position);
            String orderId = safeGetString(selectedOrder, "orderId");

            if (orderId == null || orderId.isEmpty()) {
                Toast.makeText(PendingOrdersActivity.this, "Order ID is missing!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Start OrderDetailsActivity and pass order details
            Intent intent = new Intent(PendingOrdersActivity.this, OrderDetailsActivity.class);
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

            Log.d("OrderCreatedActivity", "Order ID passed: " + orderId);
            startActivity(intent);
        });
    }

    private void fetchOrderCreatedData() {
        FirebaseDatabase.getInstance().getReference("orders")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int count = 1;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();

                            if (orderData != null) {
                                // Get status and trim any extra spaces
                                String status = safeGetString(orderData, "status").trim();

                                // Log the status value for debugging
                                Log.d("PendingOrdersActivity", "Status: " + status);

                                // Check if the status is "Order Created"
                                if ("Order Created".equalsIgnoreCase(status)) {
                                    orderData.put("orderId", snapshot.getKey());
                                    orderCreatedDataList.add(orderData);

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

                                    orderCreatedList.add(orderDetails);
                                    count++;
                                }
                            }
                        }

                        if (orderCreatedList.isEmpty()) {
                            Toast.makeText(PendingOrdersActivity.this, "No Order Created entries found", Toast.LENGTH_SHORT).show();
                        } else {
                            // Set data to ListView using ArrayAdapter
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(PendingOrdersActivity.this, android.R.layout.simple_list_item_1, orderCreatedList);
                            listViewOrderCreated.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(PendingOrdersActivity.this, "Failed to fetch orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private String safeGetString(Map<String, Object> map, String key) {
        return map != null && map.containsKey(key) && map.get(key) != null ? map.get(key).toString() : "N/A";
    }
}
