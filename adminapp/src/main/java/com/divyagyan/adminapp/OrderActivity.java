package com.divyagyan.adminapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.divyagyan.adminapp.Adapter.OrderAdapter;
import com.divyagyan.adminapp.databinding.ActivityDeliveredOrderBinding;
import com.divyagyan.adminapp.databinding.ActivityOrderBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderActivity extends DrawerBaseActivity {

    ActivityOrderBinding binding;

    private RecyclerView recyclerViewOrders;
    private OrderAdapter orderAdapter;
    private List<Map<String, Object>> orderDataList;
    private Spinner statusFilterSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        allocateActivityTitle("Orders Details");

        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        statusFilterSpinner = findViewById(R.id.statusFilterSpinner);
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));

        orderDataList = new ArrayList<>();
        setupStatusFilter();
        fetchAllOrders();
    }

    private void setupStatusFilter() {
        String[] statuses = {"All", "Order Created", "Pickup Complete", "Sent For Delivery", "Delivered"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses);
        statusFilterSpinner.setAdapter(adapter);

        statusFilterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = statuses[position];
                if (orderAdapter != null) {
                    orderAdapter.setSelectedStatus(selectedStatus);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void fetchAllOrders() {
        FirebaseDatabase.getInstance().getReference("orders")
                .limitToLast(100)
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

                        orderAdapter = new OrderAdapter(OrderActivity.this, orderDataList);
                        recyclerViewOrders.setAdapter(orderAdapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(OrderActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
