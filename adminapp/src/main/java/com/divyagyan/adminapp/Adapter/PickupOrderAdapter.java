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
import java.util.Locale;
import java.util.Map;

public class PickupOrderAdapter extends RecyclerView.Adapter<PickupOrderAdapter.PickupOrderViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> pickupOrderDataList;

    public PickupOrderAdapter(Context context, List<Map<String, Object>> pickupOrderDataList) {
        this.context = context;
        this.pickupOrderDataList = pickupOrderDataList;
    }

    @NonNull
    @Override
    public PickupOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new PickupOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PickupOrderViewHolder holder, int position) {
        Map<String, Object> orderData = pickupOrderDataList.get(position);

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

        holder.textViewStatus.setTextColor(Color.parseColor("#3F51B5")); // Blue


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            intent.putExtra("trackingNumber", trackingNumber);
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
        return pickupOrderDataList.size();
    }

    static class PickupOrderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTrackingNumber,textViewOrderId, textViewRecipientName, textViewStatus, textViewPrice, textViewDistance;

        PickupOrderViewHolder(@NonNull View itemView) {
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
