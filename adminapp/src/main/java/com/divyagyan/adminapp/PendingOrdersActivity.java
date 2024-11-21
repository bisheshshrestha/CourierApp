package com.divyagyan.adminapp;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.divyagyan.adminapp.Adapter.PendingOrderAdapter;
import com.divyagyan.adminapp.databinding.ActivityOrderBinding;
import com.divyagyan.adminapp.databinding.ActivityPendingOrdersBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PendingOrdersActivity extends DrawerBaseActivity {
    ActivityPendingOrdersBinding activityPendingOrdersBinding;

    private RecyclerView recyclerViewPendingOrders;
    private PendingOrderAdapter pendingOrderAdapter;
    private List<Map<String, Object>> pendingOrderDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityPendingOrdersBinding = ActivityPendingOrdersBinding.inflate(getLayoutInflater());
        setContentView(activityPendingOrdersBinding.getRoot());
        allocateActivityTitle("Pendings Orders");
        recyclerViewPendingOrders = findViewById(R.id.recyclerViewOrders);
        recyclerViewPendingOrders.setLayoutManager(new LinearLayoutManager(this));

        pendingOrderDataList = new ArrayList<>();
        fetchPendingOrders();
    }

    private void fetchPendingOrders() {
        FirebaseDatabase.getInstance().getReference("orders")
                .limitToLast(100)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        pendingOrderDataList.clear();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();
                            if (orderData != null) {
                                String status = orderData.get("status") != null ? orderData.get("status").toString() : "";
                                // Filter for "Order Created" status
                                if ("Order Created".equals(status)) {
                                    orderData.put("orderId", snapshot.getKey());
                                    pendingOrderDataList.add(orderData);
                                }
                            }
                        }

                        if (pendingOrderDataList.isEmpty()) {
                            Toast.makeText(PendingOrdersActivity.this, "No 'Order Created' orders found", Toast.LENGTH_SHORT).show();
                        } else {
                            pendingOrderAdapter = new PendingOrderAdapter(PendingOrdersActivity.this, pendingOrderDataList);
                            recyclerViewPendingOrders.setAdapter(pendingOrderAdapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(PendingOrdersActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
