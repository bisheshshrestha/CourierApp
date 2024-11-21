package com.divyagyan.adminapp;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.divyagyan.adminapp.Adapter.PickupOrderAdapter;
import com.divyagyan.adminapp.databinding.ActivityDeliveredOrderBinding;
import com.divyagyan.adminapp.databinding.ActivityPickupOrderBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PickupOrderActivity extends DrawerBaseActivity {
    ActivityPickupOrderBinding binding;
    private RecyclerView recyclerViewPickupOrders;
    private PickupOrderAdapter pickupOrderAdapter;
    private List<Map<String, Object>> pickupOrderDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPickupOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        allocateActivityTitle("Pickup Completed Orders");

        recyclerViewPickupOrders = findViewById(R.id.recyclerViewPickupOrders);
        recyclerViewPickupOrders.setLayoutManager(new LinearLayoutManager(this));

        pickupOrderDataList = new ArrayList<>();
        pickupOrderAdapter = new PickupOrderAdapter(this, pickupOrderDataList);
        recyclerViewPickupOrders.setAdapter(pickupOrderAdapter);

        fetchPickupCompletedOrders();
    }

    private void fetchPickupCompletedOrders() {
        FirebaseDatabase.getInstance().getReference("orders")
                .orderByChild("status")
                .equalTo("Pickup Complete")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        pickupOrderDataList.clear();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();
                            if (orderData != null) {
                                orderData.put("orderId", snapshot.getKey());
                                pickupOrderDataList.add(orderData);
                            }
                        }

                        pickupOrderAdapter.notifyDataSetChanged();

                        if (pickupOrderDataList.isEmpty()) {
                            Toast.makeText(PickupOrderActivity.this, "No Pickup Completed orders found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(PickupOrderActivity.this, "Failed to fetch orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
