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

            // Start OrderDetailsActivity and pass order details to it
            Intent intent = new Intent(ViewOrderActivity.this, OrderDetailsActivity.class);
            intent.putExtra("trackingNumber", selectedOrder.get("trackingNumber").toString());
            intent.putExtra("packageDetails", selectedOrder.get("packageDetails").toString());
            intent.putExtra("recipientName", selectedOrder.get("recipientName").toString());
            intent.putExtra("recipientPhone", selectedOrder.get("recipientPhone").toString());
            intent.putExtra("price", selectedOrder.get("price").toString());
            intent.putExtra("orderCreationTime", selectedOrder.get("orderCreationTime").toString());
            startActivity(intent);
        });
    }

    // Method to fetch orders from Firebase
    private void fetchOrders(String userUid) {
        FirebaseDatabase.getInstance().getReference("orders")
                .orderByChild("userUid")
                .equalTo(userUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();
                            orderDataList.add(orderData);

                            String orderId = snapshot.getKey();
                            String price = orderData.get("price").toString();

                            // Add order summary to orderList
                            orderList.add("Order ID: " + orderId + "\nPrice: Rs " + price);
                        }

                        // Display orders in ListView
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(ViewOrderActivity.this, android.R.layout.simple_list_item_1, orderList);
                        listViewOrders.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ViewOrderActivity.this, "Failed to fetch orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
