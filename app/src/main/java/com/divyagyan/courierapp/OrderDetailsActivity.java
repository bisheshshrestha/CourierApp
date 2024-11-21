package com.divyagyan.courierapp;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.divyagyan.courierapp.databinding.ActivityOrderDetailsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OrderDetailsActivity extends DrawerBaseActivity implements OnMapReadyCallback {

    private ActivityOrderDetailsBinding activityOrderDetailsBinding;
    private TextView textViewTrackingNumber, textViewPackageDetails, textViewRecipientName, textViewRecipientPhone, textViewPrice, textViewDistance;
    private LinearLayout statusHistoryLayout;
    private GoogleMap mMap;
    private LatLng pickupLocation;
    private LatLng deliveryLocation;
    private Button goBackButton, editOrderButton, cancelOrderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityOrderDetailsBinding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(activityOrderDetailsBinding.getRoot());
        allocateActivityTitle("Order Details");

        textViewTrackingNumber = findViewById(R.id.textView_trackingNumber);
        textViewPackageDetails = findViewById(R.id.textView_packageDetails);
        textViewRecipientName = findViewById(R.id.textView_recipientName);
        textViewRecipientPhone = findViewById(R.id.textView_recipientPhone);
        textViewPrice = findViewById(R.id.textView_orderPrice);
        textViewDistance = findViewById(R.id.textView_orderDistance);
        statusHistoryLayout = findViewById(R.id.statusHistoryLayout);
        goBackButton = findViewById(R.id.goBackButton);
        editOrderButton = findViewById(R.id.editOrderButton);
        cancelOrderButton = findViewById(R.id.cancelOrderButton);

        Intent intent = getIntent();
        String orderId = intent.getStringExtra("orderId");
        String trackingNumber = intent.getStringExtra("trackingNumber");
        String packageDetails = intent.getStringExtra("packageDetails");
        String recipientName = intent.getStringExtra("recipientName");
        String recipientPhone = intent.getStringExtra("recipientPhone");
        String price = intent.getStringExtra("price");
        String distance = intent.getStringExtra("distance");
        double pickupLat = Double.parseDouble(intent.getStringExtra("pickupLat"));
        double pickupLng = Double.parseDouble(intent.getStringExtra("pickupLng"));
        double deliveryLat = Double.parseDouble(intent.getStringExtra("deliveryLat"));
        double deliveryLng = Double.parseDouble(intent.getStringExtra("deliveryLng"));



        textViewTrackingNumber.setText("Tracking Number: " + trackingNumber);
        textViewPackageDetails.setText("Package Details: " + packageDetails);
        textViewRecipientName.setText("Recipient Name: " + recipientName);
        textViewRecipientPhone.setText("Recipient Phone: " + recipientPhone);
        textViewPrice.setText("Price: Rs " + price);
        textViewDistance.setText("Distance: " + distance + " km");

        pickupLocation = new LatLng(pickupLat, pickupLng);
        deliveryLocation = new LatLng(deliveryLat, deliveryLng);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_order_details);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        textViewTrackingNumber.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Tracking Number", trackingNumber);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(OrderDetailsActivity.this, "Tracking Number copied to clipboard", Toast.LENGTH_SHORT).show();
            return true;
        });

        editOrderButton.setOnClickListener(v -> {
            Intent intentEditOrder = new Intent(OrderDetailsActivity.this, EditOrderActivity.class);
            intentEditOrder.putExtra("orderId", orderId);
            startActivity(intentEditOrder);
            finish();
        });

        cancelOrderButton.setOnClickListener(v -> {
            showCancelOrderDialog(orderId);
        });
        goBackButton.setOnClickListener(v -> {
            Intent intent1 = new Intent(OrderDetailsActivity.this, ViewOrderActivity.class);
            startActivity(intent1);
            finish();
        });

        loadOrderStatusHistory(orderId);
    }

    private void showCancelOrderDialog(String orderId) {
        // Create an AlertDialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", (dialog, which) -> {
                    // If the user confirms, proceed to cancel the order
                    cancelOrder(orderId);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // If the user cancels, just dismiss the dialog
                    dialog.dismiss();
                })
                .setCancelable(false) // Prevent dialog from closing if the user taps outside
                .show();
    }

    private void loadOrderStatusHistory(String orderId) {
        DatabaseReference orderStatusRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId).child("statusHistory");

        orderStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                statusHistoryLayout.removeAllViews();
                String latestStatus = "";

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String status = snapshot.child("status").getValue(String.class);
                        String timestamp = snapshot.child("timestamp").getValue(String.class);

                        TextView statusTextView = new TextView(OrderDetailsActivity.this);
                        statusTextView.setText("● " + status + " - " + timestamp);
                        statusTextView.setTextSize(14);
                        statusHistoryLayout.addView(statusTextView);

                        latestStatus = status;
                    }

                    if ("Order Created".equals(latestStatus)) {
                        editOrderButton.setVisibility(View.VISIBLE);
                        cancelOrderButton.setVisibility(View.VISIBLE);
                    } else {
                        editOrderButton.setVisibility(View.GONE);
                        cancelOrderButton.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(OrderDetailsActivity.this, "No status history found for this order", Toast.LENGTH_SHORT).show();
                    editOrderButton.setVisibility(View.GONE);
                    cancelOrderButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("OrderDetailsActivity", "Failed to load status history", databaseError.toException());
            }
        });
    }


    private void cancelOrder(String orderId) {
        DatabaseReference orderStatusRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId).child("statusHistory");

        // Get the current timestamp in the desired format
        String timestamp = getCurrentFormattedTimestamp();

        // Create a HashMap to store status and timestamp
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("status", "Cancelled");
        statusUpdate.put("timestamp", timestamp);

        // Push the new status update to Firebase
        orderStatusRef.push().setValue(statusUpdate)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(OrderDetailsActivity.this, "Order has been cancelled successfully.", Toast.LENGTH_SHORT).show();
                    editOrderButton.setVisibility(View.GONE);
                    cancelOrderButton.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(OrderDetailsActivity.this, "Failed to cancel the order", Toast.LENGTH_SHORT).show();
                    Log.e("OrderDetailsActivity", "Error while cancelling order", e);
                });
    }
    private String getCurrentFormattedTimestamp() {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a");
        return dateFormat.format(new Date());
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.clear();

        if (pickupLocation != null) {
            mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }

        if (deliveryLocation != null) {
            mMap.addMarker(new MarkerOptions().position(deliveryLocation).title("Delivery Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        if (pickupLocation != null && deliveryLocation != null) {
            mMap.addPolyline(new PolylineOptions()
                    .add(pickupLocation, deliveryLocation)
                    .width(5)
                    .color(ContextCompat.getColor(this, R.color.pure_courier_text)));

            LatLng midPoint = new LatLng(
                    (pickupLocation.latitude + deliveryLocation.latitude) / 2,
                    (pickupLocation.longitude + deliveryLocation.longitude) / 2
            );
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(midPoint, 11));
        }
    }
}
