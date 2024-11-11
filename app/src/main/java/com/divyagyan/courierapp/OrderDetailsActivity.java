package com.divyagyan.courierapp;

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

public class OrderDetailsActivity extends DrawerBaseActivity implements OnMapReadyCallback {

    private ActivityOrderDetailsBinding activityOrderDetailsBinding;
    private TextView textViewTrackingNumber, textViewPackageDetails, textViewRecipientName, textViewRecipientPhone, textViewPrice,textViewDistance;
    private LinearLayout statusHistoryLayout;
    private GoogleMap mMap;
    private LatLng pickupLocation;
    private LatLng deliveryLocation;
    private Button goBackButton, editOrderButton;

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
        textViewDistance.setText("Distance: " + distance+ " km");

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
            intentEditOrder.putExtra("trackingNumber", trackingNumber);
            intentEditOrder.putExtra("packageDetails", packageDetails);
            intentEditOrder.putExtra("recipientName", recipientName);
            intentEditOrder.putExtra("recipientPhone", recipientPhone);
            intentEditOrder.putExtra("price", price);
            intentEditOrder.putExtra("distance", distance);
            intentEditOrder.putExtra("pickupLat", String.valueOf(pickupLocation.latitude));
            intentEditOrder.putExtra("pickupLng", String.valueOf(pickupLocation.longitude));
            intentEditOrder.putExtra("deliveryLat", String.valueOf(deliveryLocation.latitude));
            intentEditOrder.putExtra("deliveryLng", String.valueOf(deliveryLocation.longitude));
            startActivity(intentEditOrder);
            finish();
        });

        goBackButton.setOnClickListener(v -> {
            Intent intent1 = new Intent(OrderDetailsActivity.this, ViewOrderActivity.class);
            startActivity(intent1);
            finish();
        });

        // Load status history and handle "Delivered" status
        loadOrderStatusHistory(orderId);
    }

    private void loadOrderStatusHistory(String orderId) {
        DatabaseReference orderStatusRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId).child("statusHistory");

        orderStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                statusHistoryLayout.removeAllViews();
                boolean isDelivered = false; // Flag to check if order is delivered
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String status = snapshot.child("status").getValue(String.class);
                        String timestamp = snapshot.child("timestamp").getValue(String.class);

                        // Dynamically create TextView for each status
                        TextView statusTextView = new TextView(OrderDetailsActivity.this);
                        statusTextView.setText("● " + status + " - " + timestamp);
                        statusTextView.setTextSize(14);
                        statusHistoryLayout.addView(statusTextView);

                        // Check if the status is "Delivered"
                        if ("Delivered".equals(status)) {
                            isDelivered = true;
                        }
                    }
                    // Hide the edit button if the latest status is "Delivered"
                    if (isDelivered) {
                        editOrderButton.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(OrderDetailsActivity.this, "No status history found for this order", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("OrderDetailsActivity", "Failed to load status history", databaseError.toException());
            }
        });
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
