package com.divyagyan.adminapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.divyagyan.adminapp.databinding.ActivityDeliveredOrderBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeliveredOrderActivity extends DrawerBaseActivity {

    ActivityDeliveredOrderBinding activityDeliveredOrderBinding;
    private ListView listViewDeliveredOrders;
    private List<String> deliveredOrderList;
    private List<Map<String, Object>> deliveredOrderDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityDeliveredOrderBinding = ActivityDeliveredOrderBinding.inflate(getLayoutInflater());
        setContentView(activityDeliveredOrderBinding.getRoot());
        allocateActivityTitle("Delivered Orders");

        listViewDeliveredOrders = activityDeliveredOrderBinding.listViewDeliveredOrders;
        deliveredOrderList = new ArrayList<>();
        deliveredOrderDataList = new ArrayList<>();

        fetchDeliveredOrders();

        // Set item click listener for the ListView
        listViewDeliveredOrders.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> selectedOrder = deliveredOrderDataList.get(position);
            String orderId = safeGetString(selectedOrder, "orderId");

            if (orderId == null || orderId.isEmpty()) {
                Toast.makeText(DeliveredOrderActivity.this, "Order ID is missing!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Start OrderDetailsActivity and pass order details
            Intent intent = new Intent(DeliveredOrderActivity.this, OrderDetailsActivity.class);
            intent.putExtra("orderId", orderId);
            intent.putExtra("trackingNumber", safeGetString(selectedOrder, "trackingNumber"));
            intent.putExtra("packageDetails", safeGetString(selectedOrder, "packageDetails"));
            intent.putExtra("recipientName", safeGetString(selectedOrder, "recipientName"));
            intent.putExtra("recipientPhone", safeGetString(selectedOrder, "recipientPhone"));
            intent.putExtra("price", safeGetString(selectedOrder, "price"));

            Map<String, Object> pickupLocation = (Map<String, Object>) selectedOrder.get("pickupLocation");
            Map<String, Object> deliveryLocation = (Map<String, Object>) selectedOrder.get("deliveryLocation");

            intent.putExtra("pickupLat", pickupLocation != null ? safeGetString(pickupLocation, "latitude") : "0");
            intent.putExtra("pickupLng", pickupLocation != null ? safeGetString(pickupLocation, "longitude") : "0");
            intent.putExtra("deliveryLat", deliveryLocation != null ? safeGetString(deliveryLocation, "latitude") : "0");
            intent.putExtra("deliveryLng", deliveryLocation != null ? safeGetString(deliveryLocation, "longitude") : "0");

            Log.d("DeliveredOrderActivity", "Order ID passed: " + orderId);
            startActivity(intent);
        });
    }

    private void fetchDeliveredOrders() {
        FirebaseDatabase.getInstance().getReference("orders")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int count = 1;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();

                            if (orderData != null) {
                                String status = safeGetString(orderData, "status");
                                if ("Delivered".equalsIgnoreCase(status)) {
                                    orderData.put("orderId", snapshot.getKey());
                                    deliveredOrderDataList.add(orderData);

                                    // Extract recipient details
                                    String recipientName = safeGetString(orderData, "recipientName");
                                    String recipientPhone = safeGetString(orderData, "recipientPhone");
                                    String price = safeGetString(orderData, "price");

                                    // Format the order details
                                    String orderDetails = count + ". " +
                                            "Name: " + recipientName + "\n" +
                                            "Phone: " + recipientPhone + "\n" +
                                            "Price: Rs " + price + "\n" +
                                            "Status: Delivered";

                                    deliveredOrderList.add(orderDetails);
                                    count++;
                                }
                            }
                        }

                        if (deliveredOrderList.isEmpty()) {
                            Toast.makeText(DeliveredOrderActivity.this, "No Delivered Orders found", Toast.LENGTH_SHORT).show();
                        } else {
                            // Set data to ListView using ArrayAdapter
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(DeliveredOrderActivity.this, android.R.layout.simple_list_item_1, deliveredOrderList);
                            listViewDeliveredOrders.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(DeliveredOrderActivity.this, "Failed to fetch orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String safeGetString(Map<String, Object> map, String key) {
        return map != null && map.containsKey(key) && map.get(key) != null ? map.get(key).toString() : "N/A";
    }
}
