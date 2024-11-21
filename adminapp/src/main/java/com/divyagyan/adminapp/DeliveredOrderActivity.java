package com.divyagyan.adminapp;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.divyagyan.adminapp.Adapter.DeliveredOrderAdapter;
import com.divyagyan.adminapp.databinding.ActivityDeliveredOrderBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeliveredOrderActivity extends DrawerBaseActivity {

    private ActivityDeliveredOrderBinding binding;
    private RecyclerView recyclerViewDeliveredOrders;
    private DeliveredOrderAdapter deliveredOrderAdapter;
    private List<Map<String, Object>> orderDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeliveredOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        allocateActivityTitle("Delivered Orders");

        recyclerViewDeliveredOrders = binding.recyclerViewDeliveredOrders;
        recyclerViewDeliveredOrders.setLayoutManager(new LinearLayoutManager(this));

        orderDataList = new ArrayList<>();
        deliveredOrderAdapter = new DeliveredOrderAdapter(this, orderDataList);
        recyclerViewDeliveredOrders.setAdapter(deliveredOrderAdapter);

        fetchDeliveredOrders();
    }

    private void fetchDeliveredOrders() {
        FirebaseDatabase.getInstance().getReference("orders")
                .orderByChild("status").equalTo("Delivered")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        orderDataList.clear();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();
                            if (orderData != null) {
                                orderData.put("orderId", snapshot.getKey());
                                orderDataList.add(orderData);
                            }
                        }

                        deliveredOrderAdapter.notifyDataSetChanged();

                        if (orderDataList.isEmpty()) {
                            Toast.makeText(DeliveredOrderActivity.this, "No Delivered orders found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(DeliveredOrderActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
