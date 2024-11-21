package com.divyagyan.courierapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.divyagyan.courierapp.OrderDetailsActivity;
import com.divyagyan.courierapp.R;

import java.util.List;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> orderDataList;

    public OrderAdapter(Context context, List<Map<String, Object>> orderDataList) {
        this.context = context;
        this.orderDataList = orderDataList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Map<String, Object> orderData = orderDataList.get(position);

        String trackingNumber = safeGetString(orderData, "trackingNumber");
        String orderId = safeGetString(orderData, "orderId");
        String recipientName = safeGetString(orderData, "recipientName");
        String packageDetails = safeGetString(orderData, "packageDetails");
        String recipientPhone = safeGetString(orderData, "recipientPhone");
        String status = safeGetString(orderData, "status");
        String price = safeGetString(orderData, "price");
        String distance = safeGetString(orderData, "distance");

        Map<String, Object> pickupLocation = (Map<String, Object>) orderData.get("pickupLocation");
        Map<String, Object> deliveryLocation = (Map<String, Object>) orderData.get("deliveryLocation");

        String pickupLat = pickupLocation != null ? safeGetString(pickupLocation, "latitude") : "0";
        String pickupLng = pickupLocation != null ? safeGetString(pickupLocation, "longitude") : "0";
        String deliveryLat = deliveryLocation != null ? safeGetString(deliveryLocation, "latitude") : "0";
        String deliveryLng = deliveryLocation != null ? safeGetString(deliveryLocation, "longitude") : "0";

        holder.textViewTrackingNumber.setText("Tracking Number: " + trackingNumber);
        holder.textViewOrderId.setText("Order ID: " + orderId);
        holder.textViewRecipientName.setText("Recipient: " + recipientName);
        holder.textViewStatus.setText("Status: " + status);
        holder.textViewPrice.setText("Price: Rs. " + price);
        holder.textViewDistance.setText("Distance: " + distance + " km");

        switch (status) {
            case "Order Created":
                holder.textViewStatus.setTextColor(Color.parseColor("#FFA500"));
                break;
            case "Pickup Complete":
                holder.textViewStatus.setTextColor(Color.parseColor("#3F51B5"));
                break;
            case "Sent For Delivery":
                holder.textViewStatus.setTextColor(Color.parseColor("#FF9800"));
                break;
            case "Delivered":
                holder.textViewStatus.setTextColor(Color.parseColor("#4CAF50"));
                break;
            default:
                holder.textViewStatus.setTextColor(Color.parseColor("#777777"));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            intent.putExtra("orderId", orderId);
            intent.putExtra("trackingNumber", trackingNumber);
            intent.putExtra("recipientName", recipientName);
            intent.putExtra("status", status);
            intent.putExtra("price", price);
            intent.putExtra("distance", distance);
            intent.putExtra("pickupLat", pickupLat);
            intent.putExtra("pickupLng", pickupLng);
            intent.putExtra("deliveryLat", deliveryLat);
            intent.putExtra("deliveryLng", deliveryLng);
            intent.putExtra("packageDetails", packageDetails);
            intent.putExtra("recipientPhone", recipientPhone);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orderDataList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTrackingNumber, textViewOrderId, textViewRecipientName, textViewStatus, textViewPrice, textViewDistance;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTrackingNumber = itemView.findViewById(R.id.textViewTrackingNumber);
            textViewOrderId = itemView.findViewById(R.id.textViewOrderId);
            textViewRecipientName = itemView.findViewById(R.id.textViewRecipientName);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            textViewDistance = itemView.findViewById(R.id.textViewDistance);
        }
    }

    private String safeGetString(Map<String, Object> map, String key) {
        return map != null && map.containsKey(key) && map.get(key) != null ? map.get(key).toString() : "N/A";
    }
}
