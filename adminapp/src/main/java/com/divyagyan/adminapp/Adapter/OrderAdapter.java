package com.divyagyan.adminapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.divyagyan.adminapp.OrderDetailsActivity;
import com.divyagyan.adminapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> implements Filterable {

    private final Context context;
    private List<Map<String, Object>> orderDataList;
    private final List<Map<String, Object>> fullOrderDataList;
    private String selectedStatus = "";

    public OrderAdapter(Context context, List<Map<String, Object>> orderDataList) {
        this.context = context;
        this.orderDataList = orderDataList;
        this.fullOrderDataList = new ArrayList<>(orderDataList);
    }

    public void setSelectedStatus(String status) {
        this.selectedStatus = status;
        getFilter().filter("");
    }

    public void filter(String query) {
        getFilter().filter(query);
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

        String trackingNumber = orderData.get("trackingNumber") != null ? orderData.get("trackingNumber").toString() : "N/A";
        String orderId = orderData.get("orderId") != null ? orderData.get("orderId").toString() : "N/A";
        String recipientName = orderData.get("recipientName") != null ? orderData.get("recipientName").toString() : "N/A";
        String status = orderData.get("status") != null ? orderData.get("status").toString() : "N/A";
        String price = orderData.get("price") != null ? orderData.get("price").toString() : "N/A";
        String distance;

        // Safely handle the distance value
        Object distanceValue = orderData.get("distance");
        if (distanceValue instanceof String) {
            distance = (String) distanceValue;
        } else if (distanceValue instanceof Double) {
            distance = String.format(Locale.getDefault(), "%.2f", (Double) distanceValue);
        } else {
            distance = "N/A";
        }
        holder.textViewTrackingNumber.setText("Tracking Number: " + trackingNumber);
        holder.textViewOrderId.setText("Order ID: " + orderId);
        holder.textViewRecipientName.setText("Name: " + recipientName);
        holder.textViewStatus.setText("Status: " + status);
        holder.textViewPrice.setText("Rs. " + price);
        holder.textViewDistance.setText("Distance: " + distance + " km");

        // Set status text color based on status
        switch (status) {
            case "Order Created":
                holder.textViewStatus.setTextColor(Color.parseColor("#FFA500")); // Orange
                break;
            case "Pickup Complete":
                holder.textViewStatus.setTextColor(Color.parseColor("#3F51B5")); // Blue
                break;
            case "Sent for Delivery":
                holder.textViewStatus.setTextColor(Color.parseColor("#FF9800")); // Dark Orange
                break;
            case "Delivered":
                holder.textViewStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                break;
            default:
                holder.textViewStatus.setTextColor(Color.parseColor("#777777")); // Default gray
                break;
        }

        // Set click listener to open OrderDetailsActivity
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
        return orderDataList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Map<String, Object>> filteredList = new ArrayList<>();

                for (Map<String, Object> order : fullOrderDataList) {
                    String status = order.get("status") != null ? order.get("status").toString() : "";
                    if ((selectedStatus.isEmpty() || selectedStatus.equals("All") || status.equals(selectedStatus))) {
                        filteredList.add(order);
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                orderDataList.clear();
                orderDataList.addAll((List<Map<String, Object>>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTrackingNumber,textViewOrderId, textViewRecipientName, textViewStatus, textViewPrice, textViewDistance;

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
}
