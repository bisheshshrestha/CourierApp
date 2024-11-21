package com.divyagyan.adminapp;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.divyagyan.adminapp.Adapter.SentOrderAdapter;
import com.divyagyan.adminapp.databinding.ActivitySentOrderBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SentOrderActivity extends DrawerBaseActivity {

    private ActivitySentOrderBinding binding;
    private RecyclerView recyclerViewSentOrders;
    private SentOrderAdapter sentOrderAdapter;
    private List<Map<String, Object>> orderDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySentOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        allocateActivityTitle("Sent for Delivery Orders");

        recyclerViewSentOrders = binding.recyclerViewSentOrders;
        recyclerViewSentOrders.setLayoutManager(new LinearLayoutManager(this));

        orderDataList = new ArrayList<>();
        sentOrderAdapter = new SentOrderAdapter(this, orderDataList);
        recyclerViewSentOrders.setAdapter(sentOrderAdapter);

        fetchSentOrders();
    }

    private void fetchSentOrders() {
        FirebaseDatabase.getInstance().getReference("orders")
                .orderByChild("status").equalTo("Sent For Delivery")
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

                        sentOrderAdapter.notifyDataSetChanged();

                        if (orderDataList.isEmpty()) {
                            Toast.makeText(SentOrderActivity.this, "No Sent for Delivery orders found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SentOrderActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
