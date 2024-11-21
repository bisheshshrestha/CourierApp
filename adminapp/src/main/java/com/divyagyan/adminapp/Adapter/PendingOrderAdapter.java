package com.divyagyan.adminapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.divyagyan.adminapp.OrderDetailsActivity;
import com.divyagyan.adminapp.R;

import java.util.List;
import java.util.Map;

public class PendingOrderAdapter extends RecyclerView.Adapter<PendingOrderAdapter.PendingOrderViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> pendingOrderDataList;

    public PendingOrderAdapter(Context context, List<Map<String, Object>> pendingOrderDataList) {
        this.context = context;
        this.pendingOrderDataList = pendingOrderDataList;
    }

    @NonNull
    @Override
    public PendingOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new PendingOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingOrderViewHolder holder, int position) {
        Map<String, Object> orderData = pendingOrderDataList.get(position);

        String trackingNumber = orderData.get("trackingNumber") != null ? orderData.get("trackingNumber").toString() : "N/A";
        String orderId = orderData.get("orderId") != null ? orderData.get("orderId").toString() : "N/A";
        String recipientName = orderData.get("recipientName") != null ? orderData.get("recipientName").toString() : "N/A";
        String status = orderData.get("status") != null ? orderData.get("status").toString() : "N/A";
        String price = orderData.get("price") != null ? orderData.get("price").toString() : "N/A";
        String distance = orderData.get("distance") != null ? orderData.get("distance").toString() : "N/A";

        holder.textViewTrackingNumber.setText("Tracking Number: " + trackingNumber);
        holder.textViewOrderId.setText("Order ID: " + orderId);
        holder.textViewRecipientName.setText("Recipient: " + recipientName);
        holder.textViewStatus.setText("Status: " + status);
        holder.textViewPrice.setText("Rs. " + price);
        holder.textViewDistance.setText("Distance: " + distance + " km");

        holder.textViewStatus.setTextColor(Color.parseColor("#FFA500")); // Orange

        // Set click listener to open OrderDetailsActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            intent.putExtra("orderId", orderId);
            intent.putExtra("recipientName", recipientName);
            intent.putExtra("status", status);
            intent.putExtra("price", price);
            intent.putExtra("distance", distance);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return pendingOrderDataList.size();
    }

    static class PendingOrderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTrackingNumber,textViewOrderId, textViewRecipientName, textViewStatus, textViewPrice, textViewDistance;

        PendingOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTrackingNumber = itemView.findViewById(R.id.textViewTrackingNumber);
            textViewOrderId = itemView.findViewById(R.id.textViewOrderId);
            textViewRecipientName = itemView.findViewById(R.id.textViewRecipientName);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            textViewDistance = itemView.findViewById(R.id.textViewDistance);
        }
    }
}