package com.divyagyan.courierapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.divyagyan.courierapp.Adapter.OrderAdapter;
import com.divyagyan.courierapp.databinding.ActivityViewOrderBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ViewOrderActivity extends DrawerBaseActivity {

    private ActivityViewOrderBinding binding;
    private AutoCompleteTextView spinnerFilter;
    private RecyclerView recyclerViewOrders;
    private OrderAdapter orderAdapter;
    private List<Map<String, Object>> orderDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityViewOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        allocateActivityTitle("View Orders");

        spinnerFilter = binding.spinnerFilter;
        recyclerViewOrders = binding.recyclerViewOrders;
        orderDataList = new ArrayList<>();

        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter(this, orderDataList);
        recyclerViewOrders.setAdapter(orderAdapter);

        String[] filterOptions = {"All", "Order Created", "Pickup Complete", "Sent For Delivery", "Delivered"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filterOptions);
        spinnerFilter.setAdapter(filterAdapter);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userUid = sharedPreferences.getString("user_uid", null);

        if (userUid != null) {
            fetchOrders(userUid, "All");
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
        }

        spinnerFilter.setOnItemClickListener((parent, view, position, id) -> {
            String selectedFilter = filterOptions[position];
            fetchOrders(userUid, selectedFilter);
        });
    }

    private void fetchOrders(String userUid, String selectedFilter) {
        FirebaseDatabase.getInstance().getReference("orders")
                .limitToLast(100)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        orderDataList.clear();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();
                            if (orderData != null && userUid.equals(orderData.get("userUid"))) {
                                orderData.put("orderId", snapshot.getKey());
                                String status = orderData.get("status") != null ? orderData.get("status").toString() : "N/A";

                                if ("All".equals(selectedFilter) || selectedFilter.equals(status)) {
                                    orderDataList.add(orderData);
                                }
                            }
                        }

                        Collections.reverse(orderDataList);
                        orderAdapter.notifyDataSetChanged();

                        if (orderDataList.isEmpty()) {
                            Toast.makeText(ViewOrderActivity.this, "No orders found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ViewOrderActivity.this, "Failed to fetch orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
